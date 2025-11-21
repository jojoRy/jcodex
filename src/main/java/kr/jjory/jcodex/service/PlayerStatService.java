package kr.jjory.jcodex.service;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.repository.PlayerStatRepository;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MMOItems 스탯 보상의 영구 저장 및 재적용을 담당하는 서비스입니다.
 */
public class PlayerStatService {
    private final JCodexPlugin plugin;
    private final PlayerStatRepository repository;

    public PlayerStatService(JCodexPlugin plugin) {
        this.plugin = plugin;
        this.repository = new PlayerStatRepository(plugin);
    }

    public void applyPersistentStats(Player player) {
        if (!isMmoItemsAvailable()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        repository.loadStats(uuid).thenAccept(stats ->
                Bukkit.getScheduler().runTask(plugin, () -> applyStats(player, stats))
        );
    }

    public void incrementPersistentStats(Player player, Map<String, Double> stats) {
        if (!isMmoItemsAvailable() || stats.isEmpty()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        CompletableFuture<?>[] futures = stats.entrySet().stream()
                .map(entry -> repository.incrementStat(uuid, entry.getKey(), entry.getValue()))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).thenRun(() -> applyPersistentStats(player));
    }

    private void applyStats(Player player, Map<String, Double> stats) {
        if (!player.isOnline()) {
            return;
        }
        PlayerData playerData = PlayerData.get(player.getUniqueId());
        MMOPlayerData mmoPlayerData = playerData.getMMOPlayerData();

        stats.forEach((stat, value) -> {
            String modifierKey = buildModifierKey(stat);
            mmoPlayerData.getStatMap().getInstance(stat)
                    .getModifiers()
                    .removeIf(modifier -> modifier.getKey().equals(modifierKey));
            mmoPlayerData.getStatMap().getInstance(stat).addModifier(new StatModifier(modifierKey, stat, value));
        });
    }

    private boolean isMmoItemsAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    private String buildModifierKey(String stat) {
        return "jcodex." + stat.toLowerCase();
    }
}