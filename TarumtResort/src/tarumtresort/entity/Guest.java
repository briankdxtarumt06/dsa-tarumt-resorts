package tarumtresort.entity;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

public class Guest implements Comparable<Guest> {
    private String guestId;
    private String name;
    private String icOrPassport;
    private String contactNumber;
    private String nationality;
    private String address;          
    private LinkedListInterface<Notification> notificationList = new LinkedList<>();

    public Guest() {
    }

    public Guest(String guestId, String name, String icOrPassport, String contactNumber, String nationality, String address) {
        this.guestId = guestId;
        this.name = name;
        this.icOrPassport = icOrPassport;
        this.contactNumber = contactNumber;
        this.nationality = nationality;
        this.address = address;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcOrPassport() {
        return icOrPassport;
    }

    public void setIcOrPassport(String icOrPassport) {
        this.icOrPassport = icOrPassport;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return this guest's notifications. Lazily initialised so it is never
     * null, even for a guest loaded from JSON (Gson does not run field
     * initialisers when deserialising).
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

    @Override
    public int compareTo(Guest other) {
        return this.guestId.compareTo(other.guestId);
    }

    @Override
    public String toString() {
        return "Guest{" + "guestId=" + guestId + ", name=" + name
                + ", icOrPassport=" + icOrPassport + ", contactNumber=" + contactNumber
                + ", nationality=" + nationality + ", address=" + address + '}';
    }
}
