package kr.jjory.jcodex.listener;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.gui.Gui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * GUI 상호작용 관련 이벤트를 처리하는 리스너 클래스입니다.
 */
public class GUIInteractionListener implements Listener {

    private final JCodexPlugin plugin;

    public GUIInteractionListener(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Gui openGui = plugin.getGuiManager().getOpenGui(player);
        if (openGui == null) {
            return; // 우리 플러그인 GUI가 아니면 무시합니다.
        }

        // 최종 수정: 이벤트가 발생한 인벤토리 뷰의 최상단 인벤토리 홀더가
        // 열려있는 GUI 객체와 일치하는 경우, 먼저 이벤트를 취소합니다.
        if (event.getView().getTopInventory().getHolder() == openGui) {
            event.setCancelled(true); // 여기서 먼저 취소!
            openGui.handleClick(event);
        }
        // 플레이어 인벤토리 클릭 시 (GUI 하단) -> 해당 GUI의 handleClick 호출
        else if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() == player) {
            // 몇몇 GUI는 하단 인벤토리 클릭도 처리해야 할 수 있으므로 handleClick 호출
            openGui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // 채팅 입력 대기 중이 아니면서, 현재 열린 GUI가 닫히는 GUI와 일치할 때만 closeGui 호출
            if (!plugin.getGuiManager().isPlayerInPrompt(player) && event.getInventory().getHolder() == plugin.getGuiManager().getOpenGui(player)) {
                plugin.getGuiManager().closeGui(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (plugin.getGuiManager().isPlayerInPrompt(event.getPlayer())) {
            plugin.getGuiManager().handleChatInput(event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGuiManager().closeGui(event.getPlayer());
    }
}

