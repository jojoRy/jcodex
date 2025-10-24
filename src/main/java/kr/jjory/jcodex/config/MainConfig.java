package kr.jjory.jcodex.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * config.yml 파일의 값을 안전하게 가져오기 위한 래퍼 클래스입니다.
 */
public class MainConfig {
    private final String dbHost;
    private final int dbPort;
    private final String dbDatabase;
    private final String dbUsername;
    private final String dbPassword;
    private final int dbPoolSize;

    private final boolean redisEnabled;
    private final String redisHost;
    private final int redisPort;
    private String redisPassword; // ✅ 추가
    private final String redisChannel;

    private final String serverMode;
    private final boolean consumeItem;

    private final List<String> statsOrder;
    private final Map<String, StatDefinition> statDefinitions;

    public MainConfig(FileConfiguration config) {
        this.dbHost = config.getString("mysql.host", "localhost");
        this.dbPort = config.getInt("mysql.port", 3306);
        this.dbDatabase = config.getString("mysql.database", "jcodex");
        this.dbUsername = config.getString("mysql.username", "root");
        this.dbPassword = config.getString("mysql.password", "");
        this.dbPoolSize = config.getInt("mysql.pool-size", 10);

        this.redisEnabled = config.getBoolean("redis.enabled", true);
        this.redisHost = config.getString("redis.host", "localhost");
        this.redisPort = config.getInt("redis.port", 6379);
        this.redisPassword = config.getString("redis.password", ""); // ✅ 추가
        this.redisChannel = config.getString("redis.channel", "jcodex_sync");

        this.serverMode = config.getString("multi-server.mode", "MAIN");
        this.consumeItem = config.getBoolean("register.consume-item", false);

        this.statsOrder = config.getStringList("stats.order");
        this.statDefinitions = new HashMap<>();
        ConfigurationSection statsDefsSection = config.getConfigurationSection("stats.defs");
        if (statsDefsSection != null) {
            for (String key : statsDefsSection.getKeys(false)) {
                String display = statsDefsSection.getString(key + ".display", key);
                int decimals = statsDefsSection.getInt(key + ".decimals", 1);
                this.statDefinitions.put(key, new StatDefinition(display, decimals));
            }
        }
    }

    // Getters
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbDatabase() { return dbDatabase; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public int getDbPoolSize() { return dbPoolSize; }
    public boolean isRedisEnabled() { return redisEnabled; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }
    public String getRedisChannel() { return redisChannel; }
    public String getServerMode() { return serverMode; }
    public boolean isConsumeItem() { return consumeItem; }
    public List<String> getStatsOrder() { return Collections.unmodifiableList(statsOrder); }
    public Map<String, StatDefinition> getStatDefinitions() { return Collections.unmodifiableMap(statDefinitions); }
    public StatDefinition getStatDefinition(String key) {
        return statDefinitions.getOrDefault(key, new StatDefinition(key, 1));
    }

    public static class StatDefinition {
        private final String display;
        private final int decimals;

        public StatDefinition(String display, int decimals) {
            this.display = display;
            this.decimals = decimals;
        }

        public String getDisplay() { return display; }
        public int getDecimals() { return decimals; }
    }
}
