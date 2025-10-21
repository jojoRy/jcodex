package kr.jjory.jcodex.service;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.gui.CodexMainGUI;
import kr.jjory.jcodex.gui.Gui;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.repository.PlayerProgressRepository;
import kr.jjory.jcodex.util.ItemUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 도감 아이템 등록과 관련된 핵심 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
public class CodexService {
    private final JCodexPlugin plugin;
    private final PlayerProgressRepository playerProgressRepository;
    private final RewardService rewardService;
    private final MilestoneService milestoneService;
    private final SyncService syncService;

    public CodexService(JCodexPlugin plugin) {
        this.plugin = plugin;
        this.playerProgressRepository = new PlayerProgressRepository(plugin);
        this.rewardService = plugin.getRewardService();
        this.milestoneService = plugin.getMilestoneService();
        this.syncService = plugin.getSyncService();
    }

    public void registerItem(Player player, CodexItem codexItem) {
        // 인벤토리에서 해당 아이템 확인
        if (!hasItem(player, codexItem)) {
            player.sendMessage("§c[JCodex] 도감에 등록할 아이템을 가지고 있지 않습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        playerProgressRepository.registerItem(player.getUniqueId(), codexItem.getItemId()).thenAccept(success -> {
            if (success) {
                // Bukkit API 관련 작업은 메인 스레드에서 실행
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // 아이템 소모
                    if (plugin.getConfigManager().getMainConfig().isConsumeItem()) {
                        removeItem(player, codexItem);
                    }

                    // 등록 성공 메시지 및 효과
                    player.sendMessage("§a[JCodex] '§f" + codexItem.getDisplayName() + "§a' 아이템을 도감에 등록했습니다!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

                    // 해금 보상 지급
                    rewardService.grantUnlockReward(player, codexItem);

                    // 수정된 부분: 현재 열려있는 GUI를 즉시 새로고침합니다.
                    Gui openGui = plugin.getGuiManager().getOpenGui(player);
                    if (openGui instanceof CodexMainGUI mainGui) {
                        mainGui.refresh();
                    }
                });

                // DB/네트워크 작업은 비동기로 계속 진행
                milestoneService.checkAndGrantMilestones(player);
                syncService.publishPlayerRegister(player.getUniqueId(), codexItem.getItemId());

            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§e[JCodex] 이미 등록한 아이템입니다.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                });
            }
        });
    }

    private boolean hasItem(Player player, CodexItem codexItem) {
        ItemStack targetStack = ItemUtil.createItemFromCodexItem(codexItem);
        for (ItemStack inventoryItem : player.getInventory().getContents()) {
            if (inventoryItem != null && !inventoryItem.getType().isAir()) {
                if (ItemUtil.isSimilar(inventoryItem, targetStack, codexItem.getItemId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeItem(Player player, CodexItem codexItem) {
        ItemStack targetStack = ItemUtil.createItemFromCodexItem(codexItem);
        for (ItemStack inventoryItem : player.getInventory().getContents()) {
            if (inventoryItem != null && ItemUtil.isSimilar(inventoryItem, targetStack, codexItem.getItemId())) {
                inventoryItem.setAmount(inventoryItem.getAmount() - 1);
                return;
            }
        }
    }
}

