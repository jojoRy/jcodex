package kr.jjory.jcodex.model;

import java.util.Set;
import java.util.UUID;

/**
 * 플레이어의 도감 진행 상황을 담는 데이터 클래스입니다.
 */
public class PlayerCodexProgress {
    private final UUID uuid;
    private final Set<String> registeredItemIds;
    private final Set<Integer> claimedMilestoneIds;

    public PlayerCodexProgress(UUID uuid, Set<String> registeredItemIds, Set<Integer> claimedMilestoneIds) {
        this.uuid = uuid;
        this.registeredItemIds = registeredItemIds;
        this.claimedMilestoneIds = claimedMilestoneIds;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Set<String> getRegisteredItemIds() {
        return registeredItemIds;
    }

    public Set<Integer> getClaimedMilestoneIds() {
        return claimedMilestoneIds;
    }
}
