package nablarch.integration.health;

import nablarch.integration.redisstore.lettuce.LettuceRedisClient;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisHealthChecker}のテスト。
 */
public class RedisHealthCheckerTest {

    private final LettuceRedisClient client = mock(LettuceRedisClient.class);

    @Test
    public void success() {
        when(client.exists("healthcheck")).thenReturn(false);

        RedisHealthChecker sut = new RedisHealthChecker();
        sut.setClient(client);

        assertThat(sut.check(null, null), is(true));
    }

    @Test
    public void failureByException() {
        when(client.exists("healthcheck")).thenThrow(new RuntimeException());

        RedisHealthChecker sut = new RedisHealthChecker();
        sut.setClient(client);

        assertThat(sut.check(null, null), is(false));
    }

    @Test
    public void changeKey() {
        when(client.exists("test")).thenReturn(false);

        RedisHealthChecker sut = new RedisHealthChecker();
        sut.setClient(client);
        sut.setKey("test");

        assertThat(sut.check(null, null), is(true));

        verify(client).exists("test");
    }

    @Test
    public void defaultName() {
        RedisHealthChecker sut = new RedisHealthChecker();
        assertThat(sut.getName(), is("Redis"));
    }

    @Test
    public void changeName() {
        RedisHealthChecker sut = new RedisHealthChecker();
        sut.setName("NoSQL");
        assertThat(sut.getName(), is("NoSQL"));
    }
}