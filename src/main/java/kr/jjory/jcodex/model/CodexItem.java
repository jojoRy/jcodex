package kr.jjory.jcodex.model;

/**
 * 도감에 등록되는 개별 아이템의 정보를 담는 데이터 클래스입니다.
 */
public class CodexItem {
    private final String itemId;
    private final String displayName;
    private final CodexCategory category;
    private final RewardSpec reward;

    public CodexItem(String itemId, String displayName, CodexCategory category, RewardSpec reward) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.category = category;
        this.reward = reward;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CodexCategory getCategory() {
        return category;
    }

    public RewardSpec getReward() {
        return reward;
    }
}
