package tarumtresort.entity;


public class Room {
    private String roomId;
    private String roomNumber;
    private RoomType roomType;
    private RoomStatus roomStatus;
    private double pricePerNight;

    //constructor
    public Room(String roomId, String roomNumber, RoomType roomType,
                RoomStatus roomStatus, double pricePerNight) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomStatus = roomStatus;
        this.pricePerNight = pricePerNight;
    }

    //getters 
    public String getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public RoomType getRoomType() { return roomType; }
    public RoomStatus getRoomStatus() { return roomStatus; }
    public double getPricePerNight() { return pricePerNight; }
    
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
}
