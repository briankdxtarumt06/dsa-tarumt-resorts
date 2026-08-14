package tarumtresort.entity;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.enums.*;

public class Member implements Comparable<Member> {
    private String memberId;
    private int points;
    private Tier tier;
    private LocalDateTime enrollmentDate;
    private String guestId;
    /**
     * The member's point transactions and redemption records, stored inside
     * members.json (serialised as JSON arrays by LinkedListTypeAdapterFactory).
     */
    private LinkedListInterface<PointTransaction> pointTransactionList = new LinkedList<>();
    private LinkedListInterface<RedemptionRecord> redemptionRecordList = new LinkedList<>();

    public Member() {
    }

    public Member(String memberId, int points, Tier tier, LocalDateTime enrollmentDate, String guestId) {
        this.memberId = memberId;
        this.points = points;
        this.tier = tier;
        this.enrollmentDate = enrollmentDate;
        this.guestId = guestId;
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

    /**
     * @return this member's point transactions. Lazily initialised so it is
     * never null, even for a member loaded from JSON.
     */
    public LinkedListInterface<PointTransaction> getPointTransactionList() {
        if (pointTransactionList == null) {
            pointTransactionList = new LinkedList<>();
        }
        return pointTransactionList;
    }

    /** Adds a point transaction to this member's list. */
    public void addPointTransaction(PointTransaction transaction) {
        getPointTransactionList().addSorted(transaction);
    }

    /**
     * @return this member's redemption records. Lazily initialised so it is
     * never null, even for a member loaded from JSON.
     */
    public LinkedListInterface<RedemptionRecord> getRedemptionRecordList() {
        if (redemptionRecordList == null) {
            redemptionRecordList = new LinkedList<>();
        }
        return redemptionRecordList;
    }

    /** Adds a redemption record to this member's list. */
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
