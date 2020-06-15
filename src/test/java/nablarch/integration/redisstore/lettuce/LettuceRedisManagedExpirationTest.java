package nablarch.integration.redisstore.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import nablarch.fw.ExecutionContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class LettuceRedisManagedExpirationTest {

    private static final ExecutionContext NO_USED_CONTEXT = null;
    private static final long NO_USED_CURRENT_TIME = -1;
    private static final String SESSION_ID1 = "test-session-1";
    private static final String SESSION_ID2 = "test-session-2";
    private static final String SESSION_STORE_KEY1 = LettuceRedisStore.toSessionStoreKey(SESSION_ID1);
    private static final String SESSION_STORE_KEY2 = LettuceRedisStore.toSessionStoreKey(SESSION_ID2);

    private static String URI;

    private static RedisClient LETTUCE_CLIENT;
    private static StatefulRedisConnection<byte[], byte[]> LETTUCE_CONNECTION;
    private static RedisCommands<byte[], byte[]> LETTUCE_COMMANDS;

    @BeforeClass
    public static void beforeClass() {
        URI = String.format("redis://%s:%d", RedisTestUtil.getRedisHostAddress(), 7000);

        LETTUCE_CLIENT = RedisClient.create(URI);
        LETTUCE_CONNECTION = LETTUCE_CLIENT.connect(new ByteArrayCodec());
        LETTUCE_COMMANDS = LETTUCE_CONNECTION.sync();
    }

    private LettuceRedisManagedExpiration sut;

    @Before
    public void before() {
        sut = new LettuceRedisManagedExpiration();

        LettuceSimpleRedisClient client = new LettuceSimpleRedisClient();
        client.setUri(URI);
        client.initialize();
        sut.setClient(client);

        LETTUCE_COMMANDS.set(encodeKey(SESSION_STORE_KEY2), new byte[0]);
    }

    @Test
    public void testIsExpiredReturnsTrueIfKeyDoesNotExist() {
        assertThat("session 1 does not exist.",
                LETTUCE_COMMANDS.get(encodeKey(SESSION_STORE_KEY1)), is(nullValue()));
        assertThat("isExpired() returns true.",
                sut.isExpired(SESSION_ID1, NO_USED_CURRENT_TIME, NO_USED_CONTEXT), is(true));
    }

    @Test
    public void testIsExpiredReturnsFalseIfKeyExists() {
        LETTUCE_COMMANDS.set(encodeKey(SESSION_STORE_KEY1), new byte[0]);
        assertThat("session 1 exists.",
                LETTUCE_COMMANDS.get(encodeKey(SESSION_STORE_KEY1)), is(notNullValue()));
        assertThat("isExpired() returns false.",
                sut.isExpired(SESSION_ID1, NO_USED_CURRENT_TIME, NO_USED_CONTEXT), is(false));
    }

    @Test
    public void testSaveExpirationTime() {
        byte[] key = encodeKey(SESSION_STORE_KEY1);
        LETTUCE_COMMANDS.set(key, new byte[0]);

        assertThat("ttl is -1 before saveExpirationTime().", LETTUCE_COMMANDS.pttl(key), is(-1L));

        long expiration = 12345L;
        sut.saveExpirationDateTime(SESSION_ID1, System.currentTimeMillis() + expiration, NO_USED_CONTEXT);
        long ttl = LETTUCE_COMMANDS.pttl(key);

        assertThat("0 < ttl <= expiration", ttl,
            allOf(
                greaterThanOrEqualTo(0L),
                lessThanOrEqualTo(expiration)
            ));
    }

    @Test
    public void testSaveEmptyArrayIfSessionDoesNotExistWhenInvokeSaveExpirationTime() {
        byte[] key = encodeKey(SESSION_STORE_KEY1);

        assertThat("session does not exist.", LETTUCE_COMMANDS.get(encodeKey(SESSION_STORE_KEY1)), is(nullValue()));

        sut.saveExpirationDateTime(SESSION_ID1, System.currentTimeMillis() + 12345L, NO_USED_CONTEXT);

        assertThat("empty array is saved in session", LETTUCE_COMMANDS.get(key).length, is(0));
    }

    @Test
    public void testIsDeterminable() {
        assertThat("session does not exist.", LETTUCE_COMMANDS.get(encodeKey(SESSION_STORE_KEY1)), is(nullValue()));
        assertThat("false if session does not exist.", sut.isDeterminable(SESSION_ID1, NO_USED_CONTEXT), is(false));

        LETTUCE_COMMANDS.set(encodeKey(SESSION_STORE_KEY1), new byte[0]);

        assertThat("true if session exists.", sut.isDeterminable(SESSION_ID1, NO_USED_CONTEXT), is(true));
    }

    @After
    public void after() {
        LETTUCE_COMMANDS.keys("*".getBytes(StandardCharsets.UTF_8)).forEach(key -> LETTUCE_COMMANDS.del(key));
    }

    @AfterClass
    public static void afterClass() {
        LETTUCE_CONNECTION.close();
        LETTUCE_CLIENT.shutdown();
    }

    private static byte[] encodeKey(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }
}