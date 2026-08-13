package tarumtresort.entity;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.enums.*;
import tarumtresort.adt.LinkedList;

public class Room implements Comparable<Room>{
    private String roomId;
    private String roomNumber;
    private RoomType roomType;
    private RoomStatus roomStatus;
    private double pricePerNight;
    private LinkedListInterface <Reservation> reservations;

    //constructor
    public Room(String roomId, String roomNumber, RoomType roomType, RoomStatus roomStatus, double pricePerNight) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomStatus = roomStatus;
        this.pricePerNight = pricePerNight;
        this.reservations = new LinkedList<>();
    }

    //getters 
    public String getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public RoomType getRoomType() { return roomType; }
    public RoomStatus getRoomStatus() { return roomStatus; }
    public double getPricePerNight() { return pricePerNight; }
    public LinkedListInterface<Reservation> getReservations() { return reservations; }

    //setters
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public void setRoomStatus(RoomStatus roomStatus) { this.roomStatus = roomStatus; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    @Override
    public String toString(){
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
}
