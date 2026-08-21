package tarumtresort.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.ReservationUI;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.NationalityDAO;
import tarumtresort.dao.PaymentDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.dao.RoomDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Payment;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.ReservationTimestamps;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.PaymentMethod;
import tarumtresort.entity.enums.PaymentStatus;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.ReservationType;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.RoomType;

public class ReservationControl {

    // default nationality options offered when registering a guest
    private static final String[] DEFAULT_NATIONALITIES = {
        "Malaysian", "Singaporean", "Indonesian", "Chinese", "Indian", "Thai", "Korean", "Japanese", "American", "British", "Saudi Arabian"
    };

    // Three reservation lists (booking / guest queue / assigned) 
    private static final int VIEW_BOOKING_LIST = 1;
    private static final int VIEW_GUEST_QUEUE = 2;
    private static final int VIEW_ASSIGNED_LIST = 3;

    // paging: how many entity rows fit on one list page
    private static final int PAGE_SIZE = 20;

    // business rule: a guest still CHECKED_IN past the checkout hour (11am) on their expectedCheckOutDate is forcibly checked out so the room can be freed up for the guest queue
    private static final int FORCE_CHECKOUT_HOUR = 11;

    // ui declaration
    private ReservationUI reservationUI = new ReservationUI();

    public ReservationUI getReservationUI() {
        return reservationUI;
    }

    // List declaration
    private LinkedListInterface<Guest> guestList = new LinkedList<>();
    private LinkedListInterface<String> customNationalities = new LinkedList<>();
    private LinkedListInterface<Reservation> bookingList = new LinkedList<>();
    private LinkedListInterface<Reservation> guestQueue = new LinkedList<>();
    private LinkedListInterface<Reservation> assignedList = new LinkedList<>();
    private LinkedListInterface<Room> roomList = new LinkedList<>();

    // DAO declarations
    private static final GuestDAO guestDAO = new GuestDAO();
    private static final NationalityDAO nationalityDAO = new NationalityDAO();
    private static final ReservationDAO reservationDAO = new ReservationDAO();
    private static final RoomDAO roomDAO = new RoomDAO();

    // Controller
    private PaymentControl paymentControl = new PaymentControl();
    private PriorityReservationController priorityReservationController = new PriorityReservationController();
    private LoyaltyController loyaltyController = new LoyaltyController();
    private HousekeepingController housekeepingController = new HousekeepingController();

    // Constructors
    public ReservationControl() {
        reservationDAO.loadBookingList(bookingList);
        reservationDAO.loadGuestQueue(guestQueue);
        reservationDAO.loadAssignedList(assignedList);

        guestDAO.loadFromFile(guestList);

        String[] loaded = nationalityDAO.loadCustomNationalities();
        for (String n : loaded) {
            customNationalities.addBack(n);
        }

        roomDAO.loadFromFile(roomList);
    }

    // ===== ENTRY POINT =====

    public void runReservationModule() {
        int choice = 0;
        do {
            choice = reservationUI.getMenuChoice();
            switch (choice) {
                case 0: break;
                case 1: runGuestManagement(); break;
                case 2: runReservationManagement(); break;
                case 3: runRoomManagement(); break;
                case 4: generateReport(); break;
                default: break;
            }
        } while (choice != 0);
    }

    public static void main(String[] args) {
        ReservationControl reservationControl = new ReservationControl();
        reservationControl.runReservationModule();
    }

    // ===== GUEST MANAGEMENT =====

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

            LinkedListInterface<Guest> pageList = pageOfGuests(display, page);
            int choice = reservationUI.printGuestListMenu(pageList, page, pageCount, hasFilter, this::memberIdOf);

            if (choice == 0) {
                break;
            }

            int action = 1;
            if (choice == action++) { // 1. View Details
                viewGuest(pageList);
            } else if (choice == action++) { // 2. Register New Guest
                createGuest();
            } else if (choice == action++) { // 3. Register as Member
                registerGuestAsMember(pageList);
            } else if (choice == action++) { // 4. Filter by Nationality
                nationalityFilter = reservationUI.inputNationality(getNationalityOptions());
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

    // view flow: pick a record from the current page, then run its action menu
    private void viewGuest(LinkedListInterface<Guest> pageList) {
        if (pageList.isEmpty()) {
            reservationUI.printNoRecords();
            reservationUI.pressEnterToContinue();
            return;
        }
        int num = reservationUI.inputListIndex("guest", pageList.size());
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
            reservationUI.printGuestDetails(guest, membershipOf(guest.getGuestId()));

            int action = reservationUI.getGuestActionChoice();
            if (action == 0) {
                return;
            }

            switch (action) {
                case 1:
                    reservationUI.printGuestReservationHistory(guest.getReservations());
                    reservationUI.pressEnterToContinue();
                    System.err.println();
                    break;
                default:
                    break;
            }

            guest = getGuestById(guest.getGuestId());
        }
    }

    // action 3: register a guest (picked from the current page) as a loyalty member
    private void registerGuestAsMember(LinkedListInterface<Guest> pageList) {
        if (pageList.isEmpty()) {
            reservationUI.printNoRecords();
            reservationUI.pressEnterToContinue();
            return;
        }
        int num = reservationUI.inputListIndex("guest", pageList.size());
        if (num == 0) {
            return;
        }
        Guest guest = pageList.get(num - 1);
        if (guest == null) {
            return;
        }
        if (loyaltyController.findMemberByGuestId(guest.getGuestId()) != null) {
            System.out.println("\n  This guest is already a member: " + membershipOf(guest.getGuestId()) + ".");
            reservationUI.pressEnterToContinue();
            return;
        }
        if (!reservationUI.askConfirmation(
                "Register " + guest.getName() + " as member?",
                "Register member", "Cancel")) {
            return;
        }
        System.out.println("\n  " + loyaltyController.registerMember(guest));
        reservationUI.pressEnterToContinue();
    }

    // member id shown in the guest list "Member" column ("-" when not a member)
    private String memberIdOf(String guestId) {
        Member m = loyaltyController.findMemberByGuestId(guestId);
        return m == null ? "-" : m.getMemberId();
    }

    // membership summary shown in guest details ("-" when not a member)
    private String membershipOf(String guestId) {
        Member m = loyaltyController.findMemberByGuestId(guestId);
        return m == null ? "-" : m.getMemberId() + " (" + m.getTier() + ")";
    }

