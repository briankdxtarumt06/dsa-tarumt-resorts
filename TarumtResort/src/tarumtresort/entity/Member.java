package tarumtresort.entity;

import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.enums.*;

public class Member implements Comparable<Member> {
    private String memberId;
    private int points;
    private Tier tier;
    private LocalDateTime enrollmentDate;
    private String guestId;
    private boolean isDeleted;
    private ListInterface<PointTransaction> pointTransactionList = new DoublyLinkedList<>();
    private ListInterface<RedemptionRecord> redemptionRecordList = new DoublyLinkedList<>();

    public Member() {
    }

    public Member(String memberId, int points, Tier tier, LocalDateTime enrollmentDate, String guestId) {
        this(memberId, points, tier, enrollmentDate, guestId, new DoublyLinkedList<>(), new DoublyLinkedList<>());
    }

    public Member(String memberId, int points, Tier tier, LocalDateTime enrollmentDate, String guestId,
            ListInterface<PointTransaction> pointTransactionList,
            ListInterface<RedemptionRecord> redemptionRecordList) {
        this.memberId = memberId;
        this.points = points;
        this.tier = tier;
        this.enrollmentDate = enrollmentDate;
        this.guestId = guestId;
        this.pointTransactionList = pointTransactionList;
        this.redemptionRecordList = redemptionRecordList;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Tier getTier() {
        return tier;
    }

    public void setTier(Tier tier) {
        this.tier = tier;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public ListInterface<PointTransaction> getPointTransactionList() {
        if (pointTransactionList == null) {
            pointTransactionList = new DoublyLinkedList<>();
        }
        return pointTransactionList;
    }

    public void addPointTransaction(PointTransaction transaction) {
        getPointTransactionList().addSorted(transaction);
    }

    public ListInterface<RedemptionRecord> getRedemptionRecordList() {
        if (redemptionRecordList == null) {
            redemptionRecordList = new DoublyLinkedList<>();
        }
        return redemptionRecordList;
    }

    public void addRedemptionRecord(RedemptionRecord record) {
        getRedemptionRecordList().addSorted(record);
    }

    @Override
    public int compareTo(Member other) {
        return this.memberId.compareTo(other.memberId);
    }

    @Override
    public String toString() {
        return "Member{" + "memberId=" + memberId + ", points=" + points
                + ", tier=" + tier + ", enrollmentDate=" + enrollmentDate
                + ", guestId=" + guestId + '}';
    }
}
