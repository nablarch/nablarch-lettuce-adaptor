package nablarch.integration.redisstore.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * {@link LettuceSimpleRedisClient} のテストクラス。
 * @author Tanaka Tomoyuki
 */
public class LettuceSimpleRedisClientTest {

    private static RedisClient LETTUCE_CLIENT;
    private static StatefulRedisConnection<String, String> LETTUCE_CONNECTION;
    private static RedisCommands<String, String> LETTUCE_COMMANDS;

    private static String URI;

    @BeforeClass
    public static void beforeClass() {
        URI = String.format("redis://%s:%d", RedisTestUtil.getRedisHostAddress(), 7000);

        LETTUCE_CLIENT = RedisClient.create(URI);
        LETTUCE_CONNECTION = LETTUCE_CLIENT.connect(new StringCodec(StandardCharsets.UTF_8));
        LETTUCE_COMMANDS = LETTUCE_CONNECTION.sync();
    }

    private LettuceSimpleRedisClient sut;

    @Before
    public void before() {
        sut = new LettuceSimpleRedisClient();
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

        assertThat("pttl is -1 before pexpire", LETTUCE_COMMANDS.pttl("foo"), is(-1L));

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

        assertThat("pttl is -1 before pexpireat", LETTUCE_COMMANDS.pttl("foo"), is(-1L));

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
        assertThat(actual, is("simple"));
    }

    @After
    public void after() {
        sut.shutdown();
        LETTUCE_COMMANDS.keys("*").forEach(key -> LETTUCE_COMMANDS.del(key));
    }

    @AfterClass
    public static void afterClass() {
        LETTUCE_CONNECTION.close();
        LETTUCE_CLIENT.shutdown();
    }
}