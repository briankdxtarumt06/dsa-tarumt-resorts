package tarumtresort.control;

import tarumtresort.dao.GuestDAO;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.GuestUI;
import tarumtresort.entity.Guest;

public class GuestControl {

    // list declared
    private LinkedListInterface<Guest> guestList = new LinkedList<>();
    
    // dao 
    private static final GuestDAO guestDAO = new GuestDAO();
    
    // 
    private GuestUI guestUI = new GuestUI();

    // Constructor
    public GuestControl() {
        guestDAO.loadFromFile(guestList);
    }

    // case 1: register a new guest - continue with menu/ room booking
    public Guest registerGuest() {
        String name = capitalizeName(guestUI.inputName());
        String nationality = guestUI.inputNationality();

        String icOrPassport;
        if (nationality.equalsIgnoreCase("Malaysian")) {
            icOrPassport = inputValidIc();
        } else {
            icOrPassport = inputValidPassport();
        }

        while (isDuplicateIc(icOrPassport)) {
            guestUI.printInvalidInput("Guest already exists!");
            icOrPassport = nationality.equalsIgnoreCase("Malaysian")
                ? inputValidIc()
                : inputValidPassport();
        }

        String contactNumber = guestUI.inputContactNumber();
        String address = guestUI.inputAddress();

        String guestId = generateGuestId();
        Guest guest = new Guest(guestId, name, icOrPassport, contactNumber, nationality, address);
        guestList.addBack(guest);
        guestDAO.saveToFile(guestList);

        return guest;
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

    // VALIDATION METHODS
    // check ic validation 
    private String inputValidIc() {
        String value = "";
        while (true) {
            value = guestUI.inputIc();  // call UI to get input
            if (value == null) continue;
            String digits = value.replace("-", "").trim();

            if (digits.length() != 12 || !digits.chars().allMatch(Character::isDigit)) {
                guestUI.printInvalidInput("Invalid IC format!");
                continue;
            }

            int mm = Integer.parseInt(digits.substring(2, 4));
            int dd = Integer.parseInt(digits.substring(4, 6));
            String bp = digits.substring(6, 8);

            if (mm < 1 || mm > 12) {
                guestUI.printInvalidInput("Invalid IC format!");
                continue;
            }

            int[] daysInMonth = {31,29,31,30,31,30,31,31,30,31,30,31};
            if (dd < 1 || dd > daysInMonth[mm - 1]) {
                guestUI.printInvalidInput("Invalid IC format!");
                continue;
            }

            String[] validBpCodes = {
                "01","21","22","23","24","02","25","26","27","03","28","29",
                "04","30","05","31","59","06","32","33","07","34","35",
                "08","36","37","38","39","09","40","10","41","42","43","44",
                "11","45","46","12","47","48","49","13","50","51","52","53",
                "14","54","55","56","57","15","58","16"
            };

            boolean valid = false;
            for (String code : validBpCodes) {
                if (code.equals(bp)) { valid = true; break; }
            }

            if (!valid) {
                guestUI.printInvalidInput("Invalid IC format!");
                continue;
            }

            return value;
        }
    }

    private String inputValidPassport() {
        String value = "";
        while (true) {
            value = guestUI.inputPassport();  // call UI to get input
            if (value == null) continue;
            String trimmed = value.trim();

            if (trimmed.length() < 6 || trimmed.length() > 9) {
                guestUI.printInvalidInput("Invalid passport format!");
                continue;
            }

            boolean valid = true;
            for (int i = 0; i < trimmed.length(); i++) {
                if (!Character.isLetterOrDigit(trimmed.charAt(i))) {
                    valid = false;
                    break;
                }
            }

            if (!valid) {
                guestUI.printInvalidInput("Invalid passport format!");
                continue;
            }

            return value;
        }
    }

    // check if the ic or passport repeated
    public boolean isDuplicateIc(String icOrPassport) {
        for (int i = 0; i < guestList.size(); i++) {
            if (guestList.get(i).getIcOrPassport().equals(icOrPassport)) {
                return true;
            }
        }
        return false;
    }

    //OTHERS
    public String capitalizeName(String name) {
        String[] guestName = name.trim().split(" ");
        String result = "";

        for (int i = 0; i < guestName.length; i++) {
            if (guestName[i].length() > 0) {
                String firstLetter = guestName[i].substring(0, 1).toUpperCase();
                String rest = guestName[i].substring(1).toLowerCase();
                result += firstLetter + rest;
                if (i < guestName.length - 1) {
                    result += " ";
                }
            }
        }
        return result;
    }

    public void saveGuestList() {
        guestDAO.saveToFile(guestList);
    }
}
