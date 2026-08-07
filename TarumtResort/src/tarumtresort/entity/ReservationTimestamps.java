package tarumtresort.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservationTimestamps {
    private LocalDateTime bookingTimeStamp;
    private LocalDateTime registrationTimestamp;
    private LocalDate expectedCheckInDate;
    private LocalDate expectedCheckOutDate;
    private LocalDateTime assignedTime;
    private LocalDateTime actualCheckInTime;
    private LocalDateTime actualCheckOutTime;

    // Constructor
    public ReservationTimestamps(LocalDateTime bookingTimeStamp, LocalDateTime registrationTimestamp,
                                  LocalDate expectedCheckInDate,
                                  LocalDate expectedCheckOutDate) {
        this.registrationTimestamp = registrationTimestamp;
        this.expectedCheckInDate = expectedCheckInDate;
        this.expectedCheckOutDate = expectedCheckOutDate;
        this.assignedTime = null;
        this.actualCheckInTime = null;
        this.actualCheckOutTime = null;
    }

    //setters
    public void setBookingTimeStamp(LocalDateTime bookingTimeStamp) { this.bookingTimeStamp = bookingTimeStamp; }
    public void setRegistrationTimestamp(LocalDateTime registrationTimestamp) { this.registrationTimestamp = registrationTimestamp; }
    public void setExpectedCheckInDate(LocalDate expectedCheckInDate) { this.expectedCheckInDate = expectedCheckInDate; }
    public void setExpectedCheckOutDate(LocalDate expectedCheckOutDate) { this.expectedCheckOutDate = expectedCheckOutDate; }
    public void setAssignedTime(LocalDateTime assignedTime) { this.assignedTime = assignedTime; }
    public void setActualCheckInTime(LocalDateTime actualCheckInTime) { this.actualCheckInTime = actualCheckInTime; }
    public void setActualCheckOutTime(LocalDateTime actualCheckOutTime) { this.actualCheckOutTime = actualCheckOutTime; }

    //getters
    public LocalDateTime getBookingTimeStamp() { return bookingTimeStamp; }
    public LocalDateTime getRegistrationTimestamp() { return registrationTimestamp; }
    public LocalDate getExpectedCheckInDate() { return expectedCheckInDate; }
    public LocalDate getExpectedCheckOutDate() { return expectedCheckOutDate; }
    public LocalDateTime getAssignedTime() { return assignedTime; }
    public LocalDateTime getActualCheckInTime() { return actualCheckInTime; }
    public LocalDateTime getActualCheckOutTime() { return actualCheckOutTime; }

    

}
