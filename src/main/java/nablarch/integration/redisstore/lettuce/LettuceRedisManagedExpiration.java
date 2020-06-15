package nablarch.integration.redisstore.lettuce;

import nablarch.common.web.session.Expiration;
import nablarch.fw.ExecutionContext;

/**
 * Redis にセッションの有効期限を保存するためのクラス。
 * @author Tanaka Tomoyuki
 */
public class LettuceRedisManagedExpiration implements Expiration {

    private LettuceRedisClient client;

    @Override
    public boolean isExpired(String sessionId, long currentDateTime, ExecutionContext context) {
        return !existsSession(sessionId);
    }

    @Override
    public void saveExpirationDateTime(String sessionId, long expirationDateTime, ExecutionContext context) {
        String key = LettuceRedisStore.toSessionStoreKey(sessionId);
        if (!existsSession(sessionId)) {
            client.set(key, new byte[0]);
        }
        client.pexpireat(key, expirationDateTime);
    }

    @Override
    public boolean isDeterminable(String sessionId, ExecutionContext context) {
        return existsSession(sessionId);
    }

    private boolean existsSession(String sessionId) {
        return client.exists(LettuceRedisStore.toSessionStoreKey(sessionId));
    }

    /**
     * {@link LettuceRedisClient} を設定する。
     * @param client {@link LettuceRedisClient}
     */
    public void setClient(LettuceRedisClient client) {
        this.client = client;
    }
}