    // case 1: register a new guest - continue with menu/ room booking
    public Guest createGuest() {
        String name = capitalizeName(reservationUI.inputName());
        if (name.equals("0")) return null;

        while (isDuplicateName(name)) {
            reservationUI.printInvalidInput("Guest with this name already exists!");
            name = capitalizeName(reservationUI.inputName());
        }

        String nationality = reservationUI.inputNationality(getNationalityOptions());
        addNationalityIfNew(nationality);

        String icOrPassport;
        if (nationality.equalsIgnoreCase("Malaysian")) {
            icOrPassport = inputValidIc();
        } else {
            icOrPassport = inputValidPassport();
        }

        while (isDuplicateIc(icOrPassport)) {
            reservationUI.printInvalidInput("Guest already exists!");
            icOrPassport = nationality.equalsIgnoreCase("Malaysian")
                ? inputValidIc()
                : inputValidPassport();
        }

        String contactNumber = reservationUI.inputContactNumber();
        String address = reservationUI.inputAddress();

        String guestId = generateGuestId();
        Guest guest = new Guest(guestId, name, icOrPassport, contactNumber, nationality, address);
        guestList.addBack(guest);
        guestDAO.saveToFile(guestList);

        reservationUI.printGuestDetails(guest, "-");
        reservationUI.printSuccess();
        reservationUI.pressEnterToContinue();

        return guest;
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

    public void saveGuestList() {
        guestDAO.saveToFile(guestList);
    }

    // ===== RESERVATION MANAGEMENT =====

    public void runReservationManagement() {
        int currentView = VIEW_GUEST_QUEUE;
        RoomType roomTypeFilter = null;

        while (true) {
            forceCheckoutOverdueReservations();
            detectNoShowReservations();

            LinkedListInterface<Reservation> sourceList;
            String currentListName;
            switch (currentView) {
                case VIEW_BOOKING_LIST:
                    sourceList = bookingList;
                    currentListName = "Booking List";
                    break;
                case VIEW_ASSIGNED_LIST:
                    sourceList = assignedList;
                    currentListName = "Assigned List";
                    break;
                default:
                    sourceList = guestQueue;
                    currentListName = "Guest Queue";
                    break;
            }

            LinkedListInterface<Reservation> displayList = sourceList;
            if (roomTypeFilter != null) {
                displayList = new LinkedList<>();
                for (int i = 0; i < sourceList.size(); i++) {
                    if (sourceList.get(i).getRoomTypeRequested() == roomTypeFilter) {
                        displayList.addBack(sourceList.get(i));
                    }
                }
                currentListName += " (Filtered: " + roomTypeFilter + ")";
            }

            boolean hasFilter = roomTypeFilter != null;
            int choice = reservationUI.printReservationListMenu(currentListName, buildQueueTableData(displayList), hasFilter);

            if (hasFilter && choice == 12) {
                roomTypeFilter = null;
                continue;
            }

            switch (choice) {
                case 0: return;
                case 1: currentView = VIEW_BOOKING_LIST; roomTypeFilter = null; break;
                case 2: currentView = VIEW_GUEST_QUEUE; roomTypeFilter = null; break;
                case 3: currentView = VIEW_ASSIGNED_LIST; roomTypeFilter = null; break;
                case 4: {
                    int roomChoice = reservationUI.inputRoomTypeChoice();
                    if (roomChoice != 0) {
                        roomTypeFilter = intToRoomType(roomChoice);
                    }
                    break;
                }
                case 5: bookRoom(); break;
                case 6: guestArrival(); break;
                case 7: assignRoom(); break;
                case 8: checkIn(); break;
                case 9: checkOut(); break;
                case 10: checkQueuePosition(); break;
                case 11: cancelReservation(); break;
                default: break;
            }
        }
    }

    // business rule: guests still CHECKED_IN past FORCE_CHECKOUT_HOUR on their expectedCheckOutDate are forcibly checked out so the room can be freed up for the guest queue
    private void forceCheckoutOverdueReservations() {
        LocalDateTime now = LocalDateTime.now();
        LinkedListInterface<Reservation> forcedOut = new LinkedList<>();

        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getStatus() != ReservationStatus.CHECKED_IN) continue;

            LocalDateTime deadline = r.getTimestamps().getExpectedCheckOutDate().atTime(FORCE_CHECKOUT_HOUR, 0);
            if (now.isAfter(deadline)) {
                r.setStatus(ReservationStatus.CHECKED_OUT);
                r.getTimestamps().setActualCheckOutTime(now);
                updateRoomStatus(r.getRoomId(), RoomStatus.CLEANING);
                forcedOut.addBack(r);
            }
        }

