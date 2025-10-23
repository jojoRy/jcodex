package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.model.RewardSpec;
import kr.jjory.jcodex.repository.CodexRepository;
import kr.jjory.jcodex.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 도감 설정 GUI 클래스입니다.
 */
public class CodexAdminGUI extends Gui {

    private final JCodexPlugin plugin;
    private final CodexRepository codexRepository;
    private final Gui parentGui;

    // 기획서에 명시된 슬롯 레이아웃
    private static final int[] ITEM_SLOTS = {
            0, 1, 2, 3, 4, 5,
            9, 10, 11, 12, 13, 14,
            18, 19, 20, 21, 22, 23,
            27, 28, 29, 30, 31, 32,
            36, 37, 38, 39, 40, 41
    };
    private static final int[][] CATEGORY_SLOTS = {
            {7, 8}, {16, 17}, {25, 26}, {34, 35}, {43, 44}, {52, 53}
    };
    private static final int[] PREVIOUS_PAGE_SLOTS = {45, 46};
    private static final int[] NEXT_PAGE_SLOTS = {49, 50};

    private List<CodexItem> itemsToShow;
    private int currentPage = 0;
    private int totalPages = 0;
    private final int itemsPerPage = ITEM_SLOTS.length;

    private CodexCategory currentFilter = CodexCategory.FARMER;

    public CodexAdminGUI(Player player, JCodexPlugin plugin, Gui parentGui) {
        super(player, 54, "§e도감 설정");
        this.plugin = plugin;
        this.codexRepository = new CodexRepository(plugin);
        this.parentGui = parentGui;
        loadDataAndDraw();
    }

    public void refresh() {
        loadDataAndDraw();
    }

    private void loadDataAndDraw() {
        codexRepository.findAllAsync().thenAccept(allItems -> {
            this.itemsToShow = allItems.stream()
                    .filter(item -> currentFilter == null || item.getCategory() == currentFilter)
                    .collect(Collectors.toList());
            this.totalPages = (int) Math.ceil((double) this.itemsToShow.size() / itemsPerPage);
            plugin.getServer().getScheduler().runTask(plugin, this::draw);
        });
    }

    private void draw() {
        inventory.clear();
        drawBackground();
        drawItems();
        drawControls();
    }

