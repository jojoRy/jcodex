package kr.jjory.jcodex.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * 모든 GUI 클래스의 기반이 되는 추상 클래스입니다.
 */
public abstract class Gui implements InventoryHolder {

    protected final Player player;
    protected final Inventory inventory;

    public Gui(Player player, int size, String title) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public abstract void handleClick(InventoryClickEvent event);

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
