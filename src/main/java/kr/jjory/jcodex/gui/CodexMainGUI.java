package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.config.MainConfig;
import kr.jjory.jcodex.model.CodexCategory;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.model.PlayerCodexProgress;
import kr.jjory.jcodex.model.RewardSpec;
import kr.jjory.jcodex.repository.CodexRepository;
import kr.jjory.jcodex.repository.PlayerProgressRepository;
import kr.jjory.jcodex.service.ProgressService;
import kr.jjory.jcodex.util.ItemUtil; // ItemUtil은 아이템 생성 외 로직에 필요
import kr.jjory.jcodex.util.Lang;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일반 유저를 위한 메인 도감 GUI 클래스입니다.
 */
public class CodexMainGUI extends Gui {

    private final JCodexPlugin plugin;
    private final CodexRepository codexRepository;
    private final PlayerProgressRepository playerProgressRepository;
    private final ProgressService progressService;

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
    private static final int CATEGORY_PROGRESS_SLOT = 47;
    private static final int GLOBAL_PROGRESS_SLOT = 48;

    private List<CodexItem> itemsToShow;
    private PlayerCodexProgress playerProgress;
    private int currentPage = 0;
    private int totalPages = 0;
    private final int itemsPerPage = ITEM_SLOTS.length;

    private CodexCategory currentFilter = CodexCategory.FARMER;

    public CodexMainGUI(Player player, JCodexPlugin plugin) {
        // 수정: Gui 생성자 변경에 따라 guiConfig 전달
        super(player, 54, "main", plugin.getConfigManager().getGuiConfig());
        this.plugin = plugin;
        this.codexRepository = new CodexRepository(plugin);
        this.playerProgressRepository = new PlayerProgressRepository(plugin);
        this.progressService = plugin.getProgressService();
        loadDataAndDraw();
    }

    public void refresh() {
        loadDataAndDraw();
    }

    private void loadDataAndDraw() {
        codexRepository.findAllAsync()
                .thenCombine(playerProgressRepository.findProgressByUUID(player.getUniqueId()), (allItems, progress) -> {
                    this.playerProgress = progress;
                    this.itemsToShow = allItems.stream()
                            .filter(item -> currentFilter == null || item.getCategory() == currentFilter)
                            .collect(Collectors.toList());
                    this.totalPages = (int) Math.ceil((double) this.itemsToShow.size() / itemsPerPage);
                    return null;
                }).thenRun(() -> {
                    plugin.getServer().getScheduler().runTask(plugin, this::draw);
                });
    }

    private void draw() {
        inventory.clear(); // 이전 아이템 제거
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
                boolean isRegistered = playerProgress != null && playerProgress.getRegisteredItemIds().contains(codexItem.getItemId());

                ItemStack displayItem = ItemUtil.createItemFromCodexItem(codexItem);
                ItemMeta meta = displayItem.getItemMeta();
                String basePath = isRegistered ? "items.codex_item_registered" : "items.codex_item_unregistered";

                String itemKey = ItemUtil.getItemKey(displayItem);
                String translatedName = Lang.translate(player, itemKey);
                String finalItemName = (translatedName != null) ? translatedName : codexItem.getDisplayName();

                String name = guiConfig.getString(basePath + ".name", "%item_name%")
                        .replace("%item_name%", finalItemName);
                List<String> lore = new ArrayList<>(guiConfig.getStringList(basePath + ".lore"));

                RewardSpec reward = codexItem.getReward();
                if(reward.getMoney() > 0) lore.add(colorize("&7- 돈: &f" + formatNumber("money", reward.getMoney())));

                MainConfig mainConfig = plugin.getConfigManager().getMainConfig();
                reward.getStats().forEach((statKey, value) -> {
                    MainConfig.StatDefinition def = mainConfig.getStatDefinition(statKey);
                    lore.add(colorize("&7- 스탯: &f" + def.getDisplay() + " + " + value));
                });

                if (reward.getRewardItems() != null && !reward.getRewardItems().isEmpty()) {
                    ItemStack firstRewardItem = reward.getRewardItems().get(0);
                    String rewardItemKey = ItemUtil.getItemKey(firstRewardItem);
                    String translatedRewardName = Lang.translate(player, rewardItemKey);
                    String finalRewardName = (translatedRewardName != null) ? translatedRewardName : firstRewardItem.getType().name().toLowerCase();
                    String itemRewardLine = "&7- 아이템: &f" + finalRewardName;
                    if (reward.getRewardItems().size() > 1) {
                        itemRewardLine += " 외 " + (reward.getRewardItems().size() - 1) + "개";
                    }
                    lore.add(colorize(itemRewardLine));
                }

                meta.setDisplayName(colorize(name));
                meta.setLore(lore.stream().map(this::colorize).collect(Collectors.toList()));
                displayItem.setItemMeta(meta);
                inventory.setItem(slot, displayItem);

            } else {
                inventory.setItem(slot, null);
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
                String path = (currentFilter == category) ? "items.category_button_selected" : "items.category_button";
                ItemStack categoryItem = createCategoryItem(path, category);

                for (int slot : CATEGORY_SLOTS[i]) {
                    inventory.setItem(slot, categoryItem);
                }
            }
        }

        progressService.calculateProgress(player.getUniqueId(), currentFilter).thenAccept(progressMap -> {
            double catProgress = progressMap.getOrDefault("categoryProgress", 0.0);
            double globalProgress = progressMap.getOrDefault("globalProgress", 0.0);
            String catName = (currentFilter != null ? currentFilter.getDisplayName() : "전체");

            String formattedCatProgress = formatNumber("percentage", catProgress) + "%";
            String formattedGlobalProgress = formatNumber("percentage", globalProgress) + "%";
            ItemStack categoryProgressItem = createGuiItem("items.category_progress",
                    "%category_name%", catName,
                    "%progress%", formattedCatProgress,
                    "%category_progress%", formattedCatProgress,
                    "%global_progress%", formattedGlobalProgress);
            inventory.setItem(CATEGORY_PROGRESS_SLOT, categoryProgressItem);

            ItemStack globalProgressItem = createGuiItem("items.global_progress",
                    "%progress%", formattedGlobalProgress,
                    "%category_progress%", formattedCatProgress,
                    "%global_progress%", formattedGlobalProgress,
                    "%category_name%", catName);
            inventory.setItem(GLOBAL_PROGRESS_SLOT, globalProgressItem);
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // 수정: 리스너에서 이미 취소했으므로 여기서는 취소 X
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        // GUI 상단 클릭
        if (clickedInventory.getHolder() == this) {
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
                    if (itemIndex < itemsToShow.size()) {
                        CodexItem clickedCodexItem = itemsToShow.get(itemIndex);
                        boolean isRegistered = playerProgress != null && playerProgress.getRegisteredItemIds().contains(clickedCodexItem.getItemId());
                        if (!isRegistered) {
                            plugin.getCodexService().registerItem(player, clickedCodexItem);
                        } else {
                            player.sendMessage(getMessage("register_already"));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        }
                    }
                    return;
                }
            }
        }
        // 하단 인벤토리 클릭은 무시
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

