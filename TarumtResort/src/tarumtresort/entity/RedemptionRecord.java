package tarumtresort.entity;

import java.time.LocalDateTime;
import tarumtresort.entity.enums.RoomType;

public class RedemptionRecord implements Comparable<RedemptionRecord> {
    private String redemptionId;
    private LocalDateTime redeemedDate;
    private String memberId;
    private String rewardId;
    private String status;
    private String voucherCode;
    private Double voucherValue;
    private RoomType roomType;
    private Integer discountPercent;
    private boolean used;

    public RedemptionRecord() {
    }

    public RedemptionRecord(String redemptionId, LocalDateTime redeemedDate, String memberId, String rewardId) {
        this.redemptionId = redemptionId;
        this.redeemedDate = redeemedDate;
        this.memberId = memberId;
        this.rewardId = rewardId;
        this.status = "PENDING";
    }

    public String getRedemptionId() {
        return redemptionId;
    }

    public void setRedemptionId(String redemptionId) {
        this.redemptionId = redemptionId;
    }

    public LocalDateTime getRedeemedDate() {
        return redeemedDate;
    }

    public void setRedeemedDate(LocalDateTime redeemedDate) {
        this.redeemedDate = redeemedDate;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getRewardId() {
        return rewardId;
    }

    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public Double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(Double voucherValue) {
        this.voucherValue = voucherValue;
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

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    @Override
    public int compareTo(RedemptionRecord other) {
        if (this.redeemedDate == null && other.redeemedDate == null) {
            // both null
        } else if (this.redeemedDate == null) {
            return -1;
        } else if (other.redeemedDate == null) {
            return 1;
        } else {
            int byDate = this.redeemedDate.compareTo(other.redeemedDate);
            if (byDate != 0) {
                return byDate;
            }
        }
        if (this.redemptionId == null && other.redemptionId == null) return 0;
        if (this.redemptionId == null) return -1;
        if (other.redemptionId == null) return 1;
        return this.redemptionId.compareTo(other.redemptionId);
    }

    @Override
    public String toString() {
        return "RedemptionRecord{" + "redemptionId=" + redemptionId
                + ", redeemedDate=" + redeemedDate + ", memberId=" + memberId
                + ", rewardId=" + rewardId + '}';
    }
}
