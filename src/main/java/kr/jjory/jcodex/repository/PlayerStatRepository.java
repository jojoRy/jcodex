package kr.jjory.jcodex.repository;

import kr.jjory.jcodex.JCodexPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 플레이어의 MMOItems 스탯 보상을 영구적으로 저장하고 조회하기 위한 레포지토리입니다.
 */
public class PlayerStatRepository {
    private final JCodexPlugin plugin;

    public PlayerStatRepository(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Map<String, Double>> loadStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Double> stats = new HashMap<>();
            String sql = "SELECT stat, value FROM player_stats WHERE uuid = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        stats.put(rs.getString("stat"), rs.getDouble("value"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 스탯 조회 중 오류: " + e.getMessage());
            }
            return stats;
        });
    }

    public CompletableFuture<Void> incrementStat(UUID uuid, String stat, double value) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_stats (uuid, stat, value) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE value = value + VALUES(value)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, stat);
                pstmt.setDouble(3, value);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 스탯 갱신 중 오류: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> decrementStat(UUID uuid, String stat, double value) {
        return CompletableFuture.runAsync(() -> {
            String upsertSql = "INSERT INTO player_stats (uuid, stat, value) VALUES (?, ?, 0) " +
                    "ON DUPLICATE KEY UPDATE value = GREATEST(value - VALUES(value), 0)";
            String cleanupSql = "DELETE FROM player_stats WHERE uuid = ? AND stat = ? AND value <= 0";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement upsertStmt = conn.prepareStatement(upsertSql);
                 PreparedStatement cleanupStmt = conn.prepareStatement(cleanupSql)) {
                upsertStmt.setString(1, uuid.toString());
                upsertStmt.setString(2, stat);
                upsertStmt.setDouble(3, value);
                upsertStmt.executeUpdate();

                cleanupStmt.setString(1, uuid.toString());
                cleanupStmt.setString(2, stat);
                cleanupStmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 스탯 차감 중 오류: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}