package tarumtresort.entity;

import tarumtresort.entity.enums.*;

public class PriorityReservation implements Comparable<PriorityReservation> {

    private String reservationId;
    private PriorityLevel priorityLevel;
    private String overriddenBy;
    private String overrideReason;

    public PriorityReservation() {
    }

    public PriorityReservation(String reservationId, PriorityLevel priorityLevel) {
        this.reservationId = reservationId;
        this.priorityLevel = priorityLevel;
        this.overriddenBy = null;
        this.overrideReason = null;
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

    public int compareTo(PriorityReservation other) {
        return this.priorityLevel.compareTo(other.priorityLevel);
    }

    @Override
    public String toString() {
        return "PriorityReservation [reservationId=" + reservationId + ", priorityLevel=" + priorityLevel
                + ", overriddenBy=" + overriddenBy + ", overrideReason=" + overrideReason + "]";
    }
}