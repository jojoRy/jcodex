package kr.jjory.jcodex.storage;

// import io.lettuce.core.RedisClient; // 이 줄을 삭제하거나 주석 처리합니다.
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.config.MainConfig;

import java.util.function.BiConsumer;

/**
 * Redis 연결 및 Pub/Sub 메시징을 관리하는 클래스입니다. (Lettuce 클라이언트 사용)
 */
public class RedisClient {

    private final JCodexPlugin plugin;
    private final String channel;
    private io.lettuce.core.RedisClient lettuceClient; // FQN(전체 경로) 사용
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private StatefulRedisConnection<String, String> publishConnection;

    public RedisClient(JCodexPlugin plugin, MainConfig config) {
        this.plugin = plugin;
        if (config.isRedisEnabled()) {
            this.channel = config.getRedisChannel();
            try {
                String redisUri = "redis://:" + config.getRedisPassword() + "@" + config.getRedisHost() + ":" + config.getRedisPort();
                this.lettuceClient = io.lettuce.core.RedisClient.create(redisUri); // FQN 사용
                this.pubSubConnection = lettuceClient.connectPubSub();
                this.publishConnection = lettuceClient.connect();
                plugin.getLogger().info("Lettuce Redis 클라이언트가 성공적으로 연결되었습니다.");
            } catch (Exception e) {
                plugin.getLogger().severe("Lettuce Redis 클라이언트 연결에 실패했습니다: " + e.getMessage());
                this.lettuceClient = null;
            }
        } else {
            this.channel = null;
        }
    }

    public void subscribe(BiConsumer<String, String> onMessage) {
        if (pubSubConnection == null) return;

        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                onMessage.accept(channel, message);
            }
        });

        pubSubConnection.async().subscribe(channel);
        plugin.getLogger().info("Redis 채널 '" + channel + "' 구독을 시작합니다.");
    }

    public void publish(String message) {
        if (publishConnection == null) return;

        publishConnection.async().publish(channel, message).exceptionally(ex -> {
            plugin.getLogger().severe("Redis 메시지 발행 중 오류 발생: " + ex.getMessage());
            return null;
        });
    }

    public void close() {
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
        if (publishConnection != null) {
            publishConnection.close();
        }
        if (lettuceClient != null) {
            lettuceClient.shutdown();
        }
    }
}

