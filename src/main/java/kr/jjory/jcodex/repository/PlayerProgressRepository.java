package kr.jjory.jcodex.repository;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.PlayerCodexProgress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 플레이어 진행 상황(player_codex, player_milestones) 테이블에 대한 데이터베이스 작업을 처리하는 클래스입니다.
 */
public class PlayerProgressRepository {
    private final JCodexPlugin plugin;

    public PlayerProgressRepository(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<PlayerCodexProgress> findProgressByUUID(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> registeredItemIds = new HashSet<>();
            Set<Integer> claimedMilestoneIds = new HashSet<>();

            String itemsSql = "SELECT item_id FROM player_codex WHERE uuid = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(itemsSql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        registeredItemIds.add(rs.getString("item_id"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 등록 아이템 조회 중 오류: " + e.getMessage());
            }

            String milestonesSql = "SELECT milestone_id FROM player_milestones WHERE uuid = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(milestonesSql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        claimedMilestoneIds.add(rs.getInt("milestone_id"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 달성 보상 수령 기록 조회 중 오류: " + e.getMessage());
            }

            return new PlayerCodexProgress(uuid, registeredItemIds, claimedMilestoneIds);
        });
    }

    public CompletableFuture<Boolean> registerItem(UUID uuid, String itemId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT IGNORE INTO player_codex (uuid, item_id) VALUES (?, ?)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, itemId);
                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0; // 1이면 성공, 0이면 중복
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 아이템 등록 중 오류: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Set<UUID>> findPlayersByItemId(String itemId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> players = new HashSet<>();
            String sql = "SELECT uuid FROM player_codex WHERE item_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, itemId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        players.add(UUID.fromString(rs.getString("uuid")));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("아이템 등록 플레이어 조회 중 오류: " + e.getMessage());
            }
            return players;
        });
    }

    public CompletableFuture<Void> deleteRegistrationsByItem(String itemId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_codex WHERE item_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, itemId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("아이템 등록 기록 삭제 중 오류: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> claimMilestone(UUID uuid, int milestoneId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_milestones (uuid, milestone_id) VALUES (?, ?)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setInt(2, milestoneId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 달성 보상 수령 기록 중 오류: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