        if (forcedOut.size() > 0) {
            reservationDAO.saveAssignedList(assignedList);
            reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            saveGuestList();

            System.out.println("\n" + forcedOut.size() + " guest(s) were automatically checked out for exceeding the "
                + FORCE_CHECKOUT_HOUR + ":00 checkout deadline.");
            reservationUI.printWaitingQueueTable(buildCheckOutSummaryTableData(forcedOut));
            reservationUI.pressEnterToContinue();
        }
    }

    // business rule: an advance booking past its expectedCheckInDate with no arrival is a no-show, treated as a cancellation  
    private void detectNoShowReservations() {
        LocalDate today = LocalDate.now();
        LinkedListInterface<Reservation> noShows = new LinkedList<>();

        for (int i = bookingList.size() - 1; i >= 0; i--) {
            Reservation r = bookingList.get(i);
            if (r.getStatus() != ReservationStatus.WAITING) continue;

            if (today.isAfter(r.getTimestamps().getExpectedCheckInDate())) {
                bookingList.removeIndex(i);

                Guest guest = getGuestById(r.getGuestId());
                if (guest != null) {
                    removeReservationFromGuest(guest, r);
                }

                handleRefund(r);
                noShows.addBack(r);
            }
        }

        if (noShows.size() > 0) {
            reservationDAO.saveBookingList(bookingList);
            reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            saveGuestList();

            System.out.println("\n" + noShows.size() + " advance booking(s) auto-cancelled as no-show (guest never arrived on the expected check-in date).");
            reservationUI.printWaitingQueueTable(buildNoShowTableData(noShows));
            reservationUI.pressEnterToContinue();
        }
    }

    // case 1 
    public void registerGuest() {
        Guest guest = createGuest();

        if (guest == null) return;

        String[][] options = {
            {"1", "Book a room"},
            {"2","Back to menu"},
            {"3","Continue another guest registration"},
            {"0", "Back to main menu"}
        };

        int choice = reservationUI.showSubMenu("Next?", options);

        switch (choice) {
            case 1: bookRoom(guest.getGuestId()); break;
            case 2: break;
            case 3: registerGuest();
            case 0: // back tom main menu
            default: break;
        }
    }

    // case 2
    public void bookRoom() {
        Guest guest = selectGuestForBooking();
        if (guest == null) return;
        bookRoom(guest.getGuestId());
    }

    // paginated guest picker for bookRoom() - guestList can be large, unlike the small
    // reservation-status lists used elsewhere (checkIn/checkOut/cancel/queue)
    private Guest selectGuestForBooking() {
        int page = 0;
        while (true) {
            int pageCount = Math.max(1, (guestList.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) page = pageCount - 1;
            LinkedListInterface<Guest> pageList = pageOfGuests(guestList, page);

            int choice = reservationUI.printGuestSelectionMenu(buildGuestSelectionTableData(pageList), page, pageCount);
            if (choice == 0) return null;

            int action = pageList.size();
            if (choice <= action) {
                return pageList.get(choice - 1);
            }

            if (page < pageCount - 1) {
                action++;
                if (choice == action) { page++; continue; }
            }
            if (page > 0) {
                action++;
                if (choice == action) { page--; continue; }
            }
            action++;
            if (choice == action) { // Register New Guest
                Guest newGuest = createGuest();
                if (newGuest != null) return newGuest;
            }
        }
    }

    public void bookRoom(String guestId) {
        // reservation type
        System.out.println();
        int typeChoice = reservationUI.printBookingTypeMenu();

        if (typeChoice == 0) return;

        ReservationType reservationType = typeChoice == 1
            ? ReservationType.WALK_IN
            : ReservationType.ADVANCE_BOOKING;


        // ask once, shared for all rooms in for the same guest
        int numberOfNights = reservationUI.inputNumberOfNights();

        LocalDate expectedCheckInDate;
        if (reservationType == ReservationType.WALK_IN) {
            expectedCheckInDate = LocalDate.now();
        } else {
            expectedCheckInDate = inputValidDate("Expected check-in date (must be at least 2 days from today)", LocalDate.now().plusDays(2));
            if (expectedCheckInDate == null) {
                return;
            }
        }
        LocalDate expectedCheckOutDate = expectedCheckInDate.plusDays(numberOfNights);

        LinkedListInterface<Reservation> sessionBookings = new LinkedList<>();

        boolean continueBooking = true;
        while (continueBooking) {

            int roomChoice = reservationUI.inputRoomTypeChoice();
            if (roomChoice == 0) break;
            RoomType roomType = intToRoomType(roomChoice);
            if (roomType == null) break;

            // check whether enough rooms of this type exist for the requested date range
            // (reservations booked earlier in this same session are already saved into
            // bookingList/guestQueue by this point, so countOverlappingReservations() alone
            // already reflects them - counting sessionBookings on top would double-count)
            int totalRoomsOfType = countRoomsByType(roomType);
            int overlappingReservations = countOverlappingReservations(roomType, expectedCheckInDate, expectedCheckOutDate);

            if (overlappingReservations >= totalRoomsOfType) {
                reservationUI.printRoomNotAvailable();
                reservationUI.pressEnterToContinue();
                continue;
            }

            int numberOfGuests = reservationUI.inputNumberOfGuests();

            // create timestamps
            ReservationTimestamps timestamps = new ReservationTimestamps(
                LocalDateTime.now(),
                expectedCheckInDate,
                expectedCheckOutDate
            );

            Reservation reservation = new Reservation(
                generateReservationId(),
                generateConfirmationNumber(),
                guestId,
                null,
                roomType,
                numberOfGuests,
                numberOfNights,
                reservationType,
                ReservationStatus.WAITING,
                timestamps
            );

            if (reservationType == ReservationType.WALK_IN) {
                guestQueue.addSorted(reservation);
                reservationDAO.saveGuestQueue(guestQueue);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);

                priorityReservationController.addPriorityReservation(
                        reservation.getReservationId(), reservation.getGuestId());
            } else {
                bookingList.addSorted(reservation);
                reservationDAO.saveBookingList(bookingList);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            }

            Guest guest = getGuestById(guestId);
            if (guest != null) {
                guest.getReservations().addBack(reservation);
                saveGuestList();
            }

            System.out.println();
            reservationUI.printSuccess();
            reservationUI.printReservationDetails(reservation);

            sessionBookings.addBack(reservation);

            // ask book another room
            continueBooking = reservationUI.askConfirmation(
                "Book another room for this guest?",
                "Add another room",
                "Done booking"
            );
        }

        if (sessionBookings.size() > 0) {
            System.out.println("\nBooking Summary");
            reservationUI.printWaitingQueueTable(buildBookingSummaryTableData(sessionBookings));
            System.out.println();
            reservationUI.printSuccess();

            // payment collection (member tier discount + vouchers apply to the whole session)
            Member member = loyaltyController.findMemberByGuestId(guestId);
            Payment payment = paymentControl.processBookingPayment(sessionBookings, this, member, loyaltyController);
            if (payment == null) {
                reservationUI.printError("Payment was cancelled. Reservations are saved but not yet paid.");
            } else {
                reservationUI.printSuccess();
                System.out.println("Payment recorded: " + payment.getPaymentID());
            }
        }

        String[][] options = {
            {"1", "Book room for another guest"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1: bookRoom(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }

    // case 3 
    public void guestArrival() {

        if (bookingList.isEmpty()) {
            reservationUI.printError("No advance bookings found.");
            reservationUI.pressEnterToContinue();
            return;
        }

        // show all advance bookings, soonest expected check-in first
        LinkedListInterface<Reservation> sortedBookings = sortByExpectedCheckIn(bookingList);
        reservationUI.printBookingListForArrival(buildArrivalListTableData(sortedBookings));

        int selection = reservationUI.inputListIndex("booking", sortedBookings.size());
        if (selection == 0) {
            return;
        }

        Reservation found = sortedBookings.get(selection - 1);

        // check if today matches the expected check-in date
        LocalDate expectedCheckInDate = found.getTimestamps().getExpectedCheckInDate();
        if (!expectedCheckInDate.equals(LocalDate.now())) {
            reservationUI.printError("Not yet the expected check-in date! Expected: " + expectedCheckInDate);
            reservationUI.pressEnterToContinue();
            return;
        }

        // show booking details
        reservationUI.printReservationDetails(found);

        // confirm arrival
        if (!reservationUI.askConfirmation( "Confirm guest arrival?", "Guest will be moved to queue", "Cancel arrival")) {
            reservationUI.pressEnterToContinue();
            return;
        }

        // move from bookingList to guestQueue
        bookingList.removeElement(found);
        found.getTimestamps().setRegistrationTimestamp(LocalDateTime.now());
        guestQueue.addSorted(found);

        priorityReservationController.addPriorityReservation(
                found.getReservationId(), found.getGuestId());

        // save both lists
        reservationDAO.saveBookingList(bookingList);
        reservationDAO.saveGuestQueue(guestQueue);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
        saveGuestList();

        reservationUI.printSuccess();

        // show the updated guest queue
        reservationUI.printWaitingQueueTable(buildQueueTableData(guestQueue));

        String[][] options = {
            {"1", "Continue with another guest arrival"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };

        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1: guestArrival(); break;
            case 2: break;
            case 0: // handle back to main menu
            default: break;
        }
    }

    // case 4 
    public void assignRoom() {
        // input room type
        int roomChoice = reservationUI.inputRoomTypeChoice();
        if (roomChoice == 0) return;

        RoomType roomType = intToRoomType(roomChoice);
        if (roomType == null) return;

        // show all AVAILABLE rooms of this type (real-time status, set to AVAILABLE by
        LinkedListInterface<Room> availableRooms = getAvailableRoomsByType(roomType);
        if (availableRooms.isEmpty()) {
            reservationUI.printRoomNotAvailable();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printAvailableRoomList(buildAvailableRoomTableData(availableRooms));
        int roomSelection = reservationUI.inputListIndex("room", availableRooms.size());
        if (roomSelection == 0) return;

        Room availableRoom = availableRooms.get(roomSelection - 1);

        // find first guest in queue matching room type
        Reservation found = null;

        LinkedListInterface<Reservation> vipQueue =
                priorityReservationController.generateVIPQueue(guestQueue);

        for (int i = 0; i < vipQueue.size(); i++) {
            Reservation r = vipQueue.get(i);
            if (r.getRoomTypeRequested() == roomType) {
                found = r;
                break;
            }
        }

        if (found == null) {
            for (int i = 0; i < guestQueue.size(); i++) {
                Reservation r = guestQueue.get(i);
                if (r.getRoomTypeRequested() == roomType) {
                    found = r;
                    break;
                }
            }
        }

        if (found == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // assign room
        int queueIndex = guestQueue.indexOf(found);
        if (queueIndex >= 0) {
            guestQueue.removeIndex(queueIndex);
        }

        priorityReservationController.removeById(found.getReservationId());

        found.setRoomId(availableRoom.getRoomId());
        found.setStatus(ReservationStatus.ASSIGNED);
        found.getTimestamps().setAssignedTime(LocalDateTime.now());

        availableRoom.getReservations().addBack(found);

        updateRoomStatus(availableRoom.getRoomId(), RoomStatus.OCCUPIED);

        assignedList.addBack(found);

        reservationDAO.saveGuestQueue(guestQueue);
        reservationDAO.saveAssignedList(assignedList);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
        saveGuestList();

        reservationUI.printAssignmentSummary(found, availableRoom);
        reservationUI.printSuccess();

        String[][] options = {
            {"1", "Continue with room assignment"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };

        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1: assignRoom(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }

    // case 5 
    public void checkIn() {
        LinkedListInterface<Reservation> candidates = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getStatus() == ReservationStatus.ASSIGNED) {
                candidates.addBack(r);
            }
        }

        if (candidates.isEmpty()) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printCheckInCandidateList(buildCheckInCandidateTableData(candidates));
        int selection = reservationUI.inputListIndex("reservation", candidates.size());
        if (selection == 0) return;

        Reservation found = candidates.get(selection - 1);

        // check time validation (after 12pm)
        if (LocalTime.now().isBefore(LocalTime.of(12, 0))) {
            reservationUI.printCannotCheckIn();
            String[][] options = {
                {"1", "Try again"},
                {"0", "Back to module menu"}
            };
            int choice = reservationUI.showSubMenu("Next?", options);
            switch (choice) {
                case 1: checkIn(); break;
                case 0: break;
                default: break;
            }
            return;
        }

        // check date validation
        if (LocalDate.now().isBefore(found.getTimestamps().getExpectedCheckInDate())) {
            reservationUI.printCannotCheckIn();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printReservationDetails(found);

        if (!reservationUI.askConfirmation(
                "Confirm check in?",
                "- Guest will be checked in",
                "- Cancel check in")) {
            reservationUI.pressEnterToContinue();
            return;
        }

        found.setStatus(ReservationStatus.CHECKED_IN);
        found.getTimestamps().setActualCheckInTime(LocalDateTime.now());
        reservationDAO.saveAssignedList(assignedList);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
        saveGuestList();
        saveRoomList(); // found is also embedded in its room's own reservations list

        reservationUI.printReservationDetails(found);
        reservationUI.printSuccess();

        String[][] options = {
            {"1", "Continue with another check in"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1: checkIn(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }

    // case 6
    public void checkOut() {
        LinkedListInterface<Reservation> checkedIn = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getStatus() == ReservationStatus.CHECKED_IN) {
                checkedIn.addBack(r);
            }
        }

        if (checkedIn.isEmpty()) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printCheckOutCandidateList(buildCheckOutCandidateTableData(checkedIn));
        int selection = reservationUI.inputListIndex("reservation", checkedIn.size());
        if (selection == 0) return;

        String guestId = checkedIn.get(selection - 1).getGuestId();

        // gather ALL currently checked-in rooms for this guest (may be more than the one picked)
        LinkedListInterface<Reservation> checkedInRooms = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getGuestId().equals(guestId) && r.getStatus() == ReservationStatus.CHECKED_IN) {
                checkedInRooms.addBack(r);
            }
        }

        // show all checked-in rooms with numbering
        System.out.println("\nCurrently Checked-In Rooms:");
        for (int i = 0; i < checkedInRooms.size(); i++) {
            Reservation r = checkedInRooms.get(i);
            System.out.println("  " + (i + 1) + ". " + r.getConfirmationNumber() + " - " + r.getRoomTypeRequested());
        }

        // ask: check out all, or select specific rooms
        String[][] scopeOptions = {
            {"1", "Check Out All Rooms"},
            {"2", "Check Out Selected Rooms"},
            {"0", "Cancel"}
        };
        int scopeChoice = reservationUI.showSubMenu("Check-out Option:", scopeOptions);
        if (scopeChoice == 0) return;

        LinkedListInterface<Reservation> toCheckOut = new LinkedList<>();

        if (scopeChoice == 1) {
            toCheckOut = checkedInRooms;
        } else if (scopeChoice == 2) {
            boolean confirmed = false;
            while (!confirmed) {

                if (!reservationUI.askConfirmation(
                        "Proceed to select rooms for check-out?",
                        "- Yes, continue selecting",
                        "- No, cancel check-out")) {
                    return;
                }

                LinkedListInterface<Reservation> selected = new LinkedList<>();

                for (int i = 0; i < checkedInRooms.size(); i++) {
                    Reservation r = checkedInRooms.get(i);
                    boolean wantsToCheckOut = reservationUI.askConfirmation(
                        "Check out room " + (i + 1) + " (" + r.getConfirmationNumber() + " - " + r.getRoomTypeRequested() + ")?",
                        "- Yes, check out this room",
                        "- No, keep this room"
                    );
                    if (wantsToCheckOut) {
                        selected.addBack(r);
                    }
                }

                if (selected.size() == 0) {
                    reservationUI.printError("No rooms selected! You must select at least one room to check out.");
                    continue;
                }

                // show summary in table form and ask for final confirmation
                System.out.println("\nRooms Selected for Check-Out:");
                reservationUI.printWaitingQueueTable(buildCheckOutSelectionTableData(selected));

                boolean proceed = reservationUI.askConfirmation(
                    "Proceed with check-out for these " + selected.size() + " room(s)?",
                    "Continue with check-out",
                    "Go back and reselect"
                );

                if (proceed) {
                    toCheckOut = selected;
                    confirmed = true;
                }
            }
        } else {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < toCheckOut.size(); i++) {
            reservationUI.printReservationDetails(toCheckOut.get(i));
        }

        if (!reservationUI.askConfirmation(
                "Confirm check out for " + toCheckOut.size() + " room(s)?",
                "- Selected room(s) will be checked out",
                "- Cancel check out")) {
            reservationUI.pressEnterToContinue();
            return;
        }

        for (int i = 0; i < toCheckOut.size(); i++) {
            Reservation r = toCheckOut.get(i);
            r.setStatus(ReservationStatus.CHECKED_OUT);
            r.getTimestamps().setActualCheckOutTime(now);
            updateRoomStatus(r.getRoomId(), RoomStatus.CLEANING);
            // Author: Brian Kam Ding Xian
            housekeepingController.createCheckoutTask(r.getRoomId());
        }

        reservationDAO.saveAssignedList(assignedList);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
        saveGuestList();

        // display final status table for the checked-out room(s)
        System.out.println("\nCheck-Out Summary:");
        reservationUI.printWaitingQueueTable(buildCheckOutSummaryTableData(toCheckOut));

        reservationUI.printSuccess();

        String[][] options = {
            {"1", "Continue with another check out"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1: checkOut(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }

    // case 8
    public void checkQueuePosition() {
        if (guestQueue.isEmpty()) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printWaitingQueueTable(buildQueueTableData(guestQueue));
        int selection = reservationUI.inputListIndex("reservation", guestQueue.size());
        if (selection == 0) return;

        Reservation found = guestQueue.get(selection - 1);
        reservationUI.printQueuePosition(found.getConfirmationNumber(), selection);
        reservationUI.pressEnterToContinue();

        String[][] options = {
            {"1", "Check another queue position"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1: checkQueuePosition(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }

    // case 9 
    public void cancelReservation() {
        LinkedListInterface<Reservation> candidates = new LinkedList<>();
        for (int i = 0; i < guestQueue.size(); i++) {
            candidates.addBack(guestQueue.get(i));
        }
        for (int i = 0; i < bookingList.size(); i++) {
            candidates.addBack(bookingList.get(i));
        }
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getStatus() == ReservationStatus.ASSIGNED) {
                candidates.addBack(r);
            }
        }

        if (candidates.isEmpty()) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printWaitingQueueTable(buildQueueTableData(candidates));
        int selection = reservationUI.inputListIndex("reservation", candidates.size());
        if (selection == 0) return;

        Reservation r = candidates.get(selection - 1);
        reservationUI.printReservationDetails(r);

        if (!reservationUI.askConfirmation(
                "Cancel this reservation?",
                "- Confirm cancellation",
                "- Keep reservation")) {
            reservationUI.pressEnterToContinue();
            return;
        }

        Guest guest = getGuestById(r.getGuestId());

        int queueIndex = guestQueue.indexOf(r);
        if (queueIndex >= 0) {
            guestQueue.removeIndex(queueIndex);
            if (guest != null) removeReservationFromGuest(guest, r);
            saveGuestList();

            reservationDAO.saveGuestQueue(guestQueue);
            reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            handleRefund(r);
            reservationUI.printCancelled();
            afterCancelSuccess();
            return;
        }

        int bookingIndex = bookingList.indexOf(r);
        if (bookingIndex >= 0) {
            bookingList.removeIndex(bookingIndex);
            if (guest != null) removeReservationFromGuest(guest, r);
            saveGuestList();

            reservationDAO.saveBookingList(bookingList);
            reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            handleRefund(r);
            reservationUI.printCancelled();
            afterCancelSuccess();
            return;
        }

        // must be the assignedList (ASSIGNED) case - can cancel, free up room
        int assignedIndex = assignedList.indexOf(r);
        if (assignedIndex >= 0) {
            assignedList.removeIndex(assignedIndex);
            if (guest != null) removeReservationFromGuest(guest, r);
            saveGuestList();

            Room room = getRoomById(r.getRoomId());
            if (room != null) {
                for (int j = 0; j < room.getReservations().size(); j++) {
                    if (room.getReservations().get(j).getReservationId().equals(r.getReservationId())) {
                        room.getReservations().removeIndex(j);
                        break;
                    }
                }
            }

            // updateRoomStatus() also persists the removal above
            updateRoomStatus(r.getRoomId(), RoomStatus.AVAILABLE);
            reservationDAO.saveAssignedList(assignedList);
            reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            reservationUI.printCancelled();
            afterCancelSuccess();
        }
    }

    // remove a reservation from the guest's own reservation history list
    private void removeReservationFromGuest(Guest guest, Reservation r) {
        for (int j = 0; j < guest.getReservations().size(); j++) {
            if (guest.getReservations().get(j).getReservationId().equals(r.getReservationId())) {
                guest.getReservations().removeIndex(j);
                break;
            }
        }
    }

    private void afterCancelSuccess() {
        String[][] options = {
            {"1", "Cancel another reservation"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1: cancelReservation(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }

    // ===== ROOM MANAGEMENT =====

    public void runRoomManagement() {
        RoomType typeFilter = null;
        RoomStatus statusFilter = null;
        int page = 0;

        while (true) {
            LinkedListInterface<Room> display;
            if (typeFilter != null) {
                display = getRoomsByType(typeFilter);
            } else if (statusFilter != null) {
                display = getRoomsByStatus(statusFilter);
            } else {
                display = roomList;
            }

            boolean hasFilter = typeFilter != null || statusFilter != null;
            int pageCount = Math.max(1, (display.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount) {
                page = pageCount - 1;
            }

            LinkedListInterface<Room> pageList = pageOfRooms(display, page);
            int choice = reservationUI.printRoomListMenu(pageList, page, pageCount, hasFilter);

            if (choice == 0) break;

            int action = 1;
            if (choice == action++) { // View Details
                viewRoom(pageList);
            } else if (choice == action++) { // Filter by Room Type
                int typeChoice = reservationUI.inputRoomTypeChoice();
                if (typeChoice != 0) {
                    typeFilter = intToRoomType(typeChoice);
                    statusFilter = null;
                    page = 0;
                }
            } else if (choice == action++) { // Filter by Room Status
                int statusChoice = reservationUI.inputRoomStatusChoice();
                if (statusChoice != 0) {
                    statusFilter = intToRoomStatus(statusChoice);
                    typeFilter = null;
                    page = 0;
                }
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
                        typeFilter = null;
                        statusFilter = null;
                        page = 0;
                    }
                }
            }
        }
    }

    private void viewRoom(LinkedListInterface<Room> pageList) {
        if (pageList.isEmpty()) {
            reservationUI.printNoRecords();
            reservationUI.pressEnterToContinue();
            return;
        }
        int num = reservationUI.inputListIndex("room", pageList.size());
        if (num == 0) return;
        Room room = pageList.get(num - 1);
        if (room != null) {
            reservationUI.printRoomDetails(room);
            reservationUI.pressEnterToContinue();
        }
    }

    // update room status
    public boolean updateRoomStatus(String roomId, RoomStatus roomStatus) {
        Room room = getRoomById(roomId);
        if (room == null) {
            return false;
        }

        room.setRoomStatus(roomStatus);
        roomDAO.saveToFile(roomList);

        return true;
    }

    public void saveRoomList() {
        roomDAO.saveToFile(roomList);
    }

    // ===== REPORTS =====

    // case 10 TODO: nationality of the guest & ?
    public void generateReport() {}

    // ===== HELPERS =====

    private LinkedListInterface<Guest> pageOfGuests(LinkedListInterface<Guest> source, int page) {
        LinkedListInterface<Guest> result = new LinkedList<>();
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, source.size());
        for (int i = startIndex; i < endIndex; i++) {
            result.addBack(source.get(i));
        }
        return result;
    }

    private LinkedListInterface<Room> pageOfRooms(LinkedListInterface<Room> source, int page) {
        LinkedListInterface<Room> result = new LinkedList<>();
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, source.size());
        for (int i = startIndex; i < endIndex; i++) {
            result.addBack(source.get(i));
        }
        return result;
    }

    public RoomType intToRoomType(int choice) {
        switch (choice) {
            case 1: return RoomType.STANDARD_SINGLE;
            case 2: return RoomType.STANDARD_DOUBLE;
            case 3: return RoomType.STANDARD_TRIPLE;
            case 4: return RoomType.DELUXE_SINGLE;
            case 5: return RoomType.DELUXE_DOUBLE;
            case 6: return RoomType.DELUXE_TRIPLE;
            case 7: return RoomType.SUITE;
            default: return null;
        }
    }

    private RoomStatus intToRoomStatus(int choice) {
        switch (choice) {
            case 1: return RoomStatus.AVAILABLE;
            case 2: return RoomStatus.OCCUPIED;
            case 3: return RoomStatus.CLEANING;
            default: return null;
        }
    }

    // count how many pending/active reservations of this room type overlap the in the given date range, across bookingList + guestQueue + assignedList.
    private int countOverlappingReservations(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        int count = 0;
        count += countOverlapInList(bookingList, roomType, checkIn, checkOut);
        count += countOverlapInList(guestQueue, roomType, checkIn, checkOut);
        count += countOverlapInList(assignedList, roomType, checkIn, checkOut);
        return count;
    }

    private int countOverlapInList(LinkedListInterface<Reservation> list, RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);

            if (r.getRoomTypeRequested() != roomType) continue;

            // a guest that has already checked out no longer occupies this date range
            if (r.getStatus() == ReservationStatus.CHECKED_OUT) continue;

            LocalDate rCheckIn = r.getTimestamps().getExpectedCheckInDate();
            LocalDate rCheckOut = r.getTimestamps().getExpectedCheckOutDate();

            boolean overlap = checkIn.isBefore(rCheckOut) && checkOut.isAfter(rCheckIn);
            if (overlap) {
                count++;
            }
        }
        return count;
    }

    // returns a new list containing the same reservations, ordered by expectedCheckInDate ascending order
    private LinkedListInterface<Reservation> sortByExpectedCheckIn(LinkedListInterface<Reservation> list) {
        LinkedListInterface<Reservation> sorted = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            int insertAt = sorted.size();
            for (int j = 0; j < sorted.size(); j++) {
                LocalDate existingDate = sorted.get(j).getTimestamps().getExpectedCheckInDate();
                if (r.getTimestamps().getExpectedCheckInDate().isBefore(existingDate)) {
                    insertAt = j;
                    break;
                }
            }
            sorted.addAtIndex(insertAt, r);
        }
        return sorted;
    }

    public String capitalizeName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return trimmed;
        String[] guestName = trimmed.split(" ");
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

    private void saveCustomNationalities() {
        String[] arr = new String[customNationalities.size()];
        for (int i = 0; i < customNationalities.size(); i++) {
            arr[i] = customNationalities.get(i);
        }
        nationalityDAO.saveCustomNationalities(arr);
    }

    private String[][] buildArrivalListTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[]{"No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-In"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getGuestId(),
                guestName != null ? guestName : "-",
                r.getRoomTypeRequested().toString(),
                r.getTimestamps().getExpectedCheckInDate().toString()
            };
        }
        return data;
    }

    private String[][] buildGuestSelectionTableData(LinkedListInterface<Guest> list) {
        String[][] data = new String[list.size() + 1][5];
        data[0] = new String[]{"No.", "Guest ID", "Name", "Nationality", "Contact"};
        for (int i = 0; i < list.size(); i++) {
            Guest g = list.get(i);
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                g.getGuestId(),
                g.getName(),
                g.getNationality(),
                g.getContactNumber()
            };
        }
        return data;
    }

    private String[][] buildCheckInCandidateTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[]{"No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-In"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getGuestId(),
                guestName != null ? guestName : "-",
                r.getRoomTypeRequested().toString(),
                r.getTimestamps().getExpectedCheckInDate().toString()
            };
        }
        return data;
    }

    private String[][] buildCheckOutCandidateTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[]{"No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-Out"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getGuestId(),
                guestName != null ? guestName : "-",
                r.getRoomTypeRequested().toString(),
                r.getTimestamps().getExpectedCheckOutDate().toString()
            };
        }
        return data;
    }

    private String[][] buildNoShowTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[]{"No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-In"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getGuestId(),
                guestName != null ? guestName : "-",
                r.getRoomTypeRequested().toString(),
                r.getTimestamps().getExpectedCheckInDate().toString()
            };
        }
        return data;
    }

    private String[][] buildAvailableRoomTableData(LinkedListInterface<Room> rooms) {
        String[][] data = new String[rooms.size() + 1][4];
        data[0] = new String[]{"No.", "Room ID", "Room Number", "Price/Night"};
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getRoomId(),
                r.getRoomNumber(),
                String.format("%.2f", r.getPricePerNight())
            };
        }
        return data;
    }

    // build table data from queue
    private String[][] buildQueueTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[]{"No.", "Conf. No.", "Guest ID", "Room Type", "Type", "Status"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getGuestId(),
                r.getRoomTypeRequested().toString(),
                r.getReservationType().toString(),
                r.getStatus().toString()
            };
        }
        return data;
    }

    private String[][] buildBookingSummaryTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[]{"No.", "Conf. No.", "Room Type", "Nights", "Check-In Date", "Status"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getRoomTypeRequested().toString(),
                String.valueOf(r.getNumberOfNights()),
                r.getTimestamps().getExpectedCheckInDate().toString(),
                r.getStatus().toString()
            };
        }
        return data;
    }

    private String[][] buildCheckOutSelectionTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][5];
        data[0] = new String[]{"No.", "Conf. No.", "Room Type", "Nights", "Check-In Date"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getRoomTypeRequested().toString(),
                String.valueOf(r.getNumberOfNights()),
                r.getTimestamps().getExpectedCheckInDate().toString()
            };
        }
        return data;
    }

    private String[][] buildCheckOutSummaryTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][8];
        data[0] = new String[]{"No.", "Conf. No.", "Room Type", "Status", "Check-In Date", "Check-Out Date", "Actual Check-In", "Actual Check-Out"};
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[]{
                String.valueOf(i + 1),
                r.getConfirmationNumber(),
                r.getRoomTypeRequested().toString(),
                r.getStatus().toString(),
                r.getTimestamps().getExpectedCheckInDate().toString(),
                r.getTimestamps().getExpectedCheckOutDate().toString(),
                reservationUI.formatDateTime(r.getTimestamps().getActualCheckInTime()),
                reservationUI.formatDateTime(r.getTimestamps().getActualCheckOutTime())
            };
        }
        return data;
    }

    // ===== ID GENERATORS =====

    // generate reservation id
    private String generateReservationId() {

        int max = 0;

        // check booking list
        for (int i = 0; i < bookingList.size(); i++) {
            String reservationId = bookingList.get(i).getReservationId();
            max = maxIdFrom(reservationId, max);
        }

        // check waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            String reservationId = guestQueue.get(i).getReservationId();
            max = maxIdFrom(reservationId, max);
        }

        // check assigned list
        for (int i = 0; i < assignedList.size(); i++) {
            String reservationId = assignedList.get(i).getReservationId();
            max = maxIdFrom(reservationId, max);
        }

        return String.format("RES%03d", max + 1);
    }

    // parse "RESxxx" suffix defensively - malformed ids are skipped, never crash
    private int maxIdFrom(String reservationId, int max) {
        if (reservationId != null && reservationId.startsWith("RES")) {
            try {
                int number = Integer.parseInt(reservationId.substring(3));
                if (number > max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    // generate confirmation number
    private String generateConfirmationNumber() {

        while (true) {

            String confirmationNumber = String.format("%08d", (int) (Math.random() * 100000000));
            boolean duplicate = false;

            // check if duplicated in booking list
            for (int i = 0; i < bookingList.size(); i++) {
                if (bookingList.get(i).getConfirmationNumber().equals(confirmationNumber)) {
                    duplicate = true;
                    break;
                }
            }

            // check if duplicated in waiting queue
            for (int i = 0; i < guestQueue.size(); i++) {
                if (guestQueue.get(i).getConfirmationNumber().equals(confirmationNumber)) {
                    duplicate = true;
                    break;
                }
            }

            // check if duplicated in assigned list
            if (!duplicate) {
                for (int i = 0; i < assignedList.size(); i++) {
                    if (assignedList.get(i).getConfirmationNumber().equals(confirmationNumber)) {
                        duplicate = true;
                        break;
                    }
                }
            }

            if (!duplicate) {
                return confirmationNumber;
            }
        }
    }

    // generate guest id
    public String generateGuestId() {

        int max = 0;

        for (int i = 0; i < guestList.size(); i++) {

            String guestId = guestList.get(i).getGuestId();

            if (guestId != null && guestId.startsWith("GST")) {
                try {
                    int number = Integer.parseInt(guestId.substring(3));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException ignored) {
                    // malformed id - skip it instead of crashing
                }
            }
        }

        return String.format("GST%03d", max + 1);
    }

    // ===== FINDERS =====

    // Guest
    public Guest getGuestById(String guestId) {

        for (int i = 0; i < guestList.size(); i++) {

            Guest guest = guestList.get(i);

            if (guest.getGuestId().equals(guestId)) {
                return guest;
            }
        }

        return null;
    }

    public Guest getGuestByIcOrPassport(String icOrPassport) {

        for (int i = 0; i < guestList.size(); i++) {

            Guest guest = guestList.get(i);

            if (guest.getIcOrPassport().equals(icOrPassport)) {
                return guest;
            }
        }

        return null;
    }

    public Guest getGuestByContactNumber(String contactNumber) {

        for (int i = 0; i < guestList.size(); i++) {

            Guest guest = guestList.get(i);

            if (guest.getContactNumber().equals(contactNumber)) {
                return guest;
            }
        }

        return null;
    }

    public LinkedListInterface<Guest> getAllGuests() {
        return guestList;
    }

    public String getGuestName(String guestId) {
        Guest guest = getGuestById(guestId);

        if (guest == null) {
            return null;
        }

        return guest.getName();
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

    public String[] getNationalityOptions() {
        String[] result = new String[DEFAULT_NATIONALITIES.length + customNationalities.size()];
        System.arraycopy(DEFAULT_NATIONALITIES, 0, result, 0, DEFAULT_NATIONALITIES.length);
        for (int i = 0; i < customNationalities.size(); i++) {
            result[DEFAULT_NATIONALITIES.length + i] = customNationalities.get(i);
        }
        return result;
    }

    // Reservation
    public LinkedList<Reservation> findReservationsByRoomType(RoomType roomType) {
        LinkedList<Reservation> reservationList = new LinkedList<>();
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getRoomTypeRequested() == roomType) {
                reservationList.addBack(reservation);
            }
        }
        return reservationList;
    }

    public Reservation findReservationByConfirmationNumber(String confirmationNumber) {
        // search waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
                return reservation;
            }
        }

        // search assigned list
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
                return reservation;
            }
        }

        return null;
    }

    public Reservation findReservationByReservationId(String reservationId) {

        // search waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getReservationId().equals(reservationId)) {
                return reservation;
            }
        }

        // search assigned list
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getReservationId().equals(reservationId)) {
                return reservation;
            }
        }

        return null;
    }

    public Reservation findReservationByRoomId(String roomId) {

        for (int i = 0; i < assignedList.size(); i++) {

            Reservation reservation = assignedList.get(i);

            if (reservation.getRoomId() != null
                    && reservation.getRoomId().equals(roomId)) {

                return reservation;
            }
        }

        return null;
    }

    public LinkedListInterface<Reservation> findCheckedInReservations() {
        LinkedListInterface<Reservation> reservationList = new LinkedList<>();

        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                reservationList.addBack(reservation);
            }
        }

        return reservationList;
    }

    public LinkedListInterface<Reservation> findAssignedReservations() {
        LinkedListInterface<Reservation> reservationList = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getStatus() == ReservationStatus.ASSIGNED) {
                reservationList.addBack(reservation);
            }
        }

        return reservationList;
    }

    public LinkedListInterface<Reservation> findCheckedOutReservations() {

        LinkedListInterface<Reservation> reservationList = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
                reservationList.addBack(reservation);
            }
        }

        return reservationList;
    }

    // Room
    public Room getRoomById(String roomId) {

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }

        return null;
    }

    // get the available room by the room type
    public Room getAvailableRoom(RoomType roomType) {
        LinkedListInterface<Room> roomsOfType = getRoomsByType(roomType);
        for (int i = 0; i < roomsOfType.size(); i++) {
            Room room = roomsOfType.get(i);
            if (room.getRoomStatus() == RoomStatus.AVAILABLE) {
                return room;
            }
        }
        return null;
    }

    // get all AVAILABLE rooms of a given room type
    public LinkedListInterface<Room> getAvailableRoomsByType(RoomType roomType) {
        LinkedListInterface<Room> result = new LinkedList<>();
        LinkedListInterface<Room> roomsOfType = getRoomsByType(roomType);
        for (int i = 0; i < roomsOfType.size(); i++) {
            Room room = roomsOfType.get(i);
            if (room.getRoomStatus() == RoomStatus.AVAILABLE) {
                result.addBack(room);
            }
        }
        return result;
    }

    // count how many physical rooms exist for a given room type
    public int countRoomsByType(RoomType roomType) {
        return getRoomsByType(roomType).size();
    }

    // get all available rooms
    public LinkedListInterface<Room> getAvailableRooms() {

        LinkedListInterface<Room> availableRooms = new LinkedList<>();

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomStatus() == RoomStatus.AVAILABLE) {

                availableRooms.addBack(room);
            }
        }

        return availableRooms;
    }

    // get all occupied rooms
    public LinkedListInterface<Room> getOccupiedRooms() {

        LinkedListInterface<Room> occupiedRooms = new LinkedList<>();

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomStatus() == RoomStatus.OCCUPIED) {

                occupiedRooms.addBack(room);
            }
        }

        return occupiedRooms;
    }

    public LinkedListInterface<Room> getRoomsByStatus(RoomStatus status) {
        LinkedListInterface<Room> result = new LinkedList<>();
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomStatus() == status) {
                result.addBack(roomList.get(i));
            }
        }
        return result;
    }

    // get rooms by room type
    public LinkedListInterface<Room> getRoomsByType(RoomType roomType) {

        LinkedListInterface<Room> roomTypeList = new LinkedList<>();

        for (int i = 0; i < roomList.size(); i++) {

            Room room = roomList.get(i);

            if (room.getRoomType() == roomType) {
                roomTypeList.addBack(room);
            }
        }

        return roomTypeList;
    }

    // get all the rooms
    public LinkedListInterface<Room> getAllRooms() {
        return roomList;
    }

    public RoomStatus getRoomStatus(String roomId) {
        Room room = getRoomById(roomId);
        if (room == null) {
            return null;
        }
        return room.getRoomStatus();
    }

    public RoomType getRoomType(String roomId) {
        Room room = getRoomById(roomId);
        if (room == null) {
            return null;
        }
        return room.getRoomType();
    }

    public double getRoomPrice(String roomId) {
        Room room = getRoomById(roomId);

        if (room == null) {
            return -1;
        }

        return room.getPricePerNight();
    }

    public double getPriceByRoomType(RoomType roomType) {
        LinkedListInterface<Room> roomsOfType = getRoomsByType(roomType);
        if (roomsOfType.isEmpty()) {
            return -1;
        }
        return roomsOfType.get(0).getPricePerNight();
    }

    // ===== VALIDATION =====

    private String inputValidIc() {
        String value = "";
        while (true) {
            value = reservationUI.inputIc();  // call UI to get input
            if (value == null) continue;
            String trimmed = value.trim();

            if (trimmed.length() != 12 || !trimmed.chars().allMatch(Character::isDigit)) {
                reservationUI.printInvalidInput("Invalid IC format! Must be 12 digits, no dashes (e.g. 060322140562)");
                continue;
            }

            String digits = trimmed;

            int mm = Integer.parseInt(digits.substring(2, 4));
            int dd = Integer.parseInt(digits.substring(4, 6));
            String bp = digits.substring(6, 8);

            if (mm < 1 || mm > 12) {
                reservationUI.printInvalidInput("Invalid IC format!");
                continue;
            }

            int[] daysInMonth = {31,29,31,30,31,30,31,31,30,31,30,31};
            if (dd < 1 || dd > daysInMonth[mm - 1]) {
                reservationUI.printInvalidInput("Invalid IC format!");
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
                reservationUI.printInvalidInput("Invalid IC format!");
                continue;
            }

            return value;
        }
    }

    private String inputValidPassport() {
        String value = "";
        while (true) {
            value = reservationUI.inputPassport();  // call UI to get input
            if (value == null) continue;
            String trimmed = value.trim();

            if (trimmed.length() < 6 || trimmed.length() > 9) {
                reservationUI.printInvalidInput("Invalid passport format!");
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
                reservationUI.printInvalidInput("Invalid passport format!");
                continue;
            }

            return value;
        }
    }

    private LocalDate inputValidDate(String prompt, LocalDate minDate) {
        LocalDate date = null;
        while (date == null) {
            String input = reservationUI.inputDate(prompt + " (0 = cancel)");
            if (input.equals("0")) {
                return null;
            }
            try {
                date = LocalDate.parse(input);
                if (date.isBefore(minDate)) {
                    reservationUI.printError("Date must be on or after " + minDate + "!");
                    date = null;
                }
            } catch (DateTimeParseException e) {
                reservationUI.printError("Invalid date format! Use YYYY-MM-DD.");
            }
        }
        return date;
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

    public boolean reservationExists(String confirmationNumber) {
        return findReservationByConfirmationNumber(confirmationNumber) != null;
    }

    public boolean roomExists(String roomId) {
        return getRoomById(roomId) != null;
    }

    public boolean isRoomAvailable(String roomId) {
        Room room = getRoomById(roomId);
        return room != null && room.getRoomStatus() == RoomStatus.AVAILABLE;
    }

    private void handleRefund(Reservation r) {
        double refund = paymentControl.refundReservation(r, this);
        if (refund > 0) {
            System.out.println("Refunded: RM " + String.format("%.2f", refund));
            reservationUI.printSuccess();
        } else if (paymentControl.hasPaymentFor(r.getConfirmationNumber())) {
            reservationUI.printError("Cancelled within 24 hours of check-in - no refund applies.");
        }
    }

    // ------------------------------------------------------------------
    // PaymentControl (moved inside ReservationControl as a nested class)
    // ------------------------------------------------------------------
    private static class PaymentControl {

        private final LinkedListInterface<Payment> paymentList = new LinkedList<>();

        // DAO
        private final PaymentDAO paymentDAO = new PaymentDAO();

        public PaymentControl() {
            paymentDAO.loadFromFile(paymentList);
        }

        // called from bookRoom() - one combined payment for the whole booking session
        public Payment processBookingPayment(LinkedListInterface<Reservation> reservations,
                ReservationControl reservationControl, Member member, LoyaltyController loyaltyController) {
            if (reservations == null || reservations.size() == 0) {
                return null;
            }

            double totalRoomCharge = 0;
            for (int i = 0; i < reservations.size(); i++) {
                Reservation r = reservations.get(i);
                double pricePerNight = reservationControl.getPriceByRoomType(r.getRoomTypeRequested());
                totalRoomCharge += pricePerNight * r.getNumberOfNights();
            }

            // ---- vouchers: each voucher offsets its own room type's subtotal (capped);
            // generic vouchers (null room type) offset the remaining session charge.
            LinkedListInterface<RedemptionRecord> appliedVouchers = new LinkedList<>();
            LinkedListInterface<Double> appliedDeductions = new LinkedList<>();
            if (member != null) {
                double[] poolByType = new double[RoomType.values().length];
                for (int i = 0; i < reservations.size(); i++) {
                    Reservation r = reservations.get(i);
                    RoomType rt = r.getRoomTypeRequested();
                    if (rt != null) {
                        poolByType[rt.ordinal()]
                                += reservationControl.getPriceByRoomType(rt) * r.getNumberOfNights();
                    }
                }
                double genericPool = totalRoomCharge;

                while (true) {
                    LinkedListInterface<RedemptionRecord> available =
                            loyaltyController.getAvailableVouchers(member.getMemberId());
                    LinkedListInterface<RedemptionRecord> applicable = new LinkedList<>();
                    for (int i = 0; i < available.size(); i++) {
                        RedemptionRecord v = available.get(i);
                        if (isVoucherApplied(appliedVouchers, v.getRedemptionId())) {
                            continue;
                        }
                        double pool = v.getRoomType() == null
                                ? genericPool
                                : poolByType[v.getRoomType().ordinal()];
                        boolean isVoucher = (v.getVoucherValue() != null && v.getVoucherValue() > 0)
                                || (v.getDiscountPercent() != null && v.getDiscountPercent() > 0);
                        if (pool > 0.005 && isVoucher) {
                            applicable.addBack(v);
                        }
                    }
                    if (applicable.isEmpty()) {
                        break;
                    }

                    String redemptionId = reservationControl.getReservationUI().selectVoucher(applicable);
                    if (redemptionId == null) {
                        break; // staff chose 0 - no more vouchers
                    }
                    RedemptionRecord chosen = findVoucher(applicable, redemptionId);
                    if (chosen == null) {
                        break;
                    }
                    double pool = chosen.getRoomType() == null
                            ? genericPool
                            : poolByType[chosen.getRoomType().ordinal()];
                    // fixed-RM vouchers deduct their value (capped at the pool);
                    // percentage vouchers deduct percent% of the room-type pool
                    double deduction;
                    if (chosen.getDiscountPercent() != null) {
                        deduction = pool * chosen.getDiscountPercent() / 100.0;
                    } else {
                        deduction = Math.min(chosen.getVoucherValue(), pool);
                    }
                    if (deduction <= 0.005) {
                        break;
                    }
                    if (chosen.getRoomType() == null) {
                        genericPool -= deduction;
                    } else {
                        poolByType[chosen.getRoomType().ordinal()] -= deduction;
                    }
                    appliedVouchers.addBack(chosen);
                    appliedDeductions.addBack(deduction);
                }
            }

            double voucherTotal = 0;
            String[] voucherLabels = new String[appliedVouchers.size()];
            double[] voucherValues = new double[appliedVouchers.size()];
            for (int i = 0; i < appliedVouchers.size(); i++) {
                RedemptionRecord v = appliedVouchers.get(i);
                if (v.getDiscountPercent() != null) {
                    String room = v.getRoomType() == null ? "Any Room" : v.getRoomType().name();
                    voucherLabels[i] = "Voucher Applied (" + v.getDiscountPercent() + "% OFF " + room + ")";
                } else if (v.getRoomType() == null) {
                    voucherLabels[i] = "Voucher Applied";
                } else {
                    voucherLabels[i] = "Voucher Applied (" + v.getRoomType() + ")";
                }
                voucherValues[i] = appliedDeductions.get(i);
                voucherTotal += appliedDeductions.get(i);
            }
            double chargeAfterVouchers = Math.max(0.0, totalRoomCharge - voucherTotal);

            // member tier discount (after vouchers, before SC & tax)
            int discountPercent = member == null ? 0 : member.getTier().getDiscountPercent();
            double discount = chargeAfterVouchers * discountPercent / 100.0;
            double netRoomCharge = chargeAfterVouchers - discount;

            double serviceCharge = netRoomCharge * 0.10;
            double tax = (netRoomCharge + serviceCharge) * 0.06;
            double total = netRoomCharge + serviceCharge + tax;

            reservationControl.getReservationUI().printBill(totalRoomCharge, discountPercent, discount,
                    voucherLabels, voucherValues, serviceCharge, tax, 0.0, total);

            PaymentMethod method = askPaymentMethod(reservationControl.getReservationUI());
            if (method == null) {
                return null; // guest cancelled payment
            }

            Payment payment = new Payment(
                generatePaymentId(),
                netRoomCharge,
                serviceCharge,
                tax,
                total,
                method,
                PaymentStatus.PAID,
                LocalDateTime.now(),
                reservations.get(0).getConfirmationNumber()
            );

            for (int i = 0; i < reservations.size(); i++) {
                payment.addConfirmationNumber(reservations.get(i).getConfirmationNumber());
            }

            // vouchers are only consumed once the payment is recorded
            for (int i = 0; i < appliedVouchers.size(); i++) {
                loyaltyController.useVoucher(member.getMemberId(),
                        appliedVouchers.get(i).getRedemptionId());
            }

            paymentList.addBack(payment);
            paymentDAO.saveToFile(paymentList);

            // loyalty: 1 point per RM1 of the total bill paid (incl. SC & tax)
            int pointsEarned = (int) Math.round(total);
            if (member != null && pointsEarned > 0) {
                System.out.println(loyaltyController.earnPoints(member.getMemberId(), pointsEarned,
                        "Booking " + payment.getPaymentID(), LocalDateTime.now()));
            }
            return payment;
        }

        private boolean isVoucherApplied(LinkedListInterface<RedemptionRecord> applied, String redemptionId) {
            for (int i = 0; i < applied.size(); i++) {
                if (applied.get(i).getRedemptionId().equals(redemptionId)) {
                    return true;
                }
            }
            return false;
        }

        private RedemptionRecord findVoucher(LinkedListInterface<RedemptionRecord> list, String redemptionId) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getRedemptionId().equals(redemptionId)) {
                    return list.get(i);
                }
            }
            return null;
        }

        // refund policy: 100% refund if cancelled at least 24 hours before the
        // 12pm check-in moment; 0% refund if cancelled within the last 24 hours.
        // Called from cancelReservation() after the reservation is removed.
        public double refundReservation(Reservation r, ReservationControl reservationControl) {
            if (r == null || r.getTimestamps() == null || r.getTimestamps().getExpectedCheckInDate() == null) {
                return 0.0;
            }

            LocalDateTime checkInMoment = r.getTimestamps().getExpectedCheckInDate().atTime(12, 0);
            if (!LocalDateTime.now().isBefore(checkInMoment.minusHours(24))) {
                return 0.0; // cancelled within 24 hours of check-in - no refund
            }

            Payment payment = findPaymentByConfirmationNumber(r.getConfirmationNumber());
            if (payment == null) {
                return 0.0;
            }

            // recompute this room's share of the bill with the same formula used at booking
            double roomCharge = reservationControl.getPriceByRoomType(r.getRoomTypeRequested()) * r.getNumberOfNights();
            double serviceCharge = roomCharge * 0.10;
            double tax = (roomCharge + serviceCharge) * 0.06;
            double share = roomCharge + serviceCharge + tax;

            double remaining = payment.getTotalAmount() - payment.getRefundedAmount();
            if (remaining < 0.005) {
                return 0.0; // already fully refunded (within rounding tolerance)
            }
            double refund = Math.min(share, remaining);
            if (refund < 0.005) {
                return 0.0;
            }

            payment.setRefundedAmount(payment.getRefundedAmount() + refund);
            payment.setRefundDateTime(LocalDateTime.now());
            paymentDAO.saveToFile(paymentList);

            // claw back the loyalty points earned for this refunded share of the bill
            if (refund >= 0.005) {
                Member member = reservationControl.loyaltyController
                        .findMemberByGuestId(r.getGuestId());
                if (member != null) {
                    int pts = (int) Math.round(refund);
                    if (pts > 0) {
                        System.out.println(reservationControl.loyaltyController.deductPoints(
                                member.getMemberId(), pts,
                                "Refund of booking " + r.getConfirmationNumber(),
                                LocalDateTime.now()));
                    }
                }
            }
            return refund;
        }

        // true if any payment record covers this confirmation number
        public boolean hasPaymentFor(String confirmationNumber) {
            return findPaymentByConfirmationNumber(confirmationNumber) != null;
        }

        private Payment findPaymentByConfirmationNumber(String confirmationNumber) {
            for (int i = 0; i < paymentList.size(); i++) {
                Payment p = paymentList.get(i);
                if (confirmationNumber.equals(p.getReservationID())) {
                    return p;
                }
                LinkedListInterface<String> numbers = p.getConfirmationNumbers();
                if (numbers != null) {
                    for (int j = 0; j < numbers.size(); j++) {
                        if (confirmationNumber.equals(numbers.get(j))) {
                            return p;
                        }
                    }
                }
            }
            return null;
        }

        public void displayPaymentRecords(ReservationUI ui) {
            ui.printPaymentRecords(paymentList);
        }

        public LinkedListInterface<Payment> getPaymentList() {
            return paymentList;
        }

        public PaymentMethod askPaymentMethod(ReservationUI reservationUI) {
            String[][] methodOptions = {
                {"1", "Cash"},
                {"2", "Credit Card"},
                {"3", "Debit Card"},
                {"4", "E-Wallet"},
                {"5", "Online Banking"},
                {"0", "Cancel"}
            };
            int methodChoice = reservationUI.showSubMenu("Select Payment Method:", methodOptions);
            switch (methodChoice) {
                case 1: return PaymentMethod.CASH;
                case 2: return PaymentMethod.CREDIT_CARD;
                case 3: return PaymentMethod.DEBIT_CARD;
                case 4: return PaymentMethod.E_WALLET;
                case 5: return PaymentMethod.ONLINE_BANKING;
                default: return null;
            }
        }

        // scan existing payments so IDs never collide across app restarts
        private String generatePaymentId() {
            int max = 0;
            for (int i = 0; i < paymentList.size(); i++) {
                String id = paymentList.get(i).getPaymentID();
                if (id != null && id.startsWith("PAY")) {
                    try {
                        max = Math.max(max, Integer.parseInt(id.substring(3)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return String.format("PAY%03d", max + 1);
        }
    }

}
