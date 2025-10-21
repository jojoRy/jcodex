package kr.jjory.jcodex.service;

import kr.jjory.jcodex.JCodexPlugin;
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
                // 아이템 소모
                if (plugin.getConfigManager().getMainConfig().isConsumeItem()) {
                    removeItem(player, codexItem);
                }

                // 등록 성공 메시지 및 효과
                player.sendMessage("§a[JCodex] '§f" + codexItem.getDisplayName() + "§a' 아이템을 도감에 등록했습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

                // 해금 보상 지급
                rewardService.grantUnlockReward(player, codexItem);

                // 달성도 체크 및 보상
                milestoneService.checkAndGrantMilestones(player);

                // 다른 서버에 동기화 메시지 전송
                syncService.publishPlayerRegister(player.getUniqueId(), codexItem.getItemId());

            } else {
                player.sendMessage("§e[JCodex] 이미 등록한 아이템입니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        });
    }

    private boolean hasItem(Player player, CodexItem codexItem) {
        ItemStack targetStack = ItemUtil.createItemFromCodexItem(codexItem);
        // 간단한 구현: ID가 같으면 동일 아이템으로 간주 (NBT 등은 무시)
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
