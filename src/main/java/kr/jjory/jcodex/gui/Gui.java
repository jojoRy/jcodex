package kr.jjory.jcodex.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 모든 GUI 클래스의 기반이 되는 추상 클래스입니다.
 */
public abstract class Gui implements InventoryHolder {

    protected final Player player;
    protected final Inventory inventory;
    protected final FileConfiguration guiConfig; // gui.yml 접근용

    public Gui(Player player, int size, String titleKey, FileConfiguration guiConfig) {
        this.player = player;
        this.guiConfig = guiConfig; // guiConfig 저장
        // gui.yml에서 제목 가져오기
        String title = guiConfig.getString("titles." + titleKey, "GUI Title Error");
        this.inventory = Bukkit.createInventory(this, size, colorize(title));
    }

    // 생성자 오버로딩 (기존 방식 호환 - 이제 사용 안 함)
    /*
    public Gui(Player player, int size, String title) {
        this.player = player;
        this.guiConfig = null; // guiConfig 사용 안 함
        this.inventory = Bukkit.createInventory(this, size, colorize(title));
    }
    */


    public void open() {
        player.openInventory(inventory);
    }

    public abstract void handleClick(InventoryClickEvent event);

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // gui.yml에서 아이템 스택 생성 (Material, CustomModelData 포함)
    protected ItemStack createGuiItem(String path, String... replacements) {
        // Material 읽기 (기본값 BARRIER)
        String materialName = guiConfig.getString(path + ".material", "BARRIER").toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.BARRIER;
            Bukkit.getLogger().warning("[JCodex] gui.yml의 " + path + ".material 에 잘못된 아이템 종류 '" + materialName + "'가 설정되어 있습니다.");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // CustomModelData 읽기 (기본값 0)
            int customModelData = guiConfig.getInt(path + ".custom_model_data", 0);
            if (customModelData != 0) {
                meta.setCustomModelData(customModelData);
            }

            String name = guiConfig.getString(path + ".name", " ");
            List<String> lore = guiConfig.getStringList(path + ".lore");

            // Placeholder 교체
            if (replacements.length > 0 && replacements.length % 2 == 0) {
                for (int i = 0; i < replacements.length; i += 2) {
                    name = name.replace(replacements[i], replacements[i + 1]);
                    final int index = i;
                    lore = lore.stream()
                            .map(line -> line.replace(replacements[index], replacements[index + 1]))
                            .collect(Collectors.toList());
                }
            }

            meta.setDisplayName(colorize(name));
            meta.setLore(lore.stream().map(this::colorize).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected Material loadMaterialFromConfig(String path) {
        if (guiConfig == null || !guiConfig.contains(path)) {
            return null;
        }

        String materialName = guiConfig.getString(path);
        if (materialName == null || materialName.trim().isEmpty()) {
            return null;
        }

        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            Bukkit.getLogger().warning("[JCodex] gui.yml의 " + path + " 에 잘못된 아이템 종류 '" + materialName + "'가 설정되어 있습니다.");
        }
        return material;
    }


    // gui.yml에서 메시지 가져오기
    protected String getMessage(String path, String... replacements) {
        String message = guiConfig.getString("messages." + path, "Message not found: " + path);
        if (replacements.length > 0 && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return colorize(message);
    }

    // gui.yml에서 포맷 가져오기
    protected String formatNumber(String formatKey, double number) {
        String pattern = guiConfig.getString("formats." + formatKey, "#.#");
        return new DecimalFormat(pattern).format(number);
    }

    // 색상 코드 변환 메서드
    protected String colorize(String text) {
        // Paper API의 MiniMessage 형식을 사용하는 것이 더 권장되지만,
        // 호환성을 위해 기본적인 Bukkit 방식(&)을 사용합니다.
        return ChatColor.translateAlternateColorCodes('&', text != null ? text : "");
    }
}

