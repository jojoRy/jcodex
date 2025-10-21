package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 아이템 보상을 등록하기 위한 전용 GUI 클래스입니다.
 */
public class ItemRewardSettingGUI extends Gui {

    private final JCodexPlugin plugin;
    private final CodexItem codexItem;
    private final Gui parentGui;

    // 수정된 슬롯 레이아웃
    private static final int[] ITEM_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] BACK_BUTTON_SLOTS = {9, 10, 11};
    private static final int[] SAVE_BUTTON_SLOTS = {15, 16, 17};

    public ItemRewardSettingGUI(Player player, JCodexPlugin plugin, CodexItem codexItem, Gui parentGui) {
        super(player, 27, "아이템 보상 등록 (최대 9개)");
        this.plugin = plugin;
        this.codexItem = codexItem;
        this.parentGui = parentGui;
        draw();
    }

    private void draw() {
        drawBackground();
        drawControls();

        // 현재 설정된 보상 아이템들을 불러와 배치
        List<ItemStack> currentRewards = codexItem.getReward().getRewardItems();
        if (currentRewards != null) {
            for (int i = 0; i < currentRewards.size(); i++) {
                if (i < ITEM_SLOTS.length) {
                    inventory.setItem(ITEM_SLOTS[i], currentRewards.get(i));
                }
            }
        }
    }

    private void drawBackground() {
        ItemStack background = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, background);
        }
    }

    private void drawControls() {
        ItemStack back = ItemUtil.createItem(Material.BARRIER, "§c뒤로 가기 (저장 안 함)");
        for(int slot : BACK_BUTTON_SLOTS) inventory.setItem(slot, back);

        ItemStack save = ItemUtil.createItem(Material.ANVIL, "§a변경사항 저장하기");
        for(int slot : SAVE_BUTTON_SLOTS) inventory.setItem(slot, save);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // 아이템 슬롯(0-8)을 클릭한 경우, 아이템을 자유롭게 놓거나 집을 수 있도록 허용
        if (isSlotInArray(slot, ITEM_SLOTS)) {
            return;
        }

        // 나머지 모든 버튼 및 배경 클릭 시 기본 동작 방지
        event.setCancelled(true);

        if (isSlotInArray(slot, BACK_BUTTON_SLOTS)) {
            plugin.getGuiManager().openGui(parentGui);
        } else if (isSlotInArray(slot, SAVE_BUTTON_SLOTS)) {
            // 0-8번 슬롯의 모든 아이템을 수집하여 리스트로 저장
            List<ItemStack> newRewardItems = new ArrayList<>();
            for (int itemSlot : ITEM_SLOTS) {
                ItemStack item = inventory.getItem(itemSlot);
                if (item != null && !item.getType().isAir()) {
                    newRewardItems.add(item);
                }
            }
            codexItem.getReward().setRewardItems(newRewardItems);

            player.sendMessage("§a아이템 보상이 설정되었습니다.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

            if (parentGui instanceof RewardSettingGUI rsg) {
                rsg.refresh();
            }
            plugin.getGuiManager().openGui(parentGui);
        }
    }

    private boolean isSlotInArray(int slot, int[] array) {
        for (int i : array) {
            if (i == slot) return true;
        }
        return false;
    }
}

