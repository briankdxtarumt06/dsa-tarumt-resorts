package tarumtresort.entity;

public class Reward implements Comparable<Reward> {
    private String rewardId;
    private String name;
    private String description;
    private int pointCost;
    /**
     * Cash value in RM for voucher-type rewards (e.g. RM20 Dining Voucher = 20.0).
     * Null means this reward is NOT a voucher (experience rewards etc.).
     */
    private Double voucherValue;

    public Reward() {
    }

    public Reward(String rewardId, String name, String description, int pointCost) {
        this(rewardId, name, description, pointCost, null);
    }

    public Reward(String rewardId, String name, String description, int pointCost, Double voucherValue) {
        this.rewardId = rewardId;
        this.name = name;
        this.description = description;
        this.pointCost = pointCost;
        this.voucherValue = voucherValue;
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

    /**
     * @return the RM value of this reward if it is a voucher, or null if it is not.
     */
    public Double getVoucherValue() {
        return voucherValue;
    }

    /** Sets the RM value for voucher-type rewards; null means not a voucher. */
    public void setVoucherValue(Double voucherValue) {
        this.voucherValue = voucherValue;
    }

    @Override
    public int compareTo(Reward other) {
        int byCost = Integer.compare(this.pointCost, other.pointCost);
        if (byCost != 0) {
            return byCost;
        }
        return this.rewardId.compareTo(other.rewardId);
    }

    @Override
    public String toString() {
        return "Reward{" + "rewardId=" + rewardId + ", name=" + name
                + ", description=" + description + ", pointCost=" + pointCost + '}';
    }
}
