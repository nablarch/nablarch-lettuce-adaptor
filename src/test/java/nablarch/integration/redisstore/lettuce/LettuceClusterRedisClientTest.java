package nablarch.integration.redisstore.lettuce;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.codec.StringCodec;
import mockit.Mocked;
import mockit.Verifications;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * {@link LettuceClusterRedisClient} のテストクラス。
 * @author Tanaka Tomoyuki
 */
public class LettuceClusterRedisClientTest {
    @ClassRule
    public static TemporaryFolder tmpDir = new TemporaryFolder();

    private static List<String> URI_LIST;

    private static RedisClusterClient LETTUCE_CLIENT;
    private static StatefulRedisClusterConnection<String, String> LETTUCE_CONNECTION;
    private static RedisAdvancedClusterCommands<String, String> LETTUCE_COMMANDS;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // 検証用のLettuceクライアントを生成
        String host = RedisTestUtil.getRedisHostAddress();
        String uriFormat = "redis://%s:%d";
        String node1 = String.format(uriFormat, host, 7211);
        String node2 = String.format(uriFormat, host, 7221);
        String node3 = String.format(uriFormat, host, 7231);
        URI_LIST = Arrays.asList(node1, node2, node3);

        LETTUCE_CLIENT = RedisClusterClient.create(URI_LIST.stream().map(RedisURI::create).collect(Collectors.toList()));
        LETTUCE_CONNECTION = LETTUCE_CLIENT.connect(new StringCodec(StandardCharsets.UTF_8));
        LETTUCE_COMMANDS = LETTUCE_CONNECTION.sync();
    }

    private LettuceClusterRedisClient sut;

    @Before
    public void before() {
        sut = new LettuceClusterRedisClient();

        sut.setUriList(URI_LIST);
        sut.initialize();
    }

    @Test
    public void testSet() {
        assertThat("null value before set", LETTUCE_COMMANDS.get("foo"), is(nullValue()));

        sut.set("foo", "testてすと".getBytes(StandardCharsets.UTF_8));

        assertThat("foo was set after set", LETTUCE_COMMANDS.get("foo"), is("testてすと"));
    }

    @Test
    public void testPexpire() {
        LETTUCE_COMMANDS.set("foo", "FOO");

        assertThat("pttl is -1 before pexpire", LETTUCE_COMMANDS.pttl("foo"), Matchers.is(-1L));

        long expiration = 12345L;
        long start = System.currentTimeMillis();
        sut.pexpire("foo", expiration);
        long ttl = LETTUCE_COMMANDS.pttl("foo");
        long time = System.currentTimeMillis() - start;

        assertThat("pttl is greater than 0 after pexpire", ttl,
            allOf(
                greaterThanOrEqualTo(expiration - time),
                lessThanOrEqualTo(expiration)
            )
        );
    }

    @Test
    public void testPexpireat() {
        LETTUCE_COMMANDS.set("foo", "FOO");

        assertThat("pttl is -1 before pexpireat", LETTUCE_COMMANDS.pttl("foo"), Matchers.is(-1L));

        long expiration = 12345L;
        sut.pexpireat("foo", System.currentTimeMillis() + expiration);
        long ttl = LETTUCE_COMMANDS.pttl("foo");

        /*
         * 「pttl は 0L より大きい」という緩い条件でテストをしている理由について。
         *
         * 原因は不明だが、 Redis の時刻が JVM の時刻より進んだ状態になることがある。
         * その場合、 testPexpire() のように厳密な範囲で pttl の値を検証すると、テストが失敗になる。
         *
         * このため、絶対時間で期限を設定する PEXPIREAT を使うテストでは、
         * 「pttl は 0L より大きい」という緩めの条件で検証をしている。
         */
        assertThat("pttl is greater than 0 after pexpireat", ttl,
            allOf(
                greaterThan(0L),
                lessThanOrEqualTo(expiration)
            )
        );
    }

    @Test
    public void testPttl() {
        assertThat("pttl -2 if key does not exist.", sut.pttl("foo"), is(-2L));

        LETTUCE_COMMANDS.set("foo", "FOO");

        assertThat("pttl -1 if key does not have expiration.", sut.pttl("foo"), is(-1L));

        LETTUCE_COMMANDS.pexpire("foo", 12345L);

        assertThat("pttl is greater than 0 after pexpire.", sut.pttl("foo"), is(greaterThan(0L)));
    }

    @Test
    public void testGet() {
        LETTUCE_COMMANDS.set("bar", "てすとtest");

        byte[] actual = sut.get("bar");

        assertThat(new String(actual, StandardCharsets.UTF_8), is("てすとtest"));
    }

    @Test
    public void testGetReturnsNullIfKeyDoesNotExist() {
        byte[] actual = sut.get("fizz");

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testDel() {
        LETTUCE_COMMANDS.set("buzz", "テストtest");

        assertThat("not null value before del", LETTUCE_COMMANDS.get("buzz"), is("テストtest"));

        sut.del("buzz");

        assertThat("null value after del", LETTUCE_COMMANDS.get("buzz"), is(nullValue()));
    }

    @Test
    public void testTypeValue() {
        String actual = sut.getType();
        assertThat(actual, is("cluster"));
    }

    @Test
    public void testExist() {
        assertThat("foo does not exist.", LETTUCE_COMMANDS.exists("foo"), is(0L));

        assertThat("false if key does not exist.", sut.exists("foo"), is(false));

        LETTUCE_COMMANDS.set("foo", "FOO");

        assertThat("true if key exists.", sut.exists("foo"), is(true));
    }

    @Test
    public void testDispose(@Mocked RedisClusterClient client, @Mocked StatefulRedisClusterConnection<byte[], byte[]> connection) {
        LettuceClusterRedisClient sut = new LettuceClusterRedisClient() {
            @Override
            protected RedisClusterClient createClient() {
                return client;
            }

            @Override
            protected StatefulRedisClusterConnection<byte[], byte[]> createConnection(RedisClusterClient client) {
                return connection;
            }
        };

        sut.initialize();
        sut.dispose();

        new Verifications() {{
            connection.close(); times = 1;
            client.shutdown(); times = 1;
        }};
    }

    @After
    public void after() {
        sut.dispose();
        LETTUCE_COMMANDS.keys("*").forEach(key -> LETTUCE_COMMANDS.del(key));
    }

    @AfterClass
    public static void afterClass() {
        LETTUCE_CONNECTION.close();
        LETTUCE_CLIENT.shutdown();
    }
}