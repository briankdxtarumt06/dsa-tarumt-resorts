package tarumtresort.entity;

public class PriorityReservation {

    private String reservationId;
    private PriorityLevel priorityLevel;
    private String overriddenBy;
    private String overrideReason;

    

    public PriorityReservation() {
    }

    public PriorityReservation(String reservationId, PriorityLevel priorityLevel, String overriddenBy,
            String overrideReason) {
        this.reservationId = reservationId;
        this.priorityLevel = priorityLevel;
        this.overriddenBy = overriddenBy;
        this.overrideReason = overrideReason;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    @Override
    public String toString() {
        return "PriorityReservation [reservationId=" + reservationId + ", priorityLevel=" + priorityLevel
                + ", overriddenBy=" + overriddenBy + ", overrideReason=" + overrideReason + "]";
    }

}

enum PriorityLevel {
    PENALTY(0),
    STANDARD(10),
    ELITE(20),
    DIAMOND(30),
    PLATINUM(40),
    VIP_OVERRIDE(50),
    EMERGENCY(60);

    private final int rank;

    PriorityLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static PriorityLevel fromTier(String tier) {
        if (tier == null)
            return STANDARD;
        return switch (tier.toUpperCase()) {
            case "PLATINUM" -> PLATINUM;
            case "DIAMOND" -> DIAMOND;
            case "ELITE" -> ELITE;
            default -> STANDARD;
        };
    }
}
