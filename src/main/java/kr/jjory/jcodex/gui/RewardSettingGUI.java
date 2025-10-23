package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.config.MainConfig;
import kr.jjory.jcodex.model.CodexItem;
import kr.jjory.jcodex.repository.CodexRepository;
import kr.jjory.jcodex.util.ItemUtil; // ItemUtil은 이제 필요 없음
import kr.jjory.jcodex.util.Lang;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 도감 아이템의 해금 보상을 설정하는 GUI 클래스입니다.
 */
public class RewardSettingGUI extends Gui {

    private final JCodexPlugin plugin;
    private final CodexRepository codexRepository;
    private final CodexItem codexItem;
    private final Gui parentGui;

    // 기획서 슬롯 레이아웃
    private static final int ITEM_REWARD_SLOT = 0;
    private static final int MONEY_REWARD_SLOT = 1;
    private static final int[] STAT_REWARD_SLOTS = {2, 3, 4, 5, 6, 7, 8};
    private static final int[] BACK_BUTTON_SLOTS = {9, 10, 11};
    private static final int[] SAVE_BUTTON_SLOTS = {15, 16, 17};


    public RewardSettingGUI(Player player, JCodexPlugin plugin, CodexItem codexItem, Gui parentGui) {
        super(player, 18, "reward_setting", plugin.getConfigManager().getGuiConfig());
        this.plugin = plugin;
        this.codexRepository = new CodexRepository(plugin);
        this.codexItem = codexItem;
        this.parentGui = parentGui;
        draw();
    }

    public void refresh() {
        draw();
    }

    private void draw() {
        inventory.clear();
        drawBackground();
        drawRewardSettings();
        drawControls();
    }

