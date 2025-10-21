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

        // 최종 수정: GUI를 즉시 열지 않고, 다음 서버 틱에 안전하게 엽니다.
        // 이것이 클릭 이벤트가 꼬이는 현상을 근본적으로 해결합니다.
        plugin.getServer().getScheduler().runTask(plugin, gui::open);
    }

    public void closeGui(Player player) {
        openGuis.remove(player.getUniqueId());
        chatPrompts.remove(player.getUniqueId());
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Gui gui = openGuis.get(player.getUniqueId());
        if (gui != null) { // 열려있는 GUI가 우리 플러그인의 것이라면
            gui.handleClick(event);
        }
    }

    public void promptChatInput(Player player, Consumer<String> callback) {
        chatPrompts.put(player.getUniqueId(), callback);
    }

    public void handleChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        if (chatPrompts.containsKey(playerUuid)) {
            event.setCancelled(true);
            String message = event.getMessage();
            Consumer<String> callback = chatPrompts.remove(playerUuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(message));
        }
    }

    public Gui getOpenGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    public boolean isPlayerInPrompt(Player player) {
        return chatPrompts.containsKey(player.getUniqueId());
    }
}

