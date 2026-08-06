package tarumtresort.entity;

public class Reward {
    private String rewardId;
    private String name;
    private String description;
    private int pointCost;

    public Reward() {
    }

    public Reward(String rewardId, String name, String description, int pointCost) {
        this.rewardId = rewardId;
        this.name = name;
        this.description = description;
        this.pointCost = pointCost;
    }

    public String getRewardId() {
        return rewardId;
    }

    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPointCost() {
        return pointCost;
    }

    public void setPointCost(int pointCost) {
        this.pointCost = pointCost;
    }

    @Override
    public String toString() {
        return "Reward{" + "rewardId=" + rewardId + ", name=" + name
                + ", description=" + description + ", pointCost=" + pointCost + '}';
    }
}
