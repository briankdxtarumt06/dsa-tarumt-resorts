package tarumtresort.entity;

import java.time.LocalDateTime;
import tarumtresort.entity.enums.*;

public class Member {
    private String memberId;
    private int points;
    private Tier tier;
    private LocalDateTime enrollmentDate;
    private String guestId;

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

    @Override
    public String toString() {
        return "Member{" + "memberId=" + memberId + ", points=" + points
                + ", tier=" + tier + ", enrollmentDate=" + enrollmentDate
                + ", guestId=" + guestId + '}';
    }
}
