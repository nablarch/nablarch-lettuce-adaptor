package nablarch.integration.health;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import nablarch.integration.redisstore.lettuce.LettuceRedisClient;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * {@link RedisHealthChecker}のテスト。
 */
public class RedisHealthCheckerTest {

    @Mocked
    private LettuceRedisClient client;

    @Test
    public void success() {
        new Expectations() {{
            client.exists("healthcheck");
            result = false;
        }};

        RedisHealthChecker sut = new RedisHealthChecker();
        sut.setClient(client);

        assertThat(sut.check(null, null), is(true));
    }

    @Test
    public void failureByException() {
        new Expectations() {{
            client.exists("healthcheck");
            result = new RuntimeException();
        }};

        RedisHealthChecker sut = new RedisHealthChecker();
        sut.setClient(client);

        assertThat(sut.check(null, null), is(false));
    }

    @Test
    public void changeKey() {
        new Expectations() {{
            client.exists("test");
            result = false;
        }};

        RedisHealthChecker sut = new RedisHealthChecker();
        sut.setClient(client);
        sut.setKey("test");

        assertThat(sut.check(null, null), is(true));

        new Verifications() {{
            client.exists("test");
            times = 1;
        }};
    }
}