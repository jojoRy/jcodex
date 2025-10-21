package kr.jjory.jcodex.model;

/**
 * 도감 달성도 보상 정보를 담는 데이터 클래스입니다.
 */
public class Milestone {
    private final int id;
    private final String type; // GLOBAL, CATEGORY
    private final CodexCategory category;
    private final int percent;
    private final RewardSpec reward;

    public Milestone(int id, String type, CodexCategory category, int percent, RewardSpec reward) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.percent = percent;
        this.reward = reward;
    }

    // Getters
    public int getId() { return id; }
    public String getType() { return type; }
    public CodexCategory getCategory() { return category; }
    public int getPercent() { return percent; }
    public RewardSpec getReward() { return reward; }
}
