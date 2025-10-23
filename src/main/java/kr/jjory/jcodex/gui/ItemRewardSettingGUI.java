package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.repository.CodexRepository; // Repository 임포트 추가
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 아이템 보상을 등록하기 위한 전용 GUI 클래스입니다.
 */
public class ItemRewardSettingGUI extends Gui {

    private final JCodexPlugin plugin;
    private final CodexRepository codexRepository; // Repository 추가
    private final CodexItem codexItem;
    private final Gui parentGui;

    // 슬롯 레이아웃
    private static final int[] ITEM_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] BACK_BUTTON_SLOTS = {9, 10, 11};
    private static final int[] SAVE_BUTTON_SLOTS = {15, 16, 17};

    public ItemRewardSettingGUI(Player player, JCodexPlugin plugin, CodexItem codexItem, Gui parentGui) {
        super(player, 27, "item_reward_setting", plugin.getConfigManager().getGuiConfig());
        this.plugin = plugin;
        this.codexRepository = new CodexRepository(plugin); // Repository 초기화
        this.codexItem = codexItem;
        this.parentGui = parentGui;
        draw();
    }

    private void draw() {
        drawBackground();
        drawControls();
        drawRewardItems();
    }

    private void drawBackground() {
        ItemStack background = createGuiItem("items.background");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, background);
        }
    }

    private void drawControls() {
        ItemStack back = createGuiItem("items.item_reward_back_button");
        for(int slot : BACK_BUTTON_SLOTS) inventory.setItem(slot, back);

        ItemStack save = createGuiItem("items.item_reward_save_button");
        for(int slot : SAVE_BUTTON_SLOTS) inventory.setItem(slot, save);
    }

    private void drawRewardItems() {
        List<ItemStack> currentRewards = codexItem.getReward().getRewardItems();
        for (int slot : ITEM_SLOTS) {
            inventory.setItem(slot, null); // 먼저 비우기
        }
        if (currentRewards != null) {
            for (int i = 0; i < currentRewards.size(); i++) {
                if (i < ITEM_SLOTS.length) {
                    inventory.setItem(ITEM_SLOTS[i], currentRewards.get(i));
                }
            }
        }
    }


    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true); // 모든 클릭 기본 동작 취소

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        int slot = event.getRawSlot();

        // GUI 상단 클릭
        if (clickedInventory.getHolder() == this) {
            // 저장 버튼 클릭
            if (isSlotInArray(slot, SAVE_BUTTON_SLOTS)) {
                saveRewardItemsAndClose(); // 수정: 저장 후 닫는 메서드 호출
            }
            // 뒤로가기 버튼 클릭
            else if (isSlotInArray(slot, BACK_BUTTON_SLOTS)) {
                // 저장하지 않고 부모 GUI 열기 (다음 틱)
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(parentGui));
            }
            // 등록된 보상 아이템 슬롯(0-8) 클릭 -> 아이템 제거
            else if (isSlotInArray(slot, ITEM_SLOTS)) {
                ItemStack clickedItem = inventory.getItem(slot);
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    inventory.setItem(slot, null); // 아이템 제거
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
                }
            }
            // GUI 하단 클릭 (플레이어 인벤토리)
        } else if (clickedInventory.getHolder() == player) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                addItemToRewardSlots(clickedItem); // 아이템 추가 로직 호출
            }
        }
    }

    // 플레이어 인벤토리에서 클릭한 아이템을 GUI 상단 빈 슬롯에 추가
    private void addItemToRewardSlots(ItemStack newItem) {
        for (int slot : ITEM_SLOTS) {
            ItemStack currentItem = inventory.getItem(slot);
            if (currentItem == null || currentItem.getType().isAir()) {
                ItemStack toAdd = newItem.clone();
                toAdd.setAmount(1); // 수량 1로 고정 (선택사항)
                inventory.setItem(slot, toAdd);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);
                return; // 하나만 추가하고 종료
            }
        }
        player.sendMessage("§c보상 아이템 슬롯이 가득 찼습니다. (최대 " + ITEM_SLOTS.length + "개)");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    // 수정: 현재 GUI 상단(0-8) 슬롯의 아이템들을 CodexItem에 반영하고 DB에 즉시 저장
    private void saveRewardItemsAndClose() {
        List<ItemStack> newRewardItems = new ArrayList<>();
        for (int itemSlot : ITEM_SLOTS) {
            ItemStack item = inventory.getItem(itemSlot);
            if (item != null && !item.getType().isAir()) {
                newRewardItems.add(item.clone());
            }
        }
        // CodexItem 객체 업데이트
        codexItem.getReward().setRewardItems(newRewardItems);

        // 데이터베이스에 즉시 저장
        codexRepository.saveOrUpdateItem(codexItem).thenRun(() -> {
            player.sendMessage(getMessage("admin_item_reward_set"));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

            // DB 저장 성공 후 부모 GUI 새로고침 및 열기 (다음 틱)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (parentGui instanceof RewardSettingGUI rsg) {
                    rsg.refresh();
                }
                plugin.getGuiManager().openGui(parentGui);
            });
        }).exceptionally(ex -> {
            // 저장 실패 시 오류 메시지 표시 및 GUI 다시 열기 (다음 틱)
            plugin.getLogger().severe("아이템 보상 저장 중 오류 발생: " + ex.getMessage());
            player.sendMessage("§c아이템 보상 저장 중 오류가 발생했습니다.");
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(this)); // 현재 GUI 다시 열기
            return null;
        });
    }

    private boolean isSlotInArray(int slot, int[] array) {
        for (int i : array) {
            if (i == slot) return true;
        }
        return false;
    }
}

