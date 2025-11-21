package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.model.RewardSpec;
import kr.jjory.jcodex.repository.CodexRepository;
import kr.jjory.jcodex.repository.PlayerProgressRepository;
import kr.jjory.jcodex.service.PlayerStatService;
import kr.jjory.jcodex.util.ItemUtil;
import kr.jjory.jcodex.util.Lang; // Lang 클래스 임포트
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 도감 설정 GUI 클래스입니다.
 */
public class CodexAdminGUI extends Gui {

    private final JCodexPlugin plugin;
    private final CodexRepository codexRepository;
    private final PlayerProgressRepository progressRepository;
    private final PlayerStatService playerStatService;
    private final Gui parentGui;

    // 기획서 슬롯 레이아웃
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
        super(player, 54, "admin", plugin.getConfigManager().getGuiConfig());
        this.plugin = plugin;
        this.codexRepository = new CodexRepository(plugin);
        this.progressRepository = new PlayerProgressRepository(plugin);
        this.playerStatService = plugin.getPlayerStatService();
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
        ItemStack background = createGuiItem("items.background");
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

                // --- 아이템 이름만 번역 ---
                String itemKey = ItemUtil.getItemKey(displayItem);
                String translatedName = Lang.translate(player, itemKey);
                String finalItemName = (translatedName != null) ? translatedName : codexItem.getDisplayName();
                // --- --- ---

                // gui.yml에서 아이템 정보 가져오기 (번역된 이름 사용)
                ItemStack guiInfoItem = createGuiItem("items.admin_codex_item",
                        "%item_name%", finalItemName, // 번역된 이름 사용
                        "%item_id%", codexItem.getItemId(),
                        "%category_name%", codexItem.getCategory().getDisplayName());

                ItemMeta displayMeta = displayItem.getItemMeta();
                ItemMeta guiMeta = guiInfoItem.getItemMeta();

                // 실제 아이템에 gui.yml의 이름과 설명 적용
                if (displayMeta != null && guiMeta != null) {
                    displayMeta.setDisplayName(guiMeta.getDisplayName());
                    displayMeta.setLore(guiMeta.getLore());
                    displayItem.setItemMeta(displayMeta);
                }
                inventory.setItem(slot, displayItem);

            } else {
                ItemStack addItem = createGuiItem("items.admin_add_item");
                inventory.setItem(slot, addItem);
            }
        }
    }

    private void drawControls() {
        ItemStack prevPage = createGuiItem("items.previous_page");
        for (int slot : PREVIOUS_PAGE_SLOTS) {
            inventory.setItem(slot, prevPage);
        }
        ItemStack nextPage = createGuiItem("items.next_page");
        for (int slot : NEXT_PAGE_SLOTS) {
            inventory.setItem(slot, nextPage);
        }

        CodexCategory[] categories = CodexCategory.values();
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (i < categories.length) {
                CodexCategory category = categories[i];
                // 카테고리 이름은 번역하지 않고 Enum의 displayName 사용
                String path = (currentFilter == category) ? "items.category_button_selected" : "items.category_button";
                ItemStack categoryItem = createCategoryItem(path, category);

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
            event.setCancelled(true); // 상단 클릭 시 기본 동작 방지
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
            player.sendMessage(getMessage("admin_register_id_detect_fail"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        CodexCategory categoryToRegister = (currentFilter != null) ? currentFilter : CodexCategory.MISC;
        String originalDisplayName = itemToRegister.hasItemMeta() && itemToRegister.getItemMeta().hasDisplayName()
                ? itemToRegister.getItemMeta().getDisplayName()
                : itemToRegister.getType().name();

        CodexItem newItem = new CodexItem(itemId, originalDisplayName, categoryToRegister,
                new RewardSpec(null, 0, new HashMap<>(), null)); // 보상 리스트 초기화

        codexRepository.saveOrUpdateItem(newItem).thenRun(() -> {
            // 메시지에는 번역된 이름 사용
            String itemKey = ItemUtil.getItemKey(itemToRegister);
            String translatedName = Lang.translate(player, itemKey);
            String finalItemName = (translatedName != null) ? translatedName : originalDisplayName;

            player.sendMessage(getMessage("admin_register_success",
                    "%item_name%", finalItemName,
                    "%item_id%", itemId,
                    "%category_name%", categoryToRegister.getDisplayName()));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            plugin.getSyncService().publishItemUpdate(newItem);
            plugin.getServer().getScheduler().runTask(plugin, this::refresh);
        }).exceptionally(ex -> {
            player.sendMessage(getMessage("admin_register_error", "%error%", ex.getMessage()));
            return null;
        });
    }

    private void handleItemDeletion(CodexItem itemToDelete) {
        player.closeInventory();

        // 삭제 확인 메시지에 번역된 이름 사용
        ItemStack tempItem = ItemUtil.createItemFromCodexItem(itemToDelete);
        String itemKey = ItemUtil.getItemKey(tempItem);
        String translatedName = Lang.translate(player, itemKey);
        String finalItemName = (translatedName != null) ? translatedName : itemToDelete.getDisplayName();

        player.sendMessage(getMessage("admin_delete_confirm", "%item_name%", finalItemName));
        player.sendMessage(getMessage("admin_delete_prompt"));

        plugin.getGuiManager().promptChatInput(player, confirmation -> {
            if (confirmation.equalsIgnoreCase("삭제")) {
                String itemId = itemToDelete.getItemId();
                progressRepository.findPlayersByItemId(itemId).thenCompose(affectedPlayers ->
                        codexRepository.deleteItem(itemId)
                                .thenCompose(v -> progressRepository.deleteRegistrationsByItem(itemId))
                                .thenCompose(v -> playerStatService.removeStatsForDeletedItem(itemToDelete.getReward().getStats(), affectedPlayers))
                                .thenApply(v -> affectedPlayers)
                ).thenAccept(affectedPlayers -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(getMessage("admin_delete_success"));
                        if (!affectedPlayers.isEmpty()) {
                            player.sendMessage("§e[JCodex] " + affectedPlayers.size() + "명의 플레이어에게 적용된 스탯을 회수했습니다.");
                        }
                        plugin.getSyncService().publishItemDelete(itemId);
                        loadDataAndDraw();
                        plugin.getGuiManager().openGui(this);
                    });
                });
            } else {
                player.sendMessage(getMessage("admin_delete_cancelled"));
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(this));
            }
        });
    }

    private boolean isSlotInArray(int slot, int[] array) {
        for (int i : array) {
            if (i == slot) return true;
        }
        return false;
    }

    private ItemStack createCategoryItem(String path, CodexCategory category) {
        ItemStack categoryItem = createGuiItem(path, "%category_name%", category.getDisplayName());

        Material configuredMaterial = resolveCategoryMaterial(path, category);
        if (configuredMaterial != null) {
            categoryItem.setType(configuredMaterial);
        } else if (!guiConfig.contains(path + ".material")) {
            categoryItem.setType(category.getIcon());
        }

        ItemMeta meta = categoryItem.getItemMeta();
        if (meta != null) {
            categoryItem.setItemMeta(meta);
        }

        return categoryItem;
    }

    private Material resolveCategoryMaterial(String path, CodexCategory category) {
        Material direct = loadMaterialFromConfig(path + ".materials." + category.name());
        if (direct != null) {
            return direct;
        }

        if (!"items.category_button".equals(path)) {
            Material fallback = loadMaterialFromConfig("items.category_button.materials." + category.name());
            if (fallback != null) {
                return fallback;
            }
        }

        return null;
    }
}

