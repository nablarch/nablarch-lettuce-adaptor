package nablarch.integration.redisstore.lettuce;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import nablarch.core.repository.initialization.Initializable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cluster 構成の Redis に接続するための {@link LettuceRedisClient} 実装クラス。
 * <p>
 * このクラスの {@link #getType()} は、識別子 {@code "cluster"} を返す。
 * </p>
 *
 * @author Tanaka Tomoyuki
 */
public class LettuceClusterRedisClient extends AbstractLettuceRedisClient implements Initializable {
    private RedisClusterClient client;
    private StatefulRedisClusterConnection<byte[], byte[]> connection;
    private RedisAdvancedClusterCommands<byte[], byte[]> commands;

    /**
     * 接続するRedisクラスタの、各ノードURIのリスト。
     */
    protected List<String> uriList;

    /**
     * コンストラクタ。
     */
    public LettuceClusterRedisClient() {
        super("cluster");
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
     * 接続するRedisクラスタの、各ノードのURIをリストで設定する。
     * <p>
     * URIの書式については、<a href="https://lettuce.io/core/release/reference/#redisuri.uri-syntax">Lettuceのドキュメント</a>を参照。
     * </p>
     *
     * @param uriList 各ノードのURIのリスト
     */
    public void setUriList(List<String> uriList) {
        this.uriList = uriList;
    }

    /**
     * {@inheritDoc}
     * <p>
     * このメソッドは、 {@link #createClient()} と {@link #createConnection(RedisClusterClient)} メソッドを使って
     * {@link RedisClusterClient} と {@link StatefulRedisClusterConnection} のインスタンスを生成している。<br>
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
     * {@link RedisClusterClient} のインスタンスを生成する。
     * @return 生成された {@link RedisClusterClient}
     */
    protected RedisClusterClient createClient() {
        List<RedisURI> redisUriList = uriList.stream().map(RedisURI::create).collect(Collectors.toList());
        return RedisClusterClient.create(redisUriList);
    }

    /**
     * {@link StatefulRedisClusterConnection} のインスタンスを生成する。
     * @param client {@link #createClient()} で生成された {@link RedisClusterClient} インスタンス
     * @return 生成された {@link StatefulRedisClusterConnection}
     */
    protected StatefulRedisClusterConnection<byte[], byte[]> createConnection(RedisClusterClient client) {
        return client.connect(new ByteArrayCodec());
    }

    @Override
    public void shutdown() {
        connection.close();
        client.shutdown();
    }
}