    private void drawBackground() {
        ItemStack background = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, background);
        }
    }

    private void drawItems() {
        int startIndex = currentPage * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int itemIndex = startIndex + i;
            int slot = ITEM_SLOTS[i];

            if (itemIndex < itemsToShow.size()) {
                CodexItem codexItem = itemsToShow.get(itemIndex);
                ItemStack displayItem = ItemUtil.createItemFromCodexItem(codexItem);
                ItemMeta meta = displayItem.getItemMeta();
                meta.setDisplayName("§e" + codexItem.getDisplayName());

                List<String> lore = new ArrayList<>();
                lore.add("§7아이템 ID: " + codexItem.getItemId());
                lore.add("§7카테고리: " + codexItem.getCategory().getDisplayName());
                lore.add(" ");
                lore.add("§a좌클릭: §f보상 설정");
                lore.add("§cShift + 우클릭: §f아이템 삭제");

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                inventory.setItem(slot, displayItem);

            } else {
                ItemStack addItem = ItemUtil.createItem(Material.LIME_STAINED_GLASS_PANE, "§a아이템 신규 등록");
                ItemMeta meta = addItem.getItemMeta();
                meta.setLore(List.of("§7아래 인벤토리에서 아이템을 클릭하여", "§7이 카테고리에 신규 등록하세요."));
                addItem.setItemMeta(meta);
                inventory.setItem(slot, addItem);
            }
        }
    }

    private void drawControls() {
        ItemStack prevPage = ItemUtil.createItem(Material.ARROW, "§e이전 페이지");
        for (int slot : PREVIOUS_PAGE_SLOTS) {
            inventory.setItem(slot, prevPage);
        }
        ItemStack nextPage = ItemUtil.createItem(Material.ARROW, "§e다음 페이지");
        for (int slot : NEXT_PAGE_SLOTS) {
            inventory.setItem(slot, nextPage);
        }

        CodexCategory[] categories = CodexCategory.values();
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (i < categories.length) {
                CodexCategory category = categories[i];
                ItemStack categoryItem = ItemUtil.createItem(category.getIcon(), "§b" + category.getDisplayName());
                ItemMeta meta = categoryItem.getItemMeta();
                if (currentFilter == category) {
                    meta.setDisplayName("§a§l[ " + category.getDisplayName() + " ]");
                }
                categoryItem.setItemMeta(meta);
                for (int slot : CATEGORY_SLOTS[i]) {
                    inventory.setItem(slot, categoryItem);
                }
            }
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        // GUI 상단 클릭 (도감 GUI)
        if (clickedInventory.getHolder() == this) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (isSlotInArray(slot, PREVIOUS_PAGE_SLOTS)) {
                if (currentPage > 0) {
                    currentPage--;
                    draw();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                }
            } else if (isSlotInArray(slot, NEXT_PAGE_SLOTS)) {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    draw();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                }
            }

            for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
                if (isSlotInArray(slot, CATEGORY_SLOTS[i]) && i < CodexCategory.values().length) {
                    currentFilter = CodexCategory.values()[i];
                    currentPage = 0;
                    loadDataAndDraw();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    return;
                }
            }

            for (int i = 0; i < ITEM_SLOTS.length; i++) {
                if (slot == ITEM_SLOTS[i]) {
                    int itemIndex = (currentPage * itemsPerPage) + i;
                    if (itemIndex < itemsToShow.size()) { // 기존 아이템 클릭
                        CodexItem selectedCodexItem = itemsToShow.get(itemIndex);
                        if (event.getClick() == ClickType.LEFT) {
                            plugin.getGuiManager().openGui(new RewardSettingGUI(player, plugin, selectedCodexItem, this));
                        } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                            handleItemDeletion(selectedCodexItem);
                        }
                    }
                    // 빈 슬롯 클릭은 아무 동작 없음 (하단 인벤토리 클릭으로 처리)
                    return;
                }
            }
            // GUI 하단 클릭 (플레이어 인벤토리)
        } else if (clickedInventory.getHolder() == player) {
            event.setCancelled(true); // 하단 인벤토리 클릭도 기본 동작 방지
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                handleItemRegistration(clickedItem);
            }
        }
    }

    private void handleItemRegistration(ItemStack itemToRegister) {
        String itemId = ItemUtil.getItemId(itemToRegister);
        if (itemId == null) {
            player.sendMessage("§c[JCodex] 이 아이템의 ID를 자동으로 감지할 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        CodexCategory categoryToRegister = (currentFilter != null) ? currentFilter : CodexCategory.MISC;
        String displayName = itemToRegister.hasItemMeta() && itemToRegister.getItemMeta().hasDisplayName()
                ? itemToRegister.getItemMeta().getDisplayName()
                : itemToRegister.getType().name();

        CodexItem newItem = new CodexItem(itemId, displayName, categoryToRegister,
                new RewardSpec(null, 0, new HashMap<>(), null));

        codexRepository.saveOrUpdateItem(newItem).thenRun(() -> {
            player.sendMessage("§a아이템 '§f" + displayName + "§a' (§7" + itemId + "§a)이(가) "
                    + "§e" + categoryToRegister.getDisplayName() + "§a 카테고리에 신규 등록되었습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            plugin.getSyncService().publishItemUpdate(newItem);
            plugin.getServer().getScheduler().runTask(plugin, this::refresh); // 등록 후 GUI 새로고침
        }).exceptionally(ex -> {
            player.sendMessage("§c아이템 등록 중 오류가 발생했습니다: " + ex.getMessage());
            return null;
        });
    }

    private void handleItemDeletion(CodexItem itemToDelete) {
        player.closeInventory();
        player.sendMessage("§c정말로 '§f" + itemToDelete.getDisplayName() + "§c' 아이템을 도감에서 삭제하시겠습니까?");
        player.sendMessage("§7(삭제하려면 채팅으로 '삭제'라고 입력하세요)");

        plugin.getGuiManager().promptChatInput(player, confirmation -> {
            if (confirmation.equalsIgnoreCase("삭제")) {
                codexRepository.deleteItem(itemToDelete.getItemId()).thenRun(() -> {
                    player.sendMessage("§a아이템이 성공적으로 삭제되었습니다.");
                    plugin.getSyncService().publishItemDelete(itemToDelete.getItemId());
                    // 수정: 삭제 성공 후 GUI를 새로고침하고 다시 엽니다.
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        loadDataAndDraw(); // 데이터 새로고침
                        plugin.getGuiManager().openGui(this); // GUI 다시 열기
                    });
                });
            } else {
                player.sendMessage("§e삭제가 취소되었습니다.");
                // 수정: 삭제 취소 시에도 GUI를 다시 엽니다.
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(this));
            }
            // 수정: 콜백 외부의 GUI 열기 코드는 제거합니다.
        });
    }

    private boolean isSlotInArray(int slot, int[] array) {
        for (int i : array) {
            if (i == slot) return true;
        }
        return false;
    }
}

