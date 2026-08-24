package tarumtresort.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Author: Chai Chee Tong

public class ReservationTimestamps {

    private LocalDateTime registrationTimestamp; // The moment guest arrives at front desk to register (both Walk-in & Advance Booking)
    private LocalDate expectedCheckInDate; // The date guest plans to check in (Walk-in = today, Advance Booking = future date)
    private LocalDate expectedCheckOutDate; // The date guest plans to check out (auto-calculated = expectedCheckInDate + numberOfNights)
    private LocalDateTime assignedTime; // The moment a room is assigned to the guest (both Walk-in & Advance Booking)
    private LocalDateTime actualCheckInTime; // The moment guest officially receives the room key (both Walk-in & Advance Booking)
    private LocalDateTime actualCheckOutTime; // The moment guest leaves the hotel (both Walk-in & Advance Booking)

    // Constructor
    public ReservationTimestamps(LocalDateTime registrationTimestamp, LocalDate expectedCheckInDate,
            LocalDate expectedCheckOutDate) {
        this.registrationTimestamp = registrationTimestamp;
        this.expectedCheckInDate = expectedCheckInDate;
        this.expectedCheckOutDate = expectedCheckOutDate;
        this.assignedTime = null;
        this.actualCheckInTime = null;
        this.actualCheckOutTime = null;
    }

    // Getters
    public LocalDateTime getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public LocalDate getExpectedCheckInDate() {
        return expectedCheckInDate;
    }

    public LocalDate getExpectedCheckOutDate() {
        return expectedCheckOutDate;
    }

    public LocalDateTime getAssignedTime() {
        return assignedTime;
    }

    public LocalDateTime getActualCheckInTime() {
        return actualCheckInTime;
    }

    public LocalDateTime getActualCheckOutTime() {
        return actualCheckOutTime;
    }

    // Setters
    public void setRegistrationTimestamp(LocalDateTime registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public void setExpectedCheckInDate(LocalDate expectedCheckInDate) {
        this.expectedCheckInDate = expectedCheckInDate;
    }

    public void setExpectedCheckOutDate(LocalDate expectedCheckOutDate) {
        this.expectedCheckOutDate = expectedCheckOutDate;
    }

    public void setAssignedTime(LocalDateTime assignedTime) {
        this.assignedTime = assignedTime;
    }

    public void setActualCheckInTime(LocalDateTime actualCheckInTime) {
        this.actualCheckInTime = actualCheckInTime;
    }

    public void setActualCheckOutTime(LocalDateTime actualCheckOutTime) {
        this.actualCheckOutTime = actualCheckOutTime;
    }

    // toString
    @Override
    public String toString() {
        return "ReservationTimestamps{" +
                "registrationTimestamp=" + registrationTimestamp +
                ", expectedCheckInDate=" + expectedCheckInDate +
                ", expectedCheckOutDate=" + expectedCheckOutDate +
                ", assignedTime=" + assignedTime +
                ", actualCheckInTime=" + actualCheckInTime +
                ", actualCheckOutTime=" + actualCheckOutTime +
                '}';
    }
}