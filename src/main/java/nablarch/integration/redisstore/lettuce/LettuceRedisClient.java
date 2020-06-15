package nablarch.integration.redisstore.lettuce;

/**
 * セッションストアの実装に必要となる Redis コマンドを定義したインターフェース。
 *
 * @author Tanaka Tomoyuki
 */
public interface LettuceRedisClient {

    /**
     * 実装クラスを識別する種別を取得する。
     * @return 実装クラスを識別する値
     */
    String getType();

    /**
     * 値を保存する。
     * @param key キー
     * @param value 値
     */
    void set(String key, byte[] value);

    /**
     * キーの有効期限を設定する。
     * @param key キー
     * @param milliseconds 有効期限（ミリ秒）
     */
    void pexpire(String key, long milliseconds);

    /**
     * キーの有効期限をUTC時間で設定する
     * @param key キー
     * @param milliseconds UTC時間で指定された有効期限（ミリ秒）
     */
    void pexpireat(String key, long milliseconds);

    /**
     * キーの残りの生存期間を取得する。
     * <p>
     * キーに有効期限が設定されていない場合は {@code -1} を返し、
     * キーが存在しない場合は {@code -2} を返す。
     * </p>
     * @param key キー
     * @return 残りの生存期間（ミリ秒）
     */
    long pttl(String key);

    /**
     * 値を取得する。
     * <p>
     * 該当するキーが存在しない場合は {@code null} を返す。
     * </p>
     *
     * @param key キー
     * @return 値
     */
    byte[] get(String key);

    /**
     * 値を削除する。
     * @param key キー
     */
    void del(String key);

    /**
     * 指定したキーが存在するか確認する。
     * @param key キー
     * @return キーが存在する場合は {@code true}
     */
    boolean exists(String key);

    /**
     * Redisサーバーとの接続を閉じる。
     */
    void shutdown();
}
