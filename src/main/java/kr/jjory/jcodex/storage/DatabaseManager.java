package kr.jjory.jcodex.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.config.MainConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 데이터베이스 연결(HikariCP) 및 테이블 생성을 관리하는 클래스입니다.
 */
public class DatabaseManager {
    private final JCodexPlugin plugin;
    private final HikariDataSource dataSource;

    public DatabaseManager(JCodexPlugin plugin, MainConfig config) {
        this.plugin = plugin;
        this.dataSource = createDataSource(config);
        createTables();
    }

    private HikariDataSource createDataSource(MainConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getDbHost() + ":" + config.getDbPort() + "/" + config.getDbDatabase() + "?useSSL=false");
        hikariConfig.setUsername(config.getDbUsername());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setMaximumPoolSize(config.getDbPoolSize());
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(hikariConfig);
    }

    private void createTables() {
        String codexItemsTable = "CREATE TABLE IF NOT EXISTS codex_items (" +
                "item_id VARCHAR(128) PRIMARY KEY, " +
                "display_name VARCHAR(128) NOT NULL, " +
                "category VARCHAR(32) NOT NULL, " +
                "reward_json TEXT" +
                ");";

        String playerCodexTable = "CREATE TABLE IF NOT EXISTS player_codex (" +
                "uuid CHAR(36) NOT NULL, " +
                "item_id VARCHAR(128) NOT NULL, " +
                "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (uuid, item_id)" +
                ");";

        String milestonesTable = "CREATE TABLE IF NOT EXISTS milestones (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "type VARCHAR(16) NOT NULL, " +
                "category VARCHAR(32), " +
                "percent INT NOT NULL, " +
                "reward_json TEXT" +
                ");";

        String playerMilestonesTable = "CREATE TABLE IF NOT EXISTS player_milestones (" +
                "uuid CHAR(36) NOT NULL, " +
                "milestone_id INT NOT NULL, " +
                "claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (uuid, milestone_id)" +
                ");";

        String playerStatsTable = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid CHAR(36) NOT NULL, " +
                "stat VARCHAR(64) NOT NULL, " +
                "value DOUBLE NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (uuid, stat)" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(codexItemsTable);
            stmt.execute(playerCodexTable);
            stmt.execute(milestonesTable);
            stmt.execute(playerMilestonesTable);
            stmt.execute(playerStatsTable);
            plugin.getLogger().info("데이터베이스 테이블이 성공적으로 확인/생성되었습니다.");
        } catch (SQLException e) {
            plugin.getLogger().severe("데이터베이스 테이블 생성 중 심각한 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
