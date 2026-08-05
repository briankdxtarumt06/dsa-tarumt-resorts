package tarumtresort.entity;

import tarumtresort.entity.enums.*;

public class Reservation {
    private String reservationId;
    private String confirmationNumber;
    private String guestId;
    private String roomId;
    private RoomType roomTypeRequested;
    private int numberOfGuests;
    private int numberOfNights;
    private String reservationType;
    private String status;
    private ReservationTimestamps timestamps;

    //constructors
    public Reservation(String reservationId, String confirmationNumber,
                       String guestId, String roomId,
                       RoomType roomTypeRequested, int numberOfGuests,
                       int numberOfNights, String reservationType,
                       String status, ReservationTimestamps timestamps) {
        this.reservationId = reservationId;
        this.confirmationNumber = confirmationNumber;
        this.guestId = guestId;
        this.roomId = roomId;
        this.roomTypeRequested = roomTypeRequested;
        this.numberOfGuests = numberOfGuests;
        this.numberOfNights = numberOfNights;
        this.reservationType = reservationType;
        this.status = status;
        this.timestamps = timestamps;
    }

    //setters
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }
    public void setGuestId(String guestId) { this.guestId = guestId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomTypeRequested(RoomType roomTypeRequested) { this.roomTypeRequested = roomTypeRequested; }
    public void setNumberOfGuests(int numberOfGuests) { this.numberOfGuests = numberOfGuests; }
    public void setNumberOfNights(int numberOfNights) { this.numberOfNights = numberOfNights; }
    public void setReservationType(String reservationType) { this.reservationType = reservationType; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamps(ReservationTimestamps timestamps) { this.timestamps = timestamps; }

    //getters
    public String getReservationId() { return reservationId; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public String getGuestId() { return guestId; }
    public String getRoomId() { return roomId; }
    public RoomType getRoomTypeRequested() { return roomTypeRequested; }
    public int getNumberOfGuests() { return numberOfGuests; }
    public int getNumberOfNights() { return numberOfNights; }
    public String getReservationType() { return reservationType; }
    public String getStatus() { return status; }
    public ReservationTimestamps getTimestamps() { return timestamps; }

    // toString
    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId='" + reservationId + '\'' +
                ", confirmationNumber='" + confirmationNumber + '\'' +
                ", guestId='" + guestId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", roomTypeRequested=" + roomTypeRequested +
                ", numberOfGuests=" + numberOfGuests +
                ", numberOfNights=" + numberOfNights +
                ", reservationType='" + reservationType + '\'' +
                ", status='" + status + '\'' +
                ", timestamps=" + timestamps +
                '}';
    }
}
