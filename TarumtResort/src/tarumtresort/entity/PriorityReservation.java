package tarumtresort.entity;

import tarumtresort.entity.enums.*;

// Author: Lee Boon Yew
public class PriorityReservation implements Comparable<PriorityReservation> {

    private String reservationId;
    private PriorityLevel priorityLevel;
    private String overriddenBy;
    private String overrideReason;
    private boolean isDeleted = false;

    public PriorityReservation() {
    }

    public PriorityReservation(String reservationId, PriorityLevel priorityLevel) {
        this.reservationId = reservationId;
        this.priorityLevel = priorityLevel;
        this.overriddenBy = null;
        this.overrideReason = null;
        this.isDeleted = false;
    }

    public PriorityReservation(String reservationId, PriorityLevel priorityLevel, String overriddenBy,
            String overrideReason, boolean isDeleted) {
        this.reservationId = reservationId;
        this.priorityLevel = priorityLevel;
        this.overriddenBy = overriddenBy;
        this.overrideReason = overrideReason;
        this.isDeleted = isDeleted;
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

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public int compareTo(PriorityReservation other) {
        return this.priorityLevel.compareTo(other.priorityLevel);
    }

    @Override
    public String toString() {
        return "PriorityReservation [reservationId=" + reservationId + ", priorityLevel=" + priorityLevel
                + ", overriddenBy=" + overriddenBy + ", overrideReason=" + overrideReason + "]";
    }

    @Override
    public int hashCode() {
        return reservationId == null ? 0 : reservationId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PriorityReservation other = (PriorityReservation) obj;
        return reservationId == null
                ? other.reservationId == null
                : reservationId.equals(other.reservationId);
    }

}