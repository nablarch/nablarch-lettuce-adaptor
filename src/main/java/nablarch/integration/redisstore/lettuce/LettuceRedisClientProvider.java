package nablarch.integration.redisstore.lettuce;


import nablarch.core.repository.di.ComponentFactory;
import nablarch.core.repository.di.ContainerProcessException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link LettuceRedisClient} のインスタンスを提供するクラス。
 * <p>
 * このクラスは、 {@link #setClientList(List)} で設定されたリストの中から、
 * {@link LettuceRedisClient#getType()} が返した値と {@link #setClientType(String)} で設定された値が
 * 一致するインスタンスを検索し、最初に該当したインスタンスを {@link #createObject()} の結果として返す。
 * </p>
 * @author Tanaka Tomoyuki
 */
public class LettuceRedisClientProvider implements ComponentFactory<LettuceRedisClient> {

    private String clientType;
    private List<LettuceRedisClient> clientList;

    @Override
    public LettuceRedisClient createObject() {
        if (clientType == null) {
            throw new ContainerProcessException("clientType must not be null.");
        }
        if (clientList == null) {
            throw new ContainerProcessException("clientList must not be null.");
        }

        return clientList.stream()
                .filter(client -> client.getType().equals(clientType))
                .findFirst()
                .orElseThrow(this::createNoClientMatchesException);
    }

    /**
     * 該当する {@link LettuceRedisClient} が見つからなかったときの例外を構築する。
     * @return 構築した例外
     */
    private ContainerProcessException createNoClientMatchesException() {
        String clientTypes = clientList.stream()
                .map(LettuceRedisClient::getType)
                .collect(Collectors.joining(", ", "[", "]"));
        String message = String.format("No client matches. clientType=%s, clientList=%s", clientType, clientTypes);

        return new ContainerProcessException(message);
    }

    /**
     * 使用する {@link LettuceRedisClient} の実装を識別する値。
     * @param clientType {@link LettuceRedisClient} の実装を識別する値
     */
    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    /**
     * 候補となる {@link LettuceRedisClient} インスタンスのリストを設定する。
     * @param clientList {@link LettuceRedisClient} のリスト
     */
    public void setClientList(List<LettuceRedisClient> clientList) {
        this.clientList = clientList;
    }

}
