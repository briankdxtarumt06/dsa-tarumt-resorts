package tarumtresort.entity;

public class RedemptionRecord {
    private String redemptionId;
    private String redeemedDate;
    private String memberId;
    private String rewardId;

    public RedemptionRecord() {
    }

    public RedemptionRecord(String redemptionId, String redeemedDate, String memberId, String rewardId) {
        this.redemptionId = redemptionId;
        this.redeemedDate = redeemedDate;
        this.memberId = memberId;
        this.rewardId = rewardId;
    }

    public String getRedemptionId() {
        return redemptionId;
    }

    public void setRedemptionId(String redemptionId) {
        this.redemptionId = redemptionId;
    }

    public String getRedeemedDate() {
        return redeemedDate;
    }

    public void setRedeemedDate(String redeemedDate) {
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

    @Override
    public String toString() {
        return "RedemptionRecord{" + "redemptionId=" + redemptionId
                + ", redeemedDate=" + redeemedDate + ", memberId=" + memberId
                + ", rewardId=" + rewardId + '}';
    }
}
