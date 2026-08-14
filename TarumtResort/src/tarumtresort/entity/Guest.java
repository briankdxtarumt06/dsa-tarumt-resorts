package tarumtresort.entity;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.adt.LinkedList;

public class Guest implements Comparable<Guest> {
    private String guestId;
    private String name;
    private String icOrPassport;
    private String contactNumber;
    private String nationality;
    private String address;
    private LinkedListInterface<Reservation> reservations;

    // Constructor
    public Guest(String guestId, String name, String icOrPassport, String contactNumber, String nationality,
            String address) {
        this.guestId = guestId;
        this.name = name;
        this.icOrPassport = icOrPassport;
        this.contactNumber = contactNumber;
        this.nationality = nationality;
        this.address = address;
        this.reservations = new LinkedList<>();
    }

    // constructor (no arguments)
    public Guest() {
    }

    // setters
    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIcOrPassport(String icOrPassport) {
        this.icOrPassport = icOrPassport;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // getters
    public String getGuestId() {
        return guestId;
    }

    public String getName() {
        return name;
    }

    public String getIcOrPassport() {
        return icOrPassport;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public String getAddress() {
        return address;
    }

    public LinkedListInterface<Reservation> getReservations() {
        return reservations;
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
