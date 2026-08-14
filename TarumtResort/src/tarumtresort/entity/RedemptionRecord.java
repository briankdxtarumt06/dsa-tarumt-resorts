package tarumtresort.entity;

import java.time.LocalDateTime;

public class RedemptionRecord implements Comparable<RedemptionRecord> {
    private String redemptionId;
    private LocalDateTime redeemedDate;
    private String memberId;
    private String rewardId;
    /** PENDING, APPROVED or REJECTED. */
    private String status;

    public RedemptionRecord() {
    }

    public RedemptionRecord(String redemptionId, LocalDateTime redeemedDate, String memberId, String rewardId) {
        this.redemptionId = redemptionId;
        this.redeemedDate = redeemedDate;
        this.memberId = memberId;
        this.rewardId = rewardId;
        this.status = "PENDING"; // new redemptions start as requests
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

    @Override
    public int compareTo(RedemptionRecord other) {
        int byDate = this.redeemedDate.compareTo(other.redeemedDate);
        if (byDate != 0) {
            return byDate;
        }
        return this.redemptionId.compareTo(other.redemptionId);
    }

    @Override
    public String toString() {
        return "RedemptionRecord{" + "redemptionId=" + redemptionId
                + ", redeemedDate=" + redeemedDate + ", memberId=" + memberId
                + ", rewardId=" + rewardId + '}';
    }
}
