package kr.jjory.jcodex.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken; // Import TypeToken
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.model.RewardSpec;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material; // Import Material if needed

import java.lang.reflect.Type; // Import Type
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map; // Import Map
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors; // Import Collectors

/**
 * 도감 아이템(codex_items) 테이블에 대한 데이터베이스 작업을 처리하는 클래스입니다.
 */
public class CodexRepository {
    private final JCodexPlugin plugin;
    private final Gson gson = new GsonBuilder().create();
    private static final Type LIST_OF_MAP_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();
    private static final Type MAP_STRING_DOUBLE_TYPE = new TypeToken<Map<String, Double>>() {}.getType();


    public CodexRepository(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    // Helper: List<ItemStack> -> List<Map<String, Object>> (Bukkit 직렬화 사용)
    private List<Map<String, Object>> serializeItemStackList(List<ItemStack> items) {
        if (items == null) return new ArrayList<>();
        return items.stream()
                .map(ItemStack::serialize)
                .collect(Collectors.toList());
    }

    // Helper: List<Map<String, Object>> -> List<ItemStack> (Bukkit 역직렬화 사용)
    private List<ItemStack> deserializeItemStackList(List<Map<String, Object>> maps) {
        if (maps == null) return new ArrayList<>();
        return maps.stream()
                .map(ItemStack::deserialize)
                .collect(Collectors.toList());
    }


    public CompletableFuture<List<CodexItem>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<CodexItem> items = new ArrayList<>();
            String sql = "SELECT * FROM codex_items";
            String currentItemIdForLogging = "UNKNOWN"; // 오류 로깅용 변수

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    String itemId = rs.getString("item_id");
                    currentItemIdForLogging = itemId; // 오류 발생 시 기록할 ID 업데이트
                    String displayName = rs.getString("display_name");
                    CodexCategory category = CodexCategory.valueOf(rs.getString("category"));
                    String rewardJson = rs.getString("reward_json");

                    // try-catch 블록을 while 루프 안으로 이동하여 개별 아이템 오류 처리
                    try {
                        Map<String, Object> rewardMap = gson.fromJson(rewardJson, Map.class);
                        if (rewardMap == null) {
                            plugin.getLogger().warning("아이템 ID '" + itemId + "'의 보상 데이터(JSON)가 null입니다. 빈 보상으로 처리합니다.");
                            rewardMap = new HashMap<>(); // null 대신 빈 Map 사용
                        }

                        List<ItemStack> rewardItems = deserializeItemStackList(
                                gson.fromJson(gson.toJson(rewardMap.get("rewardItems")), LIST_OF_MAP_TYPE)
                        );
                        double money = rewardMap.containsKey("money") ? ((Number) rewardMap.get("money")).doubleValue() : 0.0;
                        Map<String, Double> stats = gson.fromJson(gson.toJson(rewardMap.get("stats")), MAP_STRING_DOUBLE_TYPE);
                        if (stats == null) stats = new HashMap<>();
                        String command = (String) rewardMap.get("command");

                        RewardSpec reward = new RewardSpec(rewardItems, money, stats, command);
                        items.add(new CodexItem(itemId, displayName, category, reward));

                    } catch (Exception e) {
                        // 수정: rs 대신 로깅용 변수 사용
                        plugin.getLogger().severe("보상 JSON 역직렬화 중 오류 발생 (ID: " + currentItemIdForLogging + "): " + e.getMessage());
                        e.printStackTrace();
                        // 오류가 발생한 아이템은 건너뛰고 계속 진행
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("도감 아이템 목록을 불러오는 중 DB 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
            return items;
        });
    }

    public CompletableFuture<Void> saveOrUpdateItem(CodexItem item) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO codex_items (item_id, display_name, category, reward_json) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), category = VALUES(category), reward_json = VALUES(reward_json)";
            String currentItemIdForLogging = item.getItemId(); // 오류 로깅용

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                Map<String, Object> rewardMap = new HashMap<>();
                rewardMap.put("rewardItems", serializeItemStackList(item.getReward().getRewardItems()));
                rewardMap.put("money", item.getReward().getMoney());
                rewardMap.put("stats", item.getReward().getStats());
                rewardMap.put("command", item.getReward().getCommand());
                String rewardJson = gson.toJson(rewardMap);


                pstmt.setString(1, item.getItemId());
                pstmt.setString(2, item.getDisplayName());
                pstmt.setString(3, item.getCategory().name());
                pstmt.setString(4, rewardJson);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("도감 아이템 저장 중 DB 오류 발생 (" + currentItemIdForLogging + "): " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                plugin.getLogger().severe("보상 JSON 직렬화 중 오류 발생 (" + currentItemIdForLogging + "): " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> saveOrUpdateAllFromYaml(List<CodexItem> items) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO codex_items (item_id, display_name, category, reward_json) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), category = VALUES(category), reward_json = VALUES(reward_json)";
            Connection conn = null; // try-with-resources 밖에서도 참조하기 위해
            PreparedStatement pstmt = null;
            String currentItemIdForLogging = "UNKNOWN"; // 오류 로깅용

            try {
                conn = plugin.getDatabaseManager().getConnection();
                pstmt = conn.prepareStatement(sql);

                for (CodexItem item : items) {
                    currentItemIdForLogging = item.getItemId(); // 오류 로깅용 ID 업데이트
                    Map<String, Object> rewardMap = new HashMap<>();
                    rewardMap.put("rewardItems", serializeItemStackList(item.getReward().getRewardItems()));
                    rewardMap.put("money", item.getReward().getMoney());
                    rewardMap.put("stats", item.getReward().getStats());
                    rewardMap.put("command", item.getReward().getCommand());
                    String rewardJson = gson.toJson(rewardMap);

                    pstmt.setString(1, item.getItemId());
                    pstmt.setString(2, item.getDisplayName());
                    pstmt.setString(3, item.getCategory().name());
                    pstmt.setString(4, rewardJson);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().severe("YAML에서 도감 아이템 일괄 저장 중 DB 오류 발생 (마지막 시도 ID: " + currentItemIdForLogging + "): " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                plugin.getLogger().severe("YAML 로드 중 보상 JSON 직렬화 오류 발생 (마지막 시도 ID: " + currentItemIdForLogging + "): " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                // 리소스 수동 해제
                try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
                try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
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
                plugin.getLogger().severe("도감 아이템 삭제 중 DB 오류 발생 (" + itemId + "): " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }
}

