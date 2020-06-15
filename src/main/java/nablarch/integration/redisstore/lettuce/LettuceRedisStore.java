package nablarch.integration.redisstore.lettuce;

import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.SessionStore;
import nablarch.fw.ExecutionContext;

import java.util.Collections;
import java.util.List;

/**
 * Lettuce を使って Redis のセッションストアを実装したクラス。
 *
 * @author Tanaka Tomoyuki
 */
public class LettuceRedisStore extends SessionStore {

    /**
     * セッションIDを元に、 Redis に格納するときに使用するキーを作成する。
     * @param sessionId セッションID
     * @return Redis への格納に使用するキー
     */
    public static String toSessionStoreKey(String sessionId) {
        return "nablarch.session." + sessionId;
    }

    private LettuceRedisClient client;

    /**
     * コンストラクタ。
     */
    public LettuceRedisStore() {
        super("redis");
    }

    @Override
    public List<SessionEntry> load(String sessionId, ExecutionContext executionContext) {
        byte[] encoded = client.get(toSessionStoreKey(sessionId));
        return encoded == null ? Collections.emptyList() : decode(encoded);
    }

    @Override
    public void save(String sessionId, List<SessionEntry> entryList, ExecutionContext executionContext) {
        if (entryList == null || entryList.isEmpty()) {
            delete(sessionId, executionContext);
        } else {
            client.set(toSessionStoreKey(sessionId), encode(entryList));
            client.pexpire(toSessionStoreKey(sessionId), getExpiresMilliSeconds());
        }
    }

    @Override
    public void delete(String sessionId, ExecutionContext executionContext) {
        client.del(toSessionStoreKey(sessionId));
    }

    @Override
    public void invalidate(String sessionId, ExecutionContext executionContext) {
        delete(sessionId, executionContext);
    }

    /**
     * {@link LettuceRedisClient} を設定する。
     * @param client {@link LettuceRedisClient}
     */
    public void setClient(LettuceRedisClient client) {
        this.client = client;
    }
}
