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
import kr.jjory.jcodex.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private static final int CATEGORY_PROGRESS_SLOT = 47;
    private static final int GLOBAL_PROGRESS_SLOT = 48;

    private List<CodexItem> itemsToShow;
    private PlayerCodexProgress playerProgress;
    private int currentPage = 0;
    private int totalPages = 0;
    private final int itemsPerPage = ITEM_SLOTS.length;

    private CodexCategory currentFilter = CodexCategory.FARMER;

    public CodexMainGUI(Player player, JCodexPlugin plugin) {
        super(player, 54, "§8📘 도감 시스템");
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
                boolean isRegistered = playerProgress != null && playerProgress.getRegisteredItemIds().contains(codexItem.getItemId());

                ItemStack displayItem = ItemUtil.createItemFromCodexItem(codexItem);
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = new ArrayList<>();

                if (isRegistered) {
                    meta.setDisplayName("§a✅ " + codexItem.getDisplayName());
                    lore.add("§a이미 등록된 아이템입니다.");
                } else {
                    meta.setDisplayName("§c❓ " + codexItem.getDisplayName());
                    lore.add("§c미등록 아이템입니다.");
                    lore.add("§7클릭하여 인벤토리의 아이템을 등록하세요.");
                }

                lore.add(" ");
                lore.add("§6[해금 보상]");
                RewardSpec reward = codexItem.getReward();
                if(reward.getMoney() > 0) lore.add("§7- 돈: §f" + reward.getMoney());

                MainConfig mainConfig = plugin.getConfigManager().getMainConfig();
                reward.getStats().forEach((statKey, value) -> {
                    MainConfig.StatDefinition def = mainConfig.getStatDefinition(statKey);
                    lore.add("§7- 스탯: §f" + def.getDisplay() + " + " + value);
                });

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                inventory.setItem(slot, displayItem);

            } else {
                inventory.setItem(slot, null);
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
                List<String> lore = new ArrayList<>();
                lore.add("§7클릭하여 §a" + category.getDisplayName() + "§7 카테고리만 봅니다.");
                if (currentFilter == category) {
                    meta.setDisplayName("§a§l[ " + category.getDisplayName() + " ]");
                }
                meta.setLore(lore);
                categoryItem.setItemMeta(meta);
                for (int slot : CATEGORY_SLOTS[i]) {
                    inventory.setItem(slot, categoryItem);
                }
            }
        }

        progressService.calculateProgress(player.getUniqueId(), currentFilter).thenAccept(progressMap -> {
            ItemStack categoryProgressItem = ItemUtil.createItem(Material.EXPERIENCE_BOTTLE,
                    "§a카테고리 달성도",
                    List.of("§7" + (currentFilter != null ? currentFilter.getDisplayName() : "전체") + ": " + String.format("%.1f%%", progressMap.getOrDefault("categoryProgress", 0.0)))
            );
            inventory.setItem(CATEGORY_PROGRESS_SLOT, categoryProgressItem);

            ItemStack globalProgressItem = ItemUtil.createItem(Material.NETHER_STAR,
                    "§b전체 달성도",
                    List.of("§7진행도: " + String.format("%.1f%%", progressMap.getOrDefault("globalProgress", 0.0)))
            );
            inventory.setItem(GLOBAL_PROGRESS_SLOT, globalProgressItem);
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
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
                if (itemIndex < itemsToShow.size()) {
                    CodexItem clickedCodexItem = itemsToShow.get(itemIndex);
                    boolean isRegistered = playerProgress != null && playerProgress.getRegisteredItemIds().contains(clickedCodexItem.getItemId());
                    if (!isRegistered) {
                        plugin.getCodexService().registerItem(player, clickedCodexItem);
                    } else {
                        player.sendMessage("§e[JCodex] 이미 등록한 아이템입니다.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
                return;
            }
        }
    }

    private boolean isSlotInArray(int slot, int[] array) {
        for (int i : array) {
            if (i == slot) return true;
        }
        return false;
    }
}