    private void drawBackground() {
        ItemStack background = createGuiItem("items.background");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, background);
        }
    }

    private void drawRewardSettings() {
        // 아이템 보상 (슬롯 0)
        List<ItemStack> currentItemRewards = codexItem.getReward().getRewardItems();
        if (currentItemRewards != null && !currentItemRewards.isEmpty()) {
            ItemStack display = currentItemRewards.get(0).clone();
            ItemMeta meta = display.getItemMeta();
            String itemKey = ItemUtil.getItemKey(display);
            String translatedName = Lang.translate(player, itemKey);
            if (translatedName != null && meta != null) {
                meta.setDisplayName(colorize(translatedName)); // 번역된 이름 설정
            }
            List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            List<String> suffix = guiConfig.getStringList("items.reward_item_set.lore_suffix");

            final int count = currentItemRewards.size();
            suffix = suffix.stream()
                    .filter(line -> count > 1 || !line.contains("%count%"))
                    .map(line -> line.replace("%count%", String.valueOf(count)))
                    .map(this::colorize)
                    .collect(Collectors.toList());
            lore.addAll(suffix);

            if (meta != null) {
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(ITEM_REWARD_SLOT, display);
        } else {
            ItemStack placeholder = createGuiItem("items.reward_item_placeholder");
            inventory.setItem(ITEM_REWARD_SLOT, placeholder);
        }

        // 돈 보상 (슬롯 1)
        ItemStack moneyItem = createGuiItem("items.reward_money",
                "%value%", formatNumber("money", codexItem.getReward().getMoney()));
        inventory.setItem(MONEY_REWARD_SLOT, moneyItem);

        // 스탯 보상 (슬롯 2-8, 오른쪽 정렬)
        MainConfig mainConfig = plugin.getConfigManager().getMainConfig();
        List<String> statsOrder = mainConfig.getStatsOrder();
        Map<String, Double> currentStats = codexItem.getReward().getStats();

        int startSlot = STAT_REWARD_SLOTS[0] + (STAT_REWARD_SLOTS.length - statsOrder.size());

        for (int i = 0; i < statsOrder.size(); i++) {
            String statKey = statsOrder.get(i);
            MainConfig.StatDefinition statDef = mainConfig.getStatDefinition(statKey);
            double value = currentStats.getOrDefault(statKey, 0.0);
            boolean isEnabled = value != 0;

            // 스탯 이름은 번역하지 않음 (config.yml의 display 사용)
            String statusDisplay = isEnabled ? getMessage("reward_stat_status_enabled") : getMessage("reward_stat_status_disabled");

            ItemStack statItem = createGuiItem("items.reward_stat",
                    "%stat_name%", statDef.getDisplay(),
                    "%status_display%", statusDisplay,
                    "%value%", String.valueOf(value));

            if (!guiConfig.contains("items.reward_stat.material")) {
                statItem.setType(isEnabled ? Material.LIME_DYE : Material.RED_DYE);
            }

            inventory.setItem(startSlot + i, statItem);
        }
    }

    private void drawControls() {
        ItemStack back = createGuiItem("items.back_button");
        for(int slot : BACK_BUTTON_SLOTS) inventory.setItem(slot, back);

        ItemStack save = createGuiItem("items.save_button");
        for(int slot : SAVE_BUTTON_SLOTS) inventory.setItem(slot, save);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // 수정: 리스너에서 이미 취소했으므로 여기서는 취소 X
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || clickedInventory.getHolder() != this) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == ITEM_REWARD_SLOT) {
            plugin.getGuiManager().openGui(new ItemRewardSettingGUI(player, plugin, codexItem, this));
        } else if (slot == MONEY_REWARD_SLOT) {
            handleMoneyRewardClick(event);
        } else if (isSlotInArray(slot, BACK_BUTTON_SLOTS)) {
            plugin.getGuiManager().openGui(parentGui);
        } else if (isSlotInArray(slot, SAVE_BUTTON_SLOTS)) {
            saveAndClose();
        } else {
            handleStatRewardClick(event);
        }
    }

    private void handleMoneyRewardClick(InventoryClickEvent event) {
        // 수정: 좌클릭으로 금액 설정
        if (event.getClick() == ClickType.LEFT) {
            promptForMoney();
        } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
            codexItem.getReward().setMoney(0);
            draw();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.5f);
        }
    }

    private void handleStatRewardClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        MainConfig mainConfig = plugin.getConfigManager().getMainConfig();
        List<String> statsOrder = mainConfig.getStatsOrder();
        int baseSlot = STAT_REWARD_SLOTS[0];
        int statCount = statsOrder.size();
        int startSlot = baseSlot + (STAT_REWARD_SLOTS.length - statCount);

        if (slot >= startSlot && slot < startSlot + statCount) {
            String statKey = statsOrder.get(slot - startSlot);
            double currentValue = codexItem.getReward().getStats().getOrDefault(statKey, 0.0);
            ClickType click = event.getClick();

            if (click == ClickType.LEFT) { // 활성/비활성 토글
                if (currentValue != 0) {
                    codexItem.getReward().getStats().remove(statKey);
                } else {
                    codexItem.getReward().getStats().put(statKey, 1.0); // 기본값 1.0으로 활성화
                }
                draw();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
            } else if (click == ClickType.RIGHT) { // 값 설정
                promptForStat(statKey);
            } else if (click == ClickType.SHIFT_RIGHT) { // 초기화
                codexItem.getReward().getStats().remove(statKey);
                draw();
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.5f);
            }
        }
    }


    private void promptForMoney() {
        player.closeInventory();
        player.sendMessage(getMessage("admin_reward_prompt_money"));
        plugin.getGuiManager().promptChatInput(player, input -> {
            try {
                double amount = Double.parseDouble(input);
                codexItem.getReward().setMoney(amount);
                player.sendMessage(getMessage("admin_reward_set_money", "%amount%", formatNumber("money", amount)));
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage("admin_reward_invalid_number"));
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(this));
        });
    }

    private void promptForStat(String statKey) {
        player.closeInventory();
        MainConfig.StatDefinition statDef = plugin.getConfigManager().getMainConfig().getStatDefinition(statKey);
        player.sendMessage(getMessage("admin_reward_prompt_stat", "%stat_name%", statDef.getDisplay()));
        plugin.getGuiManager().promptChatInput(player, input -> {
            try {
                double value = Double.parseDouble(input);
                codexItem.getReward().getStats().put(statKey, value);
                player.sendMessage(getMessage("admin_reward_set_stat", "%stat_name%", statDef.getDisplay(), "%value%", String.valueOf(value)));
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage("admin_reward_invalid_number"));
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(this));
        });
    }

    private void saveAndClose() {
        codexRepository.saveOrUpdateItem(codexItem).thenRun(() -> {
            player.sendMessage(getMessage("admin_reward_saved"));
            plugin.getSyncService().publishItemUpdate(codexItem);
            if (parentGui instanceof CodexAdminGUI adminGui) {
                plugin.getServer().getScheduler().runTask(plugin, adminGui::refresh);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(parentGui));
        });
    }

    private boolean isSlotInArray(int slot, int[] array) {
        for (int i : array) {
            if (i == slot) return true;
        }
        return false;
    }
}

