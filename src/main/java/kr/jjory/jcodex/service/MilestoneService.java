package kr.jjory.jcodex.service;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.Milestone;
import kr.jjory.jcodex.model.PlayerCodexProgress;
import kr.jjory.jcodex.repository.MilestoneRepository;
import kr.jjory.jcodex.repository.PlayerProgressRepository;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 달성도 보상 관련 로직을 처리하는 서비스 클래스입니다.
 */
public class MilestoneService {
    private final JCodexPlugin plugin;
    private final MilestoneRepository milestoneRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final ProgressService progressService;
    private final RewardService rewardService;

    public MilestoneService(JCodexPlugin plugin) {
        this.plugin = plugin;
        this.milestoneRepository = new MilestoneRepository(plugin);
        this.playerProgressRepository = new PlayerProgressRepository(plugin);
        this.progressService = plugin.getProgressService();
        this.rewardService = plugin.getRewardService();
    }

    public void checkAndGrantMilestones(Player player) {
        CompletableFuture.allOf(
                milestoneRepository.findAllAsync(),
                playerProgressRepository.findProgressByUUID(player.getUniqueId()),
                progressService.calculateAllProgress(player.getUniqueId())
        ).thenAccept(v -> {
            List<Milestone> allMilestones = milestoneRepository.findAllAsync().join();
            PlayerCodexProgress progress = playerProgressRepository.findProgressByUUID(player.getUniqueId()).join();
            Map<String, Double> progressPercentages = progressService.calculateAllProgress(player.getUniqueId()).join();

            for (Milestone milestone : allMilestones) {
                // 이미 수령한 보상은 건너뜀
                if (progress.getClaimedMilestoneIds().contains(milestone.getId())) {
                    continue;
                }

                boolean achieved = false;
                if ("GLOBAL".equalsIgnoreCase(milestone.getType())) {
                    double globalProgress = progressPercentages.getOrDefault("global", 0.0);
                    if (globalProgress >= milestone.getPercent()) {
                        achieved = true;
                    }
                } else if ("CATEGORY".equalsIgnoreCase(milestone.getType())) {
                    String categoryKey = "category_" + milestone.getCategory().name();
                    double categoryProgress = progressPercentages.getOrDefault(categoryKey, 0.0);
                    if (categoryProgress >= milestone.getPercent()) {
                        achieved = true;
                    }
                }

                if (achieved) {
                    grantMilestoneReward(player, milestone);
                }
            }
        });
    }

    private void grantMilestoneReward(Player player, Milestone milestone) {
        playerProgressRepository.claimMilestone(player.getUniqueId(), milestone.getId()).thenRun(() -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String message = "§a[JCodex] 달성도 보상을 획득했습니다! (" + milestone.getType() + " " + milestone.getPercent() + "%)";
                player.sendMessage(message);
                rewardService.grantRewardSpec(player, milestone.getReward());
            });
        });
    }
}
