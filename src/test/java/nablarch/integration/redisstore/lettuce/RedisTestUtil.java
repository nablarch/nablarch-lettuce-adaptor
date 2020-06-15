package nablarch.integration.redisstore.lettuce;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Redis テストのためのユーティリティメソッドをまとめたクラス。
 * @author Tanaka Tomoyuki
 */
public class RedisTestUtil {

    /**
     * テストで使用するRedisインスタンスのホストアドレスを取得する。
     * <p>
     * 環境変数 {@code NABLARCH_REDISSTORE_TEST_REDIS_HOST} が設定されている場合は、
     * その値を返す。<br>
     * 環境変数が設定されていない場合は、 {@link InetAddress#getLocalHost()} で取得した
     * ホストのアドレスを返す。
     * </p>
     *
     * @return テストで使用するRedisインスタンスのホストアドレス
     */
    public static String getRedisHostAddress() {
        String redisHost = System.getenv("NABLARCH_REDISSTORE_TEST_REDIS_HOST");
        try {
            return redisHost != null ? redisHost : InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private RedisTestUtil() {}
}
