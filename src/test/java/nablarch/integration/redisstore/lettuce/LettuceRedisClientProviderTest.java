package nablarch.integration.redisstore.lettuce;

import nablarch.core.repository.di.ContainerProcessException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * {@link LettuceRedisClientProvider} のテストクラス。
 * @author Tanaka Tomoyuki
 */
public class LettuceRedisClientProviderTest {
    private LettuceRedisClientProvider sut = new LettuceRedisClientProvider();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testCreateObjectReturnsClientInstanceThatHasSameTypeAsClientType() {
        MockClient fooClient = new MockClient("foo");
        MockClient barClient = new MockClient("bar");
        sut.setClientList(Arrays.asList(fooClient, barClient));

        sut.setClientType("foo");

        LettuceRedisClient actual = sut.createObject();

        assertThat(actual, is(sameInstance(fooClient)));
    }

    @Test
    public void testThrowsExceptionIfClientTypeIsNull() {
        sut.setClientType(null);
        sut.setClientList(Collections.emptyList());

        exception.expect(ContainerProcessException.class);
        exception.expectMessage("clientType must not be null.");

        sut.createObject();
    }

    @Test
    public void testThrowsExceptionIfClientListIsNull() {
        sut.setClientType("foo");
        sut.setClientList(null);

        exception.expect(ContainerProcessException.class);
        exception.expectMessage("clientList must not be null.");

        sut.createObject();
    }

    @Test
    public void testThrowsExceptionIfNoClientMatches() {
        MockClient fizzClient = new MockClient("fizz");
        MockClient buzzClient = new MockClient("buzz");
        sut.setClientList(Arrays.asList(fizzClient, buzzClient));

        sut.setClientType("foo");

        exception.expect(ContainerProcessException.class);
        exception.expectMessage("No client matches. clientType=foo, clientList=[fizz, buzz]");

        sut.createObject();
    }

    private static class MockClient implements LettuceRedisClient {
        private final String type;

        private MockClient(String type) {
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
        }
        @Override public void set(String key, byte[] value) {}
        @Override public void pexpire(String key, long milliseconds) {}
        @Override public void pexpireat(String key, long milliseconds) {}
        @Override public long pttl(String key) { return 0; }
        @Override public byte[] get(String key) { return new byte[0]; }
        @Override public void del(String key) {}
        @Override public boolean exists(String key) { return false; }
        @Override public void dispose() {}
    }
}