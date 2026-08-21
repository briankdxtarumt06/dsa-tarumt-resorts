package tarumtresort.entity;

import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.Tier;

// Author: Imam Mahdi Ali Ang Attuko
public class Reward implements Comparable<Reward> {
    private String rewardId;
    private String name;
    private String description;
    private int pointCost;
    private Double voucherValue;
    private Tier minTier;
    private RoomType roomType;
    private Integer discountPercent;
    private boolean isDeleted;

    public Reward() {
    }

    public Reward(String rewardId, String name, String description, int pointCost) {
        this(rewardId, name, description, pointCost, null, null, null, null);
    }

    public Reward(String rewardId, String name, String description, int pointCost, Double voucherValue) {
        this(rewardId, name, description, pointCost, voucherValue, null, null, null);
    }

    public Reward(String rewardId, String name, String description, int pointCost,
            Double voucherValue, Tier minTier, RoomType roomType) {
        this(rewardId, name, description, pointCost, voucherValue, minTier, roomType, null);
    }

    public Reward(String rewardId, String name, String description, int pointCost,
            Double voucherValue, Tier minTier, RoomType roomType, Integer discountPercent) {
        this.rewardId = rewardId;
        this.name = name;
        this.description = description;
        this.pointCost = pointCost;
        this.voucherValue = voucherValue;
        this.minTier = minTier;
        this.roomType = roomType;
        this.discountPercent = discountPercent;
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

    public Double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(Double voucherValue) {
        this.voucherValue = voucherValue;
    }

    public Tier getMinTier() {
        return minTier;
    }

    public void setMinTier(Tier minTier) {
        this.minTier = minTier;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
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
