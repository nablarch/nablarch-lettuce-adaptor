package nablarch.integration.redisstore.lettuce;

import java.nio.charset.StandardCharsets;

/**
 * {@link LettuceRedisClient} の共通処理をまとめた抽象クラス。
 *
 * @author Tanaka Tomoyuki
 */
public abstract class AbstractLettuceRedisClient implements LettuceRedisClient {
    private final String type;

    /**
     * コンストラクタ。
     * @param type 実装クラスを識別する値
     */
    protected AbstractLettuceRedisClient(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * 文字列のキーを {@code byte[]} にエンコードする。
     * @param key キー
     * @return エンコード後のキー
     */
    protected byte[] encodeKey(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
