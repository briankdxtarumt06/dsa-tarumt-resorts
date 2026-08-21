package tarumtresort.entity;

import java.time.LocalDate;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.enums.*;

public class Room implements Comparable<Room>{
    private String roomId;
    private String roomNumber;
    private RoomType roomType;
    private RoomStatus roomStatus;
    private double pricePerNight;
    private transient ListInterface <Reservation> reservations;

    //constructor
    public Room(String roomId, String roomNumber, RoomType roomType, RoomStatus roomStatus, double pricePerNight) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomStatus = roomStatus;
        this.pricePerNight = pricePerNight;
        this.reservations = new DoublyLinkedList<>();
    }

    //getters 
    public String getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public RoomType getRoomType() { return roomType; }
    public RoomStatus getRoomStatus() { return roomStatus; }
    public double getPricePerNight() { return pricePerNight; }
    public ListInterface<Reservation> getReservations() {
        if (reservations == null) {
            reservations = new DoublyLinkedList<>();
        }
        return reservations;
    }

    //setters
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public void setRoomStatus(RoomStatus roomStatus) { this.roomStatus = roomStatus; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    @Override
    public String toString() {
        return "Room{" +
                "roomId='" + roomId + '\'' +
                ", roomNumber='" + roomNumber + '\'' +
                ", roomType='" + roomType + '\'' +
                ", roomStatus=" + roomStatus +
                ", pricePerNight=" + pricePerNight +
                '}';
    }

    @Override
    public int compareTo(Room other) {
        return this.roomId.compareTo(other.roomId);
    }

    public boolean isAvailableForDateRange(LocalDate checkIn, LocalDate checkOut) {
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);

            LocalDate existingCheckIn = r.getTimestamps().getExpectedCheckInDate();
            LocalDate existingCheckOut = r.getTimestamps().getExpectedCheckOutDate();

            boolean overlap = checkIn.isBefore(existingCheckOut) && checkOut.isAfter(existingCheckIn);

            if (overlap) {
                return false;
            }
        }
        return true;
    }
}
