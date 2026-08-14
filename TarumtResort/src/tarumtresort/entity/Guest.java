package tarumtresort.entity;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

/**
 * Guest entity. Combines the member/points module needs (notificationList)
 * with the room/reservation module needs (reservations).
 */
public class Guest implements Comparable<Guest> {
    private String guestId;
    private String name;
    private String icOrPassport;
    private String contactNumber;
    private String nationality;
    private String address;
    private LinkedListInterface<Notification> notificationList = new LinkedList<>();
    private LinkedListInterface<Reservation> reservations;

    // constructor (no arguments) - used by Gson when loading from JSON
    public Guest() {
    }

    //Constructor
    public Guest(String guestId, String name, String icOrPassport, String contactNumber, String nationality, String address) {
        this(guestId, name, icOrPassport, contactNumber, nationality, address, new LinkedList<>());
    }

    /**
     * Full constructor that also takes the guest's notification list.
     *
     * @param notificationList this guest's notifications; a null value is
     *                         replaced with an empty list so
     *                         {@link #getNotificationList()} is never null.
     */
    public Guest(String guestId, String name, String icOrPassport, String contactNumber, String nationality,
            String address, LinkedListInterface<Notification> notificationList) {
        this.guestId = guestId;
        this.name = name;
        this.icOrPassport = icOrPassport;
        this.contactNumber = contactNumber;
        this.nationality = nationality;
        this.address = address;
        this.notificationList = notificationList == null ? new LinkedList<>() : notificationList;
        this.reservations = new LinkedList<>();
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

    /**
     * @return this guest's notifications. Lazily initialised so it is
     * never null, even for a guest loaded from JSON (Gson does not run
     * field initialisers when deserialising).
     */
    public LinkedListInterface<Notification> getNotificationList() {
        if (notificationList == null) {
            notificationList = new LinkedList<>();
        }
        return notificationList;
    }

    /** Adds a notification to this guest's list. */
    public void addNotification(Notification notification) {
        getNotificationList().addSorted(notification);
    }

    /**
     * @return this guest's reservations. Lazily initialised so it is never
     * null, even for a guest loaded from JSON.
     */
    public LinkedListInterface<Reservation> getReservations() {
        if (reservations == null) {
            reservations = new LinkedList<>();
        }
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
