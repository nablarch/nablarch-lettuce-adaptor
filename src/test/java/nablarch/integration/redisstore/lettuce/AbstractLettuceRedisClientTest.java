package nablarch.integration.redisstore.lettuce;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link AbstractLettuceRedisClient} のテストクラス。
 * @author Tanaka Tomoyuki
 */
public class AbstractLettuceRedisClientTest {

    @Test
    public void testTypeObtainedByGetterEqualsToConstructorArgument() {
        AbstractLettuceRedisClient sut = new AbstractLettuceRedisClient("foo") {
            @Override public void set(String key, byte[] value) {}

            @Override public void pexpire(String key, long milliseconds) {}
            @Override public void pexpireat(String key, long milliseconds) {}
            @Override public long pttl(String key) { return 0; }
            @Override public byte[] get(String key) { return new byte[0]; }
            @Override public void del(String key) {}
            @Override public boolean exists(String key) { return false; }
            @Override public void dispose() {}
        };

        String actual = sut.getType();

        assertThat(actual, is("foo"));
    }
}