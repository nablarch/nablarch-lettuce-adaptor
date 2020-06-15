# nablarch-redisstore-lettuce-adaptor

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
