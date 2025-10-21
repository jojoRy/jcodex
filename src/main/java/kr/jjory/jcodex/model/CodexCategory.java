package kr.jjory.jcodex.model;

import org.bukkit.Material;

/**
 * 도감 아이템의 카테고리를 정의하는 열거형 클래스입니다.
 */
public enum CodexCategory {
    FARMER("농부", "농작물, 씨앗 등 농사와 관련된 아이템", Material.WHEAT),
    MINER("광부", "광물, 원석 등 채광과 관련된 아이템", Material.DIAMOND_PICKAXE),
    FISHER("어부", "물고기 등 낚시와 관련된 아이템", Material.FISHING_ROD),
    COOK("요리사", "요리 재료 및 완성된 음식 아이템", Material.COOKED_BEEF),
    WARRIOR("전투", "무기, 방어구 등 전투와 관련된 아이템", Material.IRON_SWORD),
    MISC("기타", "특수하거나 분류하기 어려운 아이템", Material.ELYTRA);

    private final String displayName;
    private final String description;
    private final Material icon;

    CodexCategory(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }
}
