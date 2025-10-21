package kr.jjory.jcodex.service;

import com.google.gson.JsonObject;
import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.gui.CodexAdminGUI;
import kr.jjory.jcodex.gui.CodexMainGUI;
import kr.jjory.jcodex.gui.Gui;
import kr.jjory.jcodex.model.CodexItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Redis Pub/Sub을 이용한 서버 간 동기화를 처리하는 서비스 클래스입니다.
 */
public class SyncService {
    private final JCodexPlugin plugin;

    public SyncService(JCodexPlugin plugin) {
        this.plugin = plugin;
        setupSubscription();
    }

    private void setupSubscription() {
        plugin.getRedisClient().subscribe((channel, message) -> {
            // 메인 스레드에서 처리
            Bukkit.getScheduler().runTask(plugin, () -> handleMessage(message));
        });
    }

    private void handleMessage(String message) {
        // 간단한 구현: 어떤 메시지든 받으면 모든 플레이어의 GUI를 새로고침
        for (Player player : Bukkit.getOnlinePlayers()) {
            Gui openGui = plugin.getGuiManager().getOpenGui(player);
            if (openGui instanceof CodexMainGUI mainGui) {
                mainGui.refresh();
            } else if (openGui instanceof CodexAdminGUI adminGui) {
                adminGui.refresh();
            }
        }
        if (message.startsWith("RELOAD")) {
            plugin.getLogger().info("다른 서버로부터 리로드 신호를 받아 설정을 다시 불러옵니다.");
            plugin.getConfigManager().reloadConfigs();
        }
    }

    public void publishPlayerRegister(UUID uuid, String itemId) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "REGISTER");
        json.addProperty("uuid", uuid.toString());
        json.addProperty("itemId", itemId);
        plugin.getRedisClient().publish(json.toString());
    }

    public void publishItemUpdate(CodexItem item) {
        plugin.getRedisClient().publish("UPDATE_ITEM:" + item.getItemId());
    }

    public void publishItemDelete(String itemId) {
        plugin.getRedisClient().publish("DELETE_ITEM:" + itemId);
    }

    public void publishReload() {
        plugin.getRedisClient().publish("RELOAD");
    }
}
