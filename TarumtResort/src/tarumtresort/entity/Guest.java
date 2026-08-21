package tarumtresort.entity;

import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;

// Author: Chai Chee Tong

public class Guest implements Comparable<Guest> {
    private String guestId;
    private String name;
    private String icOrPassport;
    private String contactNumber;
    private String nationality;
    private String address;
    private ListInterface<Notification> notificationList = new DoublyLinkedList<>();
    private transient ListInterface<Reservation> reservations;

    // constructor 
    public Guest() {
    }

    //Constructor
    public Guest(String guestId, String name, String icOrPassport, String contactNumber, String nationality, String address) {
        this(guestId, name, icOrPassport, contactNumber, nationality, address, new DoublyLinkedList<>());
    }

    public Guest(String guestId, String name, String icOrPassport, String contactNumber, String nationality,
            String address, ListInterface<Notification> notificationList) {
        this.guestId = guestId;
        this.name = name;
        this.icOrPassport = icOrPassport;
        this.contactNumber = contactNumber;
        this.nationality = nationality;
        this.address = address;
        this.notificationList = notificationList == null ? new DoublyLinkedList<>() : notificationList;
        this.reservations = new DoublyLinkedList<>();
    }

    //setters
    public void setGuestId(String guestId) { this.guestId = guestId; }
    public void setName(String name) { this.name = name; }
    public void setIcOrPassport(String icOrPassport) { this.icOrPassport = icOrPassport; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setAddress(String address) { this.address = address; }

    //getters
    public String getGuestId() { return guestId; }
    public String getName() { return name; }
    public String getIcOrPassport() { return icOrPassport; }
    public String getContactNumber() { return contactNumber; }
    public String getNationality() { return nationality; }
    public String getAddress() { return address; }
    public ListInterface<Reservation> getReservations(){
        if (reservations == null) {
            reservations = new DoublyLinkedList<>();
        }
        return reservations;
    }

    public ListInterface<Notification> getNotificationList() {
        if (notificationList == null) {
            notificationList = new DoublyLinkedList<>();
        }
        return notificationList;
    }

    public void addNotification(Notification notification) {
        getNotificationList().addSorted(notification);
    }

    @Override
    public String toString() {
        return "Guest{" +
                "guestId='" + guestId + '\'' +
                ", name='" + name + '\'' +
                ", icOrPassport='" + icOrPassport + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", nationality='" + nationality + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    @Override
    public int compareTo(Guest other) {
        return this.guestId.compareTo(other.guestId);
    }
}