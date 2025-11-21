package kr.jjory.jcodex.util;

import dev.lone.itemsadder.api.CustomStack;
import io.lumine.mythic.lib.api.item.NBTItem;
import kr.jjory.jcodex.model.CodexItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 아이템(ItemStack) 생성 및 관리를 위한 유틸리티 클래스입니다.
 */
public class ItemUtil {

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }

    public static ItemStack createItemFromCodexItem(CodexItem codexItem) {
        String itemId = codexItem.getItemId();

        if (itemId.startsWith("itemsadder:") && Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            String iaId = itemId.substring("itemsadder:".length());
            CustomStack customStack = CustomStack.getInstance(iaId);
            return (customStack != null) ? customStack.getItemStack() : new ItemStack(Material.BARRIER);

        } else if (itemId.startsWith("mmoitems:") && Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            String[] parts = itemId.split(":");
            if (parts.length == 3) {
                // 최종 수정: MMOItems.get() -> MMOItems.plugin 으로 변경
                Type type = MMOItems.plugin.getTypes().get(parts[1]);
                if (type == null) return new ItemStack(Material.BARRIER);

                MMOItemTemplate mmoItemTemplate = MMOItems.plugin.getTemplates().getTemplate(type, parts[2]);
                if (mmoItemTemplate == null) return new ItemStack(Material.BARRIER);

                MMOItem mmoItemData = mmoItemTemplate.newBuilder().build();
                return mmoItemData.newBuilder().build();
            }

        } else if (itemId.startsWith("minecraft:")) {
            try {
                Material material = Material.valueOf(itemId.substring("minecraft:".length()).toUpperCase());
                return new ItemStack(material);
            } catch (IllegalArgumentException ignored) {}
        }

        return new ItemStack(Material.BARRIER);
    }

    public static boolean isSimilar(ItemStack inventoryItem, ItemStack codexStack, String codexItemId) {
        if (codexItemId.startsWith("itemsadder:") && Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            CustomStack customStack = CustomStack.byItemStack(inventoryItem);
            return customStack != null && customStack.getNamespacedID().equalsIgnoreCase(codexItemId.substring("itemsadder:".length()));
        } else if (codexItemId.startsWith("mmoitems:") && Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            NBTItem nbt = NBTItem.get(inventoryItem);
            String nbtId = "mmoitems:" + nbt.getType() + ":" + nbt.getString("MMOITEMS_ITEM_ID");
            return nbt.hasType() && nbtId.equalsIgnoreCase(codexItemId);
        } else {
            return inventoryItem.getType() == codexStack.getType();
        }
    }

    public static String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        // 1. ItemsAdder 아이템 확인
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            CustomStack customStack = CustomStack.byItemStack(item);
            if (customStack != null) {
                return "itemsadder:" + customStack.getNamespacedID();
            }
        }

        // 2. MMOItems 아이템 확인
        if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            NBTItem nbt = NBTItem.get(item);
            if (nbt.hasType() && nbt.hasTag("MMOITEMS_ITEM_ID")) {
                return "mmoitems:" + nbt.getType() + ":" + nbt.getString("MMOITEMS_ITEM_ID");
            }
        }

        // 3. 바닐라 아이템
        return "minecraft:" + item.getType().getKey().getKey();
    }

    /**
     * ItemStack에서 번역 키로 사용할 기본 키 (Material 이름)를 추출합니다.
     * 커스텀 아이템은 아직 지원하지 않습니다.
     */
    public static String getItemKey(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        // 커스텀 아이템(ItemsAdder/MMOItems 등)은 번역 대상에서 제외합니다.
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            if (CustomStack.byItemStack(item) != null) {
                return null;
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            NBTItem nbt = NBTItem.get(item);
            if (nbt.hasType() && nbt.hasTag("MMOITEMS_ITEM_ID")) {
                return null;
            }
        }

        // 현재는 바닐라 아이템의 Material 이름만 반환
        return item.getType().name();
    }
}

