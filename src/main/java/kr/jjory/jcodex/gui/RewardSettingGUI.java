package kr.jjory.jcodex.gui;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.config.MainConfig;
import kr.jjory.jcodex.model.CodexItem;
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
import java.util.List;
import java.util.Map;

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
        super(player, 27, "§6해금 보상 설정");
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
        ItemStack background = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
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
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(" ");
            if (currentItemRewards.size() > 1) {
                lore.add("§7(총 " + currentItemRewards.size() + "개의 아이템 보상)");
            }
            lore.add("§a클릭하여 보상 아이템 변경");
            meta.setLore(lore);
            display.setItemMeta(meta);
            inventory.setItem(ITEM_REWARD_SLOT, display);
        } else {
            ItemStack placeholder = ItemUtil.createItem(Material.BARRIER, "§c아이템 보상 없음", List.of("§a클릭하여 보상 아이템 설정"));
            inventory.setItem(ITEM_REWARD_SLOT, placeholder);
        }

        // 돈 보상 (슬롯 1)
        ItemStack moneyItem = ItemUtil.createItem(Material.GOLD_NUGGET, "§6돈 보상 설정");
        ItemMeta moneyMeta = moneyItem.getItemMeta();
        moneyMeta.setLore(List.of("§7현재 값: §e" + codexItem.getReward().getMoney(), "§a좌클릭: §f금액 설정", "§cShift+우클릭: §f초기화"));
        moneyItem.setItemMeta(moneyMeta);
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

            ItemStack statItem = ItemUtil.createItem(isEnabled ? Material.LIME_DYE : Material.RED_DYE, "§b" + statDef.getDisplay() + " 보상");
            ItemMeta statMeta = statItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(isEnabled ? "§a상태: 활성화됨" : "§c상태: 비활성화됨");
            lore.add("§7현재 값: §a" + value);
            lore.add(" ");
            lore.add("§e좌클릭: §f활성/비활성 전환");
            lore.add("§e우클릭: §f값 설정 (소수점 가능)");
            lore.add("§eShift+우클릭: §f초기화");
            statMeta.setLore(lore);
            statItem.setItemMeta(statMeta);
            inventory.setItem(startSlot + i, statItem);
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
        event.setCancelled(true);

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
        // 이 메서드가 호출되기 전에 이미 event.setCancelled(true)가 실행되었지만,
        // 코드의 명확성을 위해 한 번 더 호출합니다.
        event.setCancelled(true);
        if (event.getClick() == ClickType.LEFT) {
            promptForMoney();
        } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
            codexItem.getReward().setMoney(0);
            draw();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.5f);
        }
    }

    private void handleStatRewardClick(InventoryClickEvent event) {
        // 이 메서드가 호출되기 전에 이미 event.setCancelled(true)가 실행되었지만,
        // 코드의 명확성을 위해 한 번 더 호출합니다.
        event.setCancelled(true);
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
        player.sendMessage("§a[JCodex] 채팅으로 설정할 돈 보상 금액을 입력해주세요. (숫자만)");
        plugin.getGuiManager().promptChatInput(player, input -> {
            try {
                double amount = Double.parseDouble(input);
                codexItem.getReward().setMoney(amount);
                player.sendMessage("§a돈 보상이 §e" + amount + "§a(으)로 설정되었습니다.");
            } catch (NumberFormatException e) {
                player.sendMessage("§c잘못된 숫자 형식입니다.");
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(this));
        });
    }

    private void promptForStat(String statKey) {
        player.closeInventory();
        MainConfig.StatDefinition statDef = plugin.getConfigManager().getMainConfig().getStatDefinition(statKey);
        player.sendMessage("§a[JCodex] 채팅으로 '" + statDef.getDisplay() + "' 스탯 보상 값을 입력해주세요. (소수점 가능)");
        plugin.getGuiManager().promptChatInput(player, input -> {
            try {
                double value = Double.parseDouble(input);
                codexItem.getReward().getStats().put(statKey, value);
                player.sendMessage("§a" + statDef.getDisplay() + " 스탯 보상이 §e" + value + "§a(으)로 설정되었습니다.");
            } catch (NumberFormatException e) {
                player.sendMessage("§c잘못된 숫자 형식입니다.");
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().openGui(this));
        });
    }

    private void saveAndClose() {
        codexRepository.saveOrUpdateItem(codexItem).thenRun(() -> {
            player.sendMessage("§a보상 설정이 성공적으로 저장되었습니다.");
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

