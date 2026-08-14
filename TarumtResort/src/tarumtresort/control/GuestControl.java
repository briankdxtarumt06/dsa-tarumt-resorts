package tarumtresort.control;

import tarumtresort.dao.GuestDAO;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;

public class GuestControl {

    // list declared
    private LinkedListInterface<Guest> guestList = new LinkedList<>();
    
    // dao 
    private static final GuestDAO guestDAO = new GuestDAO();
    
    // Constructor
    //public GuestControl() {
     //   guestList = guestDAO.retrieveGuestList();
    //}

    // register guest
    public String registerGuest(String name, String icOrPassport, String contactNumber, String nationality, String address) {

        // IC / Passport cannot be duplicated
        for (int i = 0; i < guestList.size(); i++) {

            if (guestList.get(i).getIcOrPassport().equals(icOrPassport)) {
                return null;
            }
        }

        String guestId = generateGuestId();

        Guest guest = new Guest(
                guestId,
                name,
                icOrPassport,
                contactNumber,
                nationality,
                address
        );

        guestList.addBack(guest);

        //guestDAO.saveGuestList(guestList);

        return guestId;
    }

    // update guest
    public boolean updateGuest(String guestId,
                            String name,
                            String icOrPassport,
                            String contactNumber,
                            String nationality,
                            String address) {

        // IC / Passport cannot be duplicated
        for (int i = 0; i < guestList.size(); i++) {

            Guest otherGuest = guestList.get(i);

            if (!otherGuest.getGuestId().equals(guestId)
                    && otherGuest.getIcOrPassport().equals(icOrPassport)) {

                return false;
            }
        }

        // update guest information
        for (int i = 0; i < guestList.size(); i++) {

            Guest guest = guestList.get(i);

            if (guest.getGuestId().equals(guestId)) {

                guest.setName(name);
                guest.setIcOrPassport(icOrPassport);
                guest.setContactNumber(contactNumber);
                guest.setNationality(nationality);
                guest.setAddress(address);

                //guestDAO.saveGuestList(guestList);

                return true;
            }
        }

        return false;
    }

    // get guest by guest id
    public Guest getGuestById(String guestId) {

        for (int i = 0; i < guestList.size(); i++) {

            Guest guest = guestList.get(i);

            if (guest.getGuestId().equals(guestId)) {
                return guest;
            }
        }

        return null;
    }

    // get guest by IC / Passport
    public Guest getGuestByIcOrPassport(String icOrPassport) {

        for (int i = 0; i < guestList.size(); i++) {

            Guest guest = guestList.get(i);

            if (guest.getIcOrPassport().equals(icOrPassport)) {
                return guest;
            }
        }

        return null;
    }

    // get guest by contact number
    public Guest getGuestByContactNumber(String contactNumber) {

        for (int i = 0; i < guestList.size(); i++) {

            Guest guest = guestList.get(i);

            if (guest.getContactNumber().equals(contactNumber)) {
                return guest;
            }
        }

        return null;
    }

    // get all guests
    public LinkedListInterface<Guest> getAllGuests() {
        return guestList;
    }

    // generate guest id
    private String generateGuestId() {

        int max = 0;

        for (int i = 0; i < guestList.size(); i++) {

            String guestId = guestList.get(i).getGuestId();

            int number = Integer.parseInt(guestId.substring(3));

            if (number > max) {
                max = number;
            }
        }

        return String.format("GST%03d", max + 1);
    }

    // check if the guest exits
    public boolean guestExists(String guestId) {
        return getGuestById(guestId) != null;
    }

    public boolean guestExistsByIcOrPassport(String icOrPassport) {
        return getGuestByIcOrPassport(icOrPassport) != null;
    }

    public boolean guestExistsByContactNumber(String contactNumber) {
        return getGuestByContactNumber(contactNumber) != null;
    }

    public String getGuestName(String guestId) {
        Guest guest = getGuestById(guestId);

        if (guest == null) {
            return null;
        }

        return guest.getName();
    }

}
