package tarumtresort.entity;

import tarumtresort.entity.enums.*;

public class Reservation implements Comparable<Reservation>{
    private String reservationId;
    private String confirmationNumber;
    private String guestId;
    private String roomId;
    private RoomType roomTypeRequested;
    private int numberOfGuests;
    private int numberOfNights;
    private ReservationType reservationType;
    private ReservationStatus status;     
    private boolean isNoShow;        
    private ReservationTimestamps timestamps;

    //constructors
    public Reservation(String reservationId, String confirmationNumber, String guestId, String roomId, RoomType roomTypeRequested, int numberOfGuests, int numberOfNights, ReservationType reservationType, ReservationStatus status, boolean isNoShow, ReservationTimestamps timestamps) {
        this.reservationId = reservationId;
        this.confirmationNumber = confirmationNumber;
        this.guestId = guestId;
        this.roomId = roomId;
        this.roomTypeRequested = roomTypeRequested;
        this.numberOfGuests = numberOfGuests;
        this.numberOfNights = numberOfNights;
        this.reservationType = reservationType;
        this.status = status;
        this.isNoShow = isNoShow;
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
    public void setReservationType(ReservationType reservationType) { this.reservationType = reservationType; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public void serIsNoShow(boolean isNoShow){ this.isNoShow = isNoShow; }
    public void setTimestamps(ReservationTimestamps timestamps) { this.timestamps = timestamps; }

    //getters
    public String getReservationId() { return reservationId; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public String getGuestId() { return guestId; }
    public String getRoomId() { return roomId; }
    public RoomType getRoomTypeRequested() { return roomTypeRequested; }
    public int getNumberOfGuests() { return numberOfGuests; }
    public int getNumberOfNights() { return numberOfNights; }
    public ReservationType getReservationType() { return reservationType; }
    public ReservationStatus getStatus() { return status; }
    public boolean getIsNoShow(){ return isNoShow; }
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

    @Override 
    public int compareTo(Reservation other){ // compare the reservaiton type over the arrival time of the quest

        //compare the reservation type (advance booking > walk in)
        if (this.reservationType != other.reservationType) {
            return this.reservationType == ReservationType.ADVANCE_BOOKING ? -1 : 1;
        }

         return this.timestamps.getRegistrationTimestamp()
           .compareTo(other.timestamps.getRegistrationTimestamp());
    }
}
