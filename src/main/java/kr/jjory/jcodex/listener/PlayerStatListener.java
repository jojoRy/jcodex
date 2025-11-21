package kr.jjory.jcodex.listener;

import kr.jjory.jcodex.service.PlayerStatService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 플레이어 접속 시 저장된 MMOItems 스탯을 다시 적용합니다.
 */
public class PlayerStatListener implements Listener {
    private final PlayerStatService statService;

    public PlayerStatListener(PlayerStatService statService) {
        this.statService = statService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        statService.applyPersistentStats(event.getPlayer());
    }
}