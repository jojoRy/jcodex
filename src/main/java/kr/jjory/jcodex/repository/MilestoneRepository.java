package kr.jjory.jcodex.repository;

import com.google.gson.Gson;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.Milestone;
import kr.jjory.jcodex.model.RewardSpec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 달성도(milestones) 테이블에 대한 데이터베이스 작업을 처리하는 클래스입니다.
 */
public class MilestoneRepository {

    private final JCodexPlugin plugin;
    private final Gson gson = new Gson();

    public MilestoneRepository(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<List<Milestone>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Milestone> milestones = new ArrayList<>();
            String sql = "SELECT * FROM milestones";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String type = rs.getString("type");
                    String categoryStr = rs.getString("category");
                    CodexCategory category = categoryStr != null ? CodexCategory.valueOf(categoryStr) : null;
                    int percent = rs.getInt("percent");
                    String rewardJson = rs.getString("reward_json");
                    RewardSpec reward = gson.fromJson(rewardJson, RewardSpec.class);
                    milestones.add(new Milestone(id, type, category, percent, reward));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("달성도 보상 목록을 불러오는 중 오류 발생: " + e.getMessage());
            }
            return milestones;
        });
    }

    public CompletableFuture<Void> saveOrUpdateAll(List<Milestone> milestones) {
        return CompletableFuture.runAsync(() -> {
            // 기존 데이터 모두 삭제 후 새로 삽입 (간단한 구현)
            String deleteSql = "DELETE FROM milestones";
            String insertSql = "INSERT INTO milestones (type, category, percent, reward_json) VALUES (?, ?, ?, ?)";

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                     PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                    deleteStmt.executeUpdate();

                    for (Milestone milestone : milestones) {
                        insertStmt.setString(1, milestone.getType());
                        insertStmt.setString(2, milestone.getCategory() != null ? milestone.getCategory().name() : null);
                        insertStmt.setInt(3, milestone.getPercent());
                        insertStmt.setString(4, gson.toJson(milestone.getReward()));
                        insertStmt.addBatch();
                    }
                    insertStmt.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("달성도 보상 일괄 저장 중 오류 발생: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
