package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 플레이어별 GUI 상태 및 채팅 입력을 관리하는 클래스입니다.
 */
public class GuiManager {

    private final JCodexPlugin plugin;
    private final Map<UUID, Gui> openGuis = new HashMap<>();
    private final Map<UUID, Consumer<String>> chatPrompts = new HashMap<>();

    public GuiManager(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGui(Gui gui) {
        openGuis.put(gui.player.getUniqueId(), gui);
        gui.open();
    }

    public void closeGui(Player player) {
        openGuis.remove(player.getUniqueId());
        chatPrompts.remove(player.getUniqueId()); // GUI 닫힐 때 채팅 프롬프트도 제거
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Gui gui = openGuis.get(player.getUniqueId());
        if (gui != null && event.getInventory().getHolder() == gui) {
            gui.handleClick(event);
        }
    }

    public void promptChatInput(Player player, Consumer<String> callback) {
        chatPrompts.put(player.getUniqueId(), callback);
    }

    public void handleChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        // chatPrompts에 해당 플레이어가 있는지 확인 (isPlayerInPrompt와 동일한 로직)
        if (chatPrompts.containsKey(playerUuid)) {
            event.setCancelled(true);
            String message = event.getMessage();
            Consumer<String> callback = chatPrompts.remove(playerUuid);
            // 콜백을 메인 스레드에서 실행
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(message));
        }
    }

    public Gui getOpenGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    // 플레이어가 채팅 입력 대기 중인지 확인하는 메서드
    public boolean isPlayerInPrompt(Player player) {
        return chatPrompts.containsKey(player.getUniqueId());
    }
}

