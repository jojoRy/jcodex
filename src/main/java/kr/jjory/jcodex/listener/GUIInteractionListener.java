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

        // 플레이어에게 열려있는 GUI 정보를 가져옵니다.
        Gui openGui = plugin.getGuiManager().getOpenGui(player);
        if (openGui == null) {
            return; // 우리 플러그인 GUI가 아니면 무시합니다.
        }

        // 현재 이벤트가 발생한 GUI 뷰의 최상단 인벤토리 홀더가
        // 우리가 기록해둔 GUI 객체와 일치하는지 확인합니다.
        // 이 검사를 통해 우리 GUI와 관련된 클릭 이벤트만 정확히 처리할 수 있습니다.
        if (event.getView().getTopInventory().getHolder() == openGui) {
            plugin.getGuiManager().handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // 채팅 입력 대기 중인 플레이어는 GUI를 닫지 않도록 하여 버그 방지
            if (!plugin.getGuiManager().isPlayerInPrompt(player)) {
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

