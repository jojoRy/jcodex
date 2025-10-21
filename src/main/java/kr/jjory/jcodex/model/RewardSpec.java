package kr.jjory.jcodex.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 도감 해금 또는 달성도 보상의 상세 내용을 담는 데이터 클래스입니다.
 */
public class RewardSpec {
    private List<ItemStack> rewardItems;
    private double money;
    private final Map<String, Double> stats;
    private final String command;

    public RewardSpec(List<ItemStack> rewardItems, double money, Map<String, Double> stats, String command) {
        this.rewardItems = rewardItems;
        this.money = money;
        this.stats = stats;
        this.command = command;
    }

    public List<ItemStack> getRewardItems() {
        return rewardItems;
    }

    public double getMoney() {
        return money;
    }

    public Map<String, Double> getStats() {
        return stats;
    }

    public String getCommand() {
        return command;
    }

    public void setRewardItems(List<ItemStack> rewardItems) {
        this.rewardItems = rewardItems;
    }

    public void setMoney(double money) {
        this.money = money;
    }
}

