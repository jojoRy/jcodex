package kr.jjory.jcodex.loader;

import com.google.gson.Gson;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.model.Milestone;
import kr.jjory.jcodex.model.RewardSpec;
import kr.jjory.jcodex.repository.CodexRepository;
import kr.jjory.jcodex.repository.MilestoneRepository;
import kr.jjory.jcodex.util.YamlUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * YAML 설정 파일을 읽어 데이터베이스와 동기화하는 클래스입니다.
 */
public class CodexDataLoader {
    private final JCodexPlugin plugin;
    private final CodexRepository codexRepository;
    private final MilestoneRepository milestoneRepository;
    private final Gson gson = new Gson();

    public CodexDataLoader(JCodexPlugin plugin) {
        this.plugin = plugin;
        this.codexRepository = new CodexRepository(plugin);
        this.milestoneRepository = new MilestoneRepository(plugin);
    }

    public CompletableFuture<Void> loadAllToDatabase() {
        List<CodexItem> codexItems = loadCodexItems();
        List<Milestone> milestones = loadMilestones();

        return codexRepository.saveOrUpdateAllFromYaml(codexItems)
                .thenCompose(v -> milestoneRepository.saveOrUpdateAll(milestones));
    }

    private List<CodexItem> loadCodexItems() {
        List<CodexItem> items = new ArrayList<>();
        FileConfiguration config = plugin.getConfigManager().getCodexItemsConfig();
        List<Map<?, ?>> itemList = config.getMapList("items");

        for (Map<?, ?> itemMap : itemList) {
            String id = (String) itemMap.get("id");
            String displayName = ((String) itemMap.get("display_name")).replace("&", "§");
            CodexCategory category = CodexCategory.valueOf(((String) itemMap.get("category")).toUpperCase());

            Map<String, Object> rewardMap = (Map<String, Object>) itemMap.get("reward");
            RewardSpec reward = parseReward(rewardMap);

            items.add(new CodexItem(id, displayName, category, reward));
        }
        return items;
    }

    private List<Milestone> loadMilestones() {
        List<Milestone> milestones = new ArrayList<>();
        FileConfiguration config = plugin.getConfigManager().getMilestonesConfig();
        List<Map<?, ?>> milestoneList = config.getMapList("milestones");

        for (Map<?, ?> milestoneMap : milestoneList) {
            String type = (String) milestoneMap.get("type");
            CodexCategory category = milestoneMap.containsKey("category") ? CodexCategory.valueOf(((String) milestoneMap.get("category")).toUpperCase()) : null;
            int percent = (int) milestoneMap.get("percent");
            Map<String, Object> rewardMap = (Map<String, Object>) milestoneMap.get("reward");
            RewardSpec reward = parseReward(rewardMap);

            milestones.add(new Milestone(0, type, category, percent, reward));
        }
        return milestones;
    }

    private RewardSpec parseReward(Map<String, Object> rewardMap) {
        if (rewardMap == null) return new RewardSpec(null, 0, new HashMap<>(), null);

        double money = ((Number) rewardMap.getOrDefault("money", 0)).doubleValue();
        String command = (String) rewardMap.get("command");
        Map<String, Double> stats = new HashMap<>();
        if (rewardMap.containsKey("stats")) {
            Map<String, Object> statsMap = (Map<String, Object>) rewardMap.get("stats");
            for (Map.Entry<String, Object> entry : statsMap.entrySet()) {
                stats.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
            }
        }
        return new RewardSpec(null, money, stats, command);
    }

    public CompletableFuture<Void> exportAllFromDatabase() {
        return codexRepository.findAllAsync().thenCompose(codexItems -> {
            return milestoneRepository.findAllAsync().thenAccept(milestones -> {
                try {
                    exportCodexItems(codexItems);
                    exportMilestones(milestones);
                } catch (IOException e) {
                    throw new RuntimeException("파일로 내보내는 중 오류 발생", e);
                }
            });
        });
    }

    private void exportCodexItems(List<CodexItem> items) throws IOException {
        File file = new File(plugin.getDataFolder(), "codex_items_export.yml");
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (CodexItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getItemId());
            itemMap.put("display_name", item.getDisplayName().replace("§", "&"));
            itemMap.put("category", item.getCategory().name());

            Map<String, Object> rewardMap = new HashMap<>();
            RewardSpec reward = item.getReward();
            if (reward.getMoney() > 0) rewardMap.put("money", reward.getMoney());
            if (reward.getCommand() != null && !reward.getCommand().isEmpty()) rewardMap.put("command", reward.getCommand());
            if (reward.getStats() != null && !reward.getStats().isEmpty()) rewardMap.put("stats", reward.getStats());
            if (!rewardMap.isEmpty()) itemMap.put("reward", rewardMap);

            itemList.add(itemMap);
        }
        config.set("items", itemList);
        config.save(file);
    }

    private void exportMilestones(List<Milestone> milestones) throws IOException {
        File file = new File(plugin.getDataFolder(), "milestones_export.yml");
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> milestoneList = new ArrayList<>();
        for (Milestone milestone : milestones) {
            Map<String, Object> milestoneMap = new HashMap<>();
            milestoneMap.put("type", milestone.getType());
            if (milestone.getCategory() != null) milestoneMap.put("category", milestone.getCategory().name());
            milestoneMap.put("percent", milestone.getPercent());

            Map<String, Object> rewardMap = new HashMap<>();
            RewardSpec reward = milestone.getReward();
            if (reward.getMoney() > 0) rewardMap.put("money", reward.getMoney());
            if (reward.getCommand() != null && !reward.getCommand().isEmpty()) rewardMap.put("command", reward.getCommand());
            if (reward.getStats() != null && !reward.getStats().isEmpty()) rewardMap.put("stats", reward.getStats());
            if (!rewardMap.isEmpty()) milestoneMap.put("reward", rewardMap);

            milestoneList.add(milestoneMap);
        }
        config.set("milestones", milestoneList);
        config.save(file);
    }
}
