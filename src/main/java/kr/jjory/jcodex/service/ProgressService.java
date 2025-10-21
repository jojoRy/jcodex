package kr.jjory.jcodex.service;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.repository.CodexRepository;
import kr.jjory.jcodex.repository.PlayerProgressRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 플레이어의 도감 진행도를 계산하는 서비스 클래스입니다.
 */
public class ProgressService {

    private final JCodexPlugin plugin;
    private final CodexRepository codexRepository;
    private final PlayerProgressRepository playerProgressRepository;

    public ProgressService(JCodexPlugin plugin) {
        this.plugin = plugin;
        this.codexRepository = new CodexRepository(plugin);
        this.playerProgressRepository = new PlayerProgressRepository(plugin);
    }

    public CompletableFuture<Map<String, Double>> calculateProgress(UUID uuid, CodexCategory categoryFilter) {
        return codexRepository.findAllAsync().thenCombine(playerProgressRepository.findProgressByUUID(uuid), (allItems, progress) -> {
            Map<String, Double> progressMap = new HashMap<>();

            long totalCount = allItems.size();
            long registeredCount = progress.getRegisteredItemIds().size();
            double globalProgress = (totalCount > 0) ? ((double) registeredCount / totalCount * 100.0) : 0.0;
            progressMap.put("globalProgress", globalProgress);

            if (categoryFilter != null) {
                List<CodexItem> categoryItems = allItems.stream()
                        .filter(item -> item.getCategory() == categoryFilter)
                        .collect(Collectors.toList());

                long categoryTotal = categoryItems.size();
                long categoryRegistered = categoryItems.stream()
                        .filter(item -> progress.getRegisteredItemIds().contains(item.getItemId()))
                        .count();

                double categoryProgress = (categoryTotal > 0) ? ((double) categoryRegistered / categoryTotal * 100.0) : 0.0;
                progressMap.put("categoryProgress", categoryProgress);
            } else {
                progressMap.put("categoryProgress", 0.0);
            }

            return progressMap;
        });
    }

    public CompletableFuture<Map<String, Double>> calculateAllProgress(UUID uuid) {
        return codexRepository.findAllAsync().thenCombine(playerProgressRepository.findProgressByUUID(uuid), (allItems, progress) -> {
            Map<String, Double> progressPercentages = new HashMap<>();

            // 전체 진행도
            long totalCount = allItems.size();
            long registeredCount = progress.getRegisteredItemIds().size();
            progressPercentages.put("global", (totalCount > 0) ? ((double) registeredCount / totalCount * 100.0) : 0.0);

            // 카테고리별 진행도
            for (CodexCategory category : CodexCategory.values()) {
                List<String> categoryItemIds = allItems.stream()
                        .filter(item -> item.getCategory() == category)
                        .map(CodexItem::getItemId)
                        .collect(Collectors.toList());

                long categoryTotal = categoryItemIds.size();
                long categoryRegistered = categoryItemIds.stream()
                        .filter(id -> progress.getRegisteredItemIds().contains(id))
                        .count();

                double percent = (categoryTotal > 0) ? ((double) categoryRegistered / categoryTotal * 100.0) : 0.0;
                progressPercentages.put("category_" + category.name(), percent);
            }
            return progressPercentages;
        });
    }
}
