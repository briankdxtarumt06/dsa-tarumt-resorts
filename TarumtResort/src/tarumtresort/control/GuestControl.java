package tarumtresort.control;

import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.NationalityDAO;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.GuestUI;
import tarumtresort.entity.Guest;
import tarumtresort.utility.ConsoleUtil;

public class GuestControl {

    private static final String[] DEFAULT_NATIONALITIES = {
        "Malaysian", "Singaporean", "Indonesian", "Chinese", "Indian",
        "Thai", "Korean", "Japanese", "American", "British", "Saudi Arabian"
    };

    // list declared
    private LinkedListInterface<Guest> guestList = new LinkedList<>();
    private LinkedListInterface<String> customNationalities = new LinkedList<>();

    // dao 
    private static final GuestDAO guestDAO = new GuestDAO();
    private static final NationalityDAO nationalityDAO = new NationalityDAO();
  
    // 
    private GuestUI guestUI = new GuestUI();

    // Constructor
    public GuestControl() {
        guestDAO.loadFromFile(guestList);

        String[] loaded = nationalityDAO.loadCustomNationalities();
        for (String n : loaded) {
            customNationalities.addBack(n);
        }
    }

    private static final int PAGE_SIZE = 20;

    // entry point for guest management (replaces old registerGuest-only flow)
    public void runGuestManagement() {
        String nationalityFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Guest> display;
            if (nationalityFilter != null) {
                display = getGuestsByNationality(nationalityFilter);
            } else {
                display = guestList;
            }

            boolean hasFilter = nationalityFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            LinkedListInterface<Guest> pageList = pageOf(display, page);
            int choice = guestUI.printGuestListMenu(pageList, page, pageCount, hasFilter);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewGuest(pageList);
            } else if (choice == action++) { // 2. Register New Guest
                registerGuest();
            } else if (choice == action++) { // 3. Filter by Nationality
                nationalityFilter = guestUI.inputNationality(getNationalityOptions());
                page = 0;
            } else {
                boolean matched = false;
                if (page < pageCount - 1) {
                    matched = choice == action;
                    action++;
                    if (matched) page++;
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched) page--;
                }
                if (!matched && hasFilter) {
                    matched = choice == action;
                    action++;
                    if (matched) {
                        nationalityFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    private LinkedListInterface<Guest> pageOf(LinkedListInterface<Guest> source, int page) {
        LinkedListInterface<Guest> result = new LinkedList<>();
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, source.size());
        for (int i = startIndex; i < endIndex; i++) {
            result.addBack(source.get(i));
        }
        return result;
    }

    public LinkedListInterface<Guest> getGuestsByNationality(String nationality) {
        LinkedListInterface<Guest> result = new LinkedList<>();
        for (int i = 0; i < guestList.size(); i++) {
            if (guestList.get(i).getNationality().equalsIgnoreCase(nationality)) {
                result.addBack(guestList.get(i));
            }
        }
        return result;
    }

    // view flow: pick a record from the current page, then run its action menu
    private void viewGuest(LinkedListInterface<Guest> pageList) {
        if (pageList.isEmpty()) {
            guestUI.printNoRecords();
            guestUI.pressEnterToContinue();
            return;
        }
        int num = guestUI.inputListIndex("guest", pageList.size());
        if (num == 0) {
            return;
        }
        Guest guest = pageList.get(num - 1);
        if (guest != null) {
            handleGuestActions(guest);
        }
    }

    // select-entity action loop: details -> action -> details, until Back
    private void handleGuestActions(Guest guest) {
        while (true) {
            guestUI.printGuestDetails(guest);

            int action = guestUI.getGuestActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1:
                    guestUI.printGuestReservationHistory(guest.getReservations());
                    guestUI.pressEnterToContinue();
                    System.err.println();
                    break;
                default:
                    break;
            }

            guest = getGuestById(guest.getGuestId()); 
        }
    }

    // case 1: register a new guest - continue with menu/ room booking
    public Guest registerGuest() {
        String name = capitalizeName(guestUI.inputName());
        if (name.equals("0")) return null;
        
        while (isDuplicateName(name)) {
            guestUI.printInvalidInput("Guest with this name already exists!");
            name = capitalizeName(guestUI.inputName());
        }
        
        String nationality = guestUI.inputNationality(getNationalityOptions());
        addNationalityIfNew(nationality);

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

        guestUI.printGuestDetails(guest);
        guestUI.printSuccess();
        guestUI.pressEnterToContinue();

        return guest;
    }

    // update guest
    public boolean updateGuest(String guestId,
                            String name,
                            String icOrPassport,
                            String contactNumber,
                            String nationality,
                            String address) {

        // IC / Passport cannot be duplicated with another guest
        Guest owner = getGuestByIcOrPassport(icOrPassport);
        if (owner != null && !owner.getGuestId().equals(guestId)) {
            return false;
        }

        // update guest information
        Guest guest = getGuestById(guestId);
        if (guest == null) {
            return false;
        }

        guest.setName(name);
        guest.setIcOrPassport(icOrPassport);
        guest.setContactNumber(contactNumber);
        guest.setNationality(nationality);
        guest.setAddress(address);

        //guestDAO.saveGuestList(guestList);

        return true;
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

    public String[] getNationalityOptions() {
        String[] result = new String[DEFAULT_NATIONALITIES.length + customNationalities.size()];
        System.arraycopy(DEFAULT_NATIONALITIES, 0, result, 0, DEFAULT_NATIONALITIES.length);
        for (int i = 0; i < customNationalities.size(); i++) {
            result[DEFAULT_NATIONALITIES.length + i] = customNationalities.get(i);
        }
        return result;
    }

    public void addNationalityIfNew(String nationality) {
        for (String d : DEFAULT_NATIONALITIES) {
            if (d.equalsIgnoreCase(nationality)) return;
        }
        for (int i = 0; i < customNationalities.size(); i++) {
            if (customNationalities.get(i).equalsIgnoreCase(nationality)) return;
        }
        customNationalities.addBack(nationality);
        saveCustomNationalities();
    }

    private void saveCustomNationalities() {
        String[] arr = new String[customNationalities.size()];
        for (int i = 0; i < customNationalities.size(); i++) {
            arr[i] = customNationalities.get(i);
        }
        nationalityDAO.saveCustomNationalities(arr);
    }

    // get all guests
    public LinkedListInterface<Guest> getAllGuests() {
        return guestList;
    }

    // generate guest id
    public String generateGuestId() {

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
            String trimmed = value.trim();

            if (trimmed.length() != 12 || !trimmed.chars().allMatch(Character::isDigit)) {
                guestUI.printInvalidInput("Invalid IC format! Must be 12 digits, no dashes (e.g. 060322140562)");
                continue;
            }

            String digits = trimmed;

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
        return guestExistsByIcOrPassport(icOrPassport);
    }

    // check if name repeated
    public boolean isDuplicateName(String name) {
        for (int i = 0; i < guestList.size(); i++) {
            if (guestList.get(i).getName().equalsIgnoreCase(name)) {
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
