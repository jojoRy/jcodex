package kr.jjory.jcodex.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.model.RewardSpec;
import kr.jjory.jcodex.util.ItemStackAdapter;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 도감 아이템(codex_items) 테이블에 대한 데이터베이스 작업을 처리하는 클래스입니다.
 */
public class CodexRepository {
    private final JCodexPlugin plugin;
    private final Gson gson;

    public CodexRepository(JCodexPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                .create();
    }

    public CompletableFuture<List<CodexItem>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<CodexItem> items = new ArrayList<>();
            String sql = "SELECT * FROM codex_items";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    String itemId = rs.getString("item_id");
                    String displayName = rs.getString("display_name");
                    CodexCategory category = CodexCategory.valueOf(rs.getString("category"));
                    String rewardJson = rs.getString("reward_json");
                    RewardSpec reward = gson.fromJson(rewardJson, RewardSpec.class);
                    items.add(new CodexItem(itemId, displayName, category, reward));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("도감 아이템 목록을 불러오는 중 오류 발생: " + e.getMessage());
            }
            return items;
        });
    }

    public CompletableFuture<Void> saveOrUpdateItem(CodexItem item) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO codex_items (item_id, display_name, category, reward_json) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), category = VALUES(category), reward_json = VALUES(reward_json)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, item.getItemId());
                pstmt.setString(2, item.getDisplayName());
                pstmt.setString(3, item.getCategory().name());
                pstmt.setString(4, gson.toJson(item.getReward()));
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("도감 아이템 저장 중 오류 발생: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> saveOrUpdateAllFromYaml(List<CodexItem> items) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO codex_items (item_id, display_name, category, reward_json) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), category = VALUES(category), reward_json = VALUES(reward_json)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                for (CodexItem item : items) {
                    pstmt.setString(1, item.getItemId());
                    pstmt.setString(2, item.getDisplayName());
                    pstmt.setString(3, item.getCategory().name());
                    pstmt.setString(4, gson.toJson(item.getReward()));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().severe("YAML에서 도감 아이템 일괄 저장 중 오류 발생: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> deleteItem(String itemId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM codex_items WHERE item_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, itemId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("도감 아이템 삭제 중 오류 발생: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}

