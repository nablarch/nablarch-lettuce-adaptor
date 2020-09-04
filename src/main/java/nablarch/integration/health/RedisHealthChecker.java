package nablarch.integration.health;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.handler.health.HealthChecker;
import nablarch.integration.redisstore.lettuce.LettuceRedisClient;

/**
 * Redisのヘルスチェックを行うクラス。
 *
 * キーの存在チェックを行い、例外が発生しなければヘルシと判断する。
 * キーのデフォルトは"healthcheck"。
 * キーは存在しなくてよい。
 *
 * @author Kiyohito Itoh
 */
public class RedisHealthChecker extends HealthChecker {

    private LettuceRedisClient client;
    private String key = "healthcheck";

    public RedisHealthChecker() {
        setName("Redis");
    }

    @Override
    protected boolean tryOut(HttpRequest request, ExecutionContext context) {
        client.exists(key);
        return true;
    }

    /**
     * Redisのクライアントを設定する。
     * @param client Redisのクライアント
     */
    public void setClient(LettuceRedisClient client) {
        this.client = client;
    }

    /**
     * 存在チェックに使用するキーを設定する。
     * @param key 存在チェックに使用するキー
     */
    public void setKey(String key) {
        this.key = key;
    }
}
