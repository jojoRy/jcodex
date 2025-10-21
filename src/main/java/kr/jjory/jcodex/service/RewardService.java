package kr.jjory.jcodex.service;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.model.RewardSpec;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 보상 지급 관련 로직을 처리하는 서비스 클래스입니다.
 */
public class RewardService {

    private final JCodexPlugin plugin;

    public RewardService(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    public void grantUnlockReward(Player player, CodexItem codexItem) {
        grantRewardSpec(player, codexItem.getReward());
    }

    public void grantRewardSpec(Player player, RewardSpec reward) {
        // 아이템 보상
        List<ItemStack> rewardItems = reward.getRewardItems();
        if (rewardItems != null && !rewardItems.isEmpty()) {
            for (ItemStack item : rewardItems) {
                player.getInventory().addItem(item.clone());
            }
            player.sendMessage("§a[JCodex] 보상으로 아이템 " + rewardItems.size() + "개를 받았습니다.");
        }

        // 돈 보상
        if (reward.getMoney() > 0 && JCodexPlugin.getEconomy() != null) {
            JCodexPlugin.getEconomy().depositPlayer(player, reward.getMoney());
            player.sendMessage("§a[JCodex] 보상으로 §e" + reward.getMoney() + "§a원을 받았습니다.");
        }

        // 스탯 보상 (MMOItems)
        if (!reward.getStats().isEmpty() && Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            PlayerData playerData = PlayerData.get(player.getUniqueId());
            MMOPlayerData mmoPlayerData = playerData.getMMOPlayerData();
            for (Map.Entry<String, Double> entry : reward.getStats().entrySet()) {
                String stat = entry.getKey();
                double value = entry.getValue();

                // 최종 수정: StatModifier(key, stat, value) 생성자를 사용하여 API 요구사항 충족
                String modifierKey = "jcodex." + stat.toLowerCase();
                StatModifier statModifier = new StatModifier(modifierKey, stat, value);
                mmoPlayerData.getStatMap().getInstance(stat).addModifier(statModifier);

                player.sendMessage("§a[JCodex] 보상으로 스탯 §b" + stat + " + " + value + "§a를 받았습니다.");
            }
        }

        // 명령어 보상
        if (reward.getCommand() != null && !reward.getCommand().isEmpty()) {
            String command = reward.getCommand().replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}

