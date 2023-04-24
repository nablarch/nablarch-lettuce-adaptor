package nablarch.integration.redisstore.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link LettuceMasterReplicaRedisClient} のテストクラス。
 * @author Tanaka Tomoyuki
 */
public class LettuceMasterReplicaRedisClientTest {

    @ClassRule
    public static TemporaryFolder tmpDir = new TemporaryFolder();

    private static RedisClient LETTUCE_CLIENT;
    private static StatefulRedisMasterReplicaConnection<String, String> LETTUCE_CONNECTION;
    private static RedisCommands<String, String> LETTUCE_COMMANDS;

    private static String URI;

    @BeforeClass
    public static void beforeClass() {
        // 検証用のLettuceクライアントを生成
        String containerIpAddress = RedisTestUtil.getRedisHostAddress();
        URI = String.format("redis-sentinel://%s:%d,%s:%d,%s:%d?sentinelMasterId=%s",
            containerIpAddress, 7111,
            containerIpAddress, 7112,
            containerIpAddress, 7113,
            "mysentinel"
        );

        LETTUCE_CLIENT = RedisClient.create();
        LETTUCE_CONNECTION = MasterReplica.connect(LETTUCE_CLIENT, new StringCodec(StandardCharsets.UTF_8), RedisURI.create(URI));
        LETTUCE_COMMANDS = LETTUCE_CONNECTION.sync();
    }

    private LettuceMasterReplicaRedisClient sut;

    @Before
    public void before() {
        sut = new LettuceMasterReplicaRedisClient();

        sut.setUri(URI);
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
    public void testExist() {
        assertThat("foo does not exist.", LETTUCE_COMMANDS.exists("foo"), is(0L));

        assertThat("false if key does not exist.", sut.exists("foo"), is(false));

        LETTUCE_COMMANDS.set("foo", "FOO");

        assertThat("true if key exists.", sut.exists("foo"), is(true));
    }

    @Test
    public void testTypeValue() {
        String actual = sut.getType();
        assertThat(actual, is("masterReplica"));
    }

    @Test
    public void testDispose() {
        RedisClient client = mock(RedisClient.class);
        @SuppressWarnings("unchekced")
        StatefulRedisMasterReplicaConnection<byte[], byte[]> connection = mock(StatefulRedisMasterReplicaConnection.class);

        LettuceMasterReplicaRedisClient sut = new LettuceMasterReplicaRedisClient() {
            @Override
            protected RedisClient createClient() {
                return client;
            }

            @Override
            protected StatefulRedisMasterReplicaConnection<byte[], byte[]> createConnection(RedisClient client) {
                return connection;
            }
        };

        sut.initialize();
        sut.dispose();

        verify(connection).close();
        verify(client).shutdown();
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