package kr.jjory.jcodex.service;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.repository.PlayerStatRepository;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
                Bukkit.getScheduler().runTask(plugin, () -> ensureStatsApplied(player, stats))
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

    public CompletableFuture<Void> decrementPersistentStats(UUID uuid, Map<String, Double> stats) {
        if (!isMmoItemsAvailable() || stats.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<?>[] futures = stats.entrySet().stream()
                .map(entry -> repository.decrementStat(uuid, entry.getKey(), entry.getValue()))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenRun(() -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                applyPersistentStats(online);
            }
        });
    }

    public CompletableFuture<Void> removeStatsForDeletedItem(Map<String, Double> stats, Set<UUID> affectedPlayers) {
        if (stats.isEmpty() || affectedPlayers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] futures = affectedPlayers.stream()
                .map(uuid -> decrementPersistentStats(uuid, stats))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.getOnlinePlayers().stream()
                        .filter(player -> affectedPlayers.contains(player.getUniqueId()))
                        .forEach(this::applyPersistentStats)
        ));
    }

    private void ensureStatsApplied(Player player, Map<String, Double> stats) {
        if (!player.isOnline()) {
            return;
        }
        PlayerData playerData = PlayerData.get(player.getUniqueId());
        MMOPlayerData mmoPlayerData = playerData.getMMOPlayerData();
        PersistentDataContainer dataContainer = player.getPersistentDataContainer();
        Set<String> activeStatKeys = stats.keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        stats.forEach((stat, value) -> {
            String modifierKey = buildModifierKey(stat);
            boolean alreadyApplied = mmoPlayerData.getStatMap().getInstance(stat)
                    .getModifiers()
                    .stream()
                    .anyMatch(modifier -> modifier.getKey().equals(modifierKey)
                            && Math.abs(modifier.getValue() - value) < 0.000001);

            if (!alreadyApplied) {
                mmoPlayerData.getStatMap().getInstance(stat)
                        .getModifiers()
                        .removeIf(modifier -> modifier.getKey().equals(modifierKey));
                mmoPlayerData.getStatMap().getInstance(stat).addModifier(new StatModifier(modifierKey, stat, value));
            }

            persistAppliedValue(dataContainer, stat, value);
        });

        removeStaleEntries(activeStatKeys, mmoPlayerData, dataContainer);
    }

    private boolean isMmoItemsAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    private String buildModifierKey(String stat) {
        return "jcodex." + stat.toLowerCase();
    }

    private void persistAppliedValue(PersistentDataContainer container, String stat, double value) {
        NamespacedKey key = new NamespacedKey(plugin, buildModifierKey(stat));
        container.set(key, PersistentDataType.STRING, Double.toString(value));
    }

    private void removeStaleEntries(Set<String> activeStats, MMOPlayerData data, PersistentDataContainer container) {
        data.getStatMap().getInstances().values().forEach(instance ->
                instance.getModifiers().removeIf(modifier -> {
                    if (!modifier.getKey().startsWith("jcodex.")) {
                        return false;
                    }
                    String statName = modifier.getKey().substring("jcodex.".length());
                    if (activeStats.contains(statName.toLowerCase())) {
                        return false;
                    }
                    NamespacedKey key = new NamespacedKey(plugin, modifier.getKey());
                    container.remove(key);
                    return true;
                })
        );
    }
}
