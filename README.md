# nablarch-lettuce-adaptor

## 単体テストの実行方法
本モジュールの単体テストを実行するためには、Redisを別途起動しておく必要がある。

テスト用のRedisを手早く起動できるようにするため、Docker Composeのひな形ファイルを `docker-compose` ディレクトリに用意している。

以下のコマンドを実行すると、ローカルでDocker Composeを起動するための設定ファイルが `docker-compose-local` ディレクトリに出力される。

```sh
$ mvn -Pinit-docker-compose -DREDIS_HOST=<ローカルのIPアドレス> resources:resources
```

`<ローカルのIPアドレス>` には、実行時のローカルの IP アドレスを設定する（`localhost`, `127.0.0.1` は不可）。

コマンドラインで `docker-compose-local` に移動し、以下のコマンドを実行することでテスト用のRedisを起動できる。

```sh
$ docker-compose up -d
```

なお、起動のためには以下のTCPポートが全て空いている必要がある。

- 単一構成用
    - `7000`
- Master-Replica構成用
    - Redisインスタンス用
        - `7101`, `7102`, `7103`
    - Sentinelインスタンス用
        - `7111`, `7112`, `7113`
- Cluster構成用
    - Redisインスタンス用
        - `7211`, `7212`, `7221`, `7222`, `7231`, `7232`
    - ノード間通信用
        - `17211`, `17212`, `17221`, `17222`, `17231`, `17232`

### クラスタのテストでエラーが発生する場合
`docker-compose/cluster/create-cluster/create-cluster.sh` の改行コードが CRLF になっていると、 `LettuceClusterRedisClientTest` のテストで以下のようなエラーが発生する。

```
io.lettuce.core.RedisException: io.lettuce.core.cluster.PartitionSelectorException: Cannot determine a partition for slot 5061.
    at io.lettuce.core.LettuceFutures.awaitOrCancel(LettuceFutures.java:135)
    at io.lettuce.core.cluster.ClusterFutureSyncInvocationHandler.handleInvocation(ClusterFutureSyncInvocationHandler.java:123)
    at io.lettuce.core.internal.AbstractInvocationHandler.invoke(AbstractInvocationHandler.java:80)
    ...
```

この場合、 `create-cluster` コンテナのログには次のようなエラーログが出力されている。

```
$ docker-compose logs create_cluster
Attaching to create_cluster
create_cluster    | /usr/local/bin/docker-entrypoint.sh: 16: exec: /redis/create-cluster.sh: not fou              nd
```

`.gitattributes` でシェルスクリプトの改行コードは LF に固定しているが、なんらかの原因で改行コードが CRLF になって上記エラーが発生した場合は、次のように対処することでエラーを解消できる。

1. `create-cluster.sh` の改行コードを LF に変換する
2. Docker Compose で作成したコンテナイメージを削除する

コンテナイメージの削除は、次のコマンドで実行できる。

```sh
$ cd docker-compose

$ docker-compose down --rmi all --volumes
```
