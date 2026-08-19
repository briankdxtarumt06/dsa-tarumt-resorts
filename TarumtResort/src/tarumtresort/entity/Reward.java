package tarumtresort.entity;

import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.Tier;

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
    /**
     * Lowest tier that may redeem this reward. Null means SILVER (everyone).
     */
    private Tier minTier;
    /**
     * Room type this voucher applies to (null = generic voucher, any room type).
     */
    private RoomType roomType;
    /**
     * Percentage discount for percent-type vouchers (e.g. 20 = 20% off the
     * room charge of the matching room type). Null means this reward is NOT
     * a percentage voucher (it is either a fixed-RM voucher or not a voucher).
     */
    private Integer discountPercent;

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

    /**
     * @return the lowest tier allowed to redeem this reward (null = SILVER).
     */
    public Tier getMinTier() {
        return minTier;
    }

    public void setMinTier(Tier minTier) {
        this.minTier = minTier;
    }

    /**
     * @return the room type this voucher applies to, or null for a generic voucher.
     */
    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    /**
     * @return the percentage discount for percent-type vouchers, or null if
     *         this reward is not a percentage voucher.
     */
    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
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
