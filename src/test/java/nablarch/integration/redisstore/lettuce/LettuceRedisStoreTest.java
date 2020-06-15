package nablarch.integration.redisstore.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.StateEncoder;
import nablarch.common.web.session.encoder.JavaSerializeStateEncoder;
import nablarch.fw.ExecutionContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * {@link LettuceRedisStore} のテストクラス。
 * @author Tanaka Tomoyuki
 */
public class LettuceRedisStoreTest {

    private static final ExecutionContext NO_USED = null;
    private static final String SESSION_ID1 = "test-session1";
    private static final String SESSION_ID2 = "test-session2";
    private static final String SESSION_STORE_KEY1 = LettuceRedisStore.toSessionStoreKey(SESSION_ID1);
    private static final String SESSION_STORE_KEY2 = LettuceRedisStore.toSessionStoreKey(SESSION_ID2);
    private static final StateEncoder STATE_ENCODER = new JavaSerializeStateEncoder();
    private static final long EXPIRES = 5000L;

    private static RedisClient LETTUCE_CLIENT;
    private static StatefulRedisConnection<byte[], byte[]> LETTUCE_CONNECTION;
    private static RedisCommands<byte[], byte[]> LETTUCE_COMMANDS;

    private static String URI;

    private final List<SessionEntry> sessionEntryList1 = Arrays.asList(
        entry("one", "ONE"),
        entry("two", "TWO"),
        entry("three", "THREE")
    );

    private final List<SessionEntry> sessionEntryList2 = Arrays.asList(
        entry("four", "FOUR"),
        entry("five", "FIVE")
    );

    @BeforeClass
    public static void beforeClass() {
        URI = String.format("redis://%s:%d", RedisTestUtil.getRedisHostAddress(), 7000);

        LETTUCE_CLIENT = RedisClient.create(RedisURI.create(URI));
        LETTUCE_CONNECTION = LETTUCE_CLIENT.connect(new ByteArrayCodec());
        LETTUCE_COMMANDS = LETTUCE_CONNECTION.sync();
    }

    private LettuceRedisStore sut;

    @Before
    public void before() {
        sut = new LettuceRedisStore();

        sut.setStateEncoder(STATE_ENCODER);
        sut.setExpires(EXPIRES);

        LettuceSimpleRedisClient client = new LettuceSimpleRedisClient();
        client.setUri(URI);
        client.initialize();
        sut.setClient(client);
    }

    @Test
    public void testScenarioSaveAndLoadAndDelete() {
        // SessionEntryリストを保存
        assertThat("session1 in redis is null before save.", getFromRedis(SESSION_STORE_KEY1), is(nullValue()));
        assertThat("session2 in redis is null before save.", getFromRedis(SESSION_STORE_KEY2), is(nullValue()));

        sut.save(SESSION_ID1, sessionEntryList1, NO_USED);
        sut.save(SESSION_ID2, sessionEntryList2, NO_USED);

        assertThat("session1 in redis is not null after save.", getFromRedis(SESSION_STORE_KEY1), is(notNullValue()));
        assertThat("session2 in redis is not null after save.", getFromRedis(SESSION_STORE_KEY2), is(notNullValue()));

        // 保存されたSessionEntryリストの取得
        List<SessionEntry> loadedEntryList1 = sut.load(SESSION_ID1, NO_USED);

        assertThat("loaded entry list1 equals to saved entry list1.", loadedEntryList1, contains(
            allOf(
                hasProperty("key", is("one")),
                hasProperty("value", is("ONE"))
            ),
            allOf(
                hasProperty("key", is("two")),
                hasProperty("value", is("TWO"))
            ),
            allOf(
                hasProperty("key", is("three")),
                hasProperty("value", is("THREE"))
            )
        ));

        List<SessionEntry> loadedEntryList2 = sut.load(SESSION_ID2, NO_USED);

        assertThat("loaded entry list2 equals to saved entry list2.", loadedEntryList2, contains(
            allOf(
                hasProperty("key", is("four")),
                hasProperty("value", is("FOUR"))
            ),
            allOf(
                hasProperty("key", is("five")),
                hasProperty("value", is("FIVE"))
            )
        ));

        // SessionEntryリストの削除
        sut.delete(SESSION_ID1, NO_USED);

        assertThat("session1 in redis is null after delete session1.", getFromRedis(SESSION_STORE_KEY1), is(nullValue()));
        assertThat("session2 in redis is not null after delete session1.", getFromRedis(SESSION_STORE_KEY2), is(notNullValue()));
    }

    @Test
    public void testScenarioDeleteSessionIfSavedEntryListIsEmpty() {
        sut.save(SESSION_ID1, sessionEntryList1, NO_USED);

        assertThat("session in redis is not null before save empty list.", getFromRedis(SESSION_STORE_KEY1), is(notNullValue()));

        sut.save(SESSION_ID1, Collections.emptyList(), NO_USED);

        assertThat("session in redis is null after save empty list.", getFromRedis(SESSION_STORE_KEY1), is(nullValue()));
    }

    @Test
    public void testScenarioDeleteSessionIfSavedEntryListIsNull() {
        sut.save(SESSION_ID1, sessionEntryList1, NO_USED);

        assertThat("session in redis is not null before save null.", getFromRedis(SESSION_STORE_KEY1), is(notNullValue()));

        sut.save(SESSION_ID1, null, NO_USED);

        assertThat("session in redis is null after save null.", getFromRedis(SESSION_STORE_KEY1), is(nullValue()));
    }

    @Test
    public void testInvalidate() {
        sut.save(SESSION_ID1, sessionEntryList1, NO_USED);

        assertThat("session in redis is not null after save.", getFromRedis(SESSION_STORE_KEY1), is(notNullValue()));

        sut.invalidate(SESSION_ID1, NO_USED);

        assertThat("session in redis is null after invalidate.", getFromRedis(SESSION_STORE_KEY1), is(nullValue()));
    }

    @Test
    public void testLoadEmptyListIfSessionDoesNotExist() {
        List<SessionEntry> loaded = sut.load(SESSION_ID1, NO_USED);
        assertThat(loaded, is(empty()));
    }

    @Test
    public void testExpiration() {
        long start = System.currentTimeMillis();
        sut.save(SESSION_ID1, sessionEntryList1, NO_USED);

        Long pttl = LETTUCE_COMMANDS.pttl(SESSION_STORE_KEY1.getBytes(StandardCharsets.UTF_8));
        long time = System.currentTimeMillis() - start;

        assertThat(pttl, allOf(
            greaterThanOrEqualTo(sut.getExpiresMilliSeconds() - time),
            lessThanOrEqualTo(sut.getExpiresMilliSeconds())
        ));
    }

    private SessionEntry entry(String key, Object value) {
        return new SessionEntry(key, value, sut);
    }

    private static byte[] getFromRedis(String key) {
        return LETTUCE_COMMANDS.get(key.getBytes(StandardCharsets.UTF_8));
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
}