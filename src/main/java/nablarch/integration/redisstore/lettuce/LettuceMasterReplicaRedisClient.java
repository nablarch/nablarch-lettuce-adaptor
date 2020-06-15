package nablarch.integration.redisstore.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import nablarch.core.repository.initialization.Initializable;

/**
 * Master/Replica 構成の Redis に接続するための {@link LettuceRedisClient} 実装。
 * <p>
 * このクラスの {@link #getType()} は、識別子 {@code "masterReplica"} を返す。
 * </p>
 *
 * @author Tanaka Tomoyuki
 */
public class LettuceMasterReplicaRedisClient extends AbstractLettuceRedisClient implements Initializable {
    private RedisClient client;
    private StatefulRedisMasterReplicaConnection<byte[], byte[]> connection;
    private RedisCommands<byte[], byte[]> commands;

    /**
     * 接続するRedisサーバーのURI。
     */
    protected String uri;

    /**
     * コンストラクタ。
     */
    public LettuceMasterReplicaRedisClient() {
        super("masterReplica");
    }

    /**
     * 接続するRedisサーバーのURIを設定する。
     * <p>
     * URIの書式については、<a href="https://lettuce.io/core/release/reference/#redisuri.uri-syntax">Lettuceのドキュメント</a>を参照。
     * </p>
     *
     * @param uri 接続するRedisサーバーのURI
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public void set(String key, byte[] value) {
        commands.set(encodeKey(key), value);
    }

    @Override
    public void pexpire(String key, long milliseconds) {
        commands.pexpire(encodeKey(key), milliseconds);
    }

    @Override
    public void pexpireat(String key, long milliseconds) {
        commands.pexpireat(encodeKey(key), milliseconds);
    }

    @Override
    public long pttl(String key) {
        return commands.pttl(encodeKey(key));
    }

    @Override
    public byte[] get(String key) {
        return commands.get(encodeKey(key));
    }

    @Override
    public void del(String key) {
        commands.del(encodeKey(key));
    }

    @Override
    public boolean exists(String key) {
        return commands.exists(encodeKey(key)) == 1L;
    }

    /**
     * {@inheritDoc}
     * <p>
     * このメソッドは、 {@link #createClient()} と {@link #createConnection(RedisClient)} メソッドを使って
     * {@link RedisClient} と {@link StatefulRedisMasterReplicaConnection} のインスタンスを生成している。<br>
     * これらのインスタンスの設定を任意にカスタマイズしたい場合は、このクラスを継承したサブクラスを作り、
     * それぞれの {@code create} メソッドをオーバーライドすること。
     * </p>
     */
    @Override
    public void initialize() {
        client = createClient();
        connection = createConnection(client);
        commands = connection.sync();
    }

    /**
     * {@link RedisClient} のインスタンスを生成する。
     * @return 生成された {@link RedisClient}
     */
    protected RedisClient createClient() {
        return RedisClient.create();
    }

    /**
     * {@link StatefulRedisMasterReplicaConnection} のインスタンスを生成する。
     * @param client {@link #createClient()} で生成された {@link RedisClient} インスタンス
     * @return 生成された {@link StatefulRedisMasterReplicaConnection}
     */
    protected StatefulRedisMasterReplicaConnection<byte[], byte[]> createConnection(RedisClient client) {
        return MasterReplica.connect(client, new ByteArrayCodec(), RedisURI.create(uri));
    }

    @Override
    public void shutdown() {
        connection.close();
        client.shutdown();
    }
}
