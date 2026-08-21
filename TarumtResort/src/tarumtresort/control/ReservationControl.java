package tarumtresort.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

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
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.ReservationTimestamps;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.PaymentMethod;
import tarumtresort.entity.enums.PaymentStatus;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.ReservationType;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.entity.enums.RoomType;
// import tarumtresort.report.NationalityReport;
import tarumtresort.report.ReportResult;
//import tarumtresort.report.ReportUI;
// import tarumtresort.report.RoomTypeReport;

public class ReservationControl {

    // default nationality options offered when registering a guest
    private static final String[] DEFAULT_NATIONALITIES = {
            "Malaysian", "Singaporean", "Indonesian", "Chinese", "Indian", "Thai", "Korean", "Japanese", "American",
            "British", "Saudi Arabian"
    };

    // Reservation views
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
    private LinkedListInterface<Reservation> reservations = new LinkedList<>();
    private LinkedListInterface<Guest> guestList = new LinkedList<>();
    private LinkedListInterface<String> customNationalities = new LinkedList<>();
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
        reservationDAO.loadAllReservations(reservations);

        guestDAO.loadFromFile(guestList);

        String[] loaded = nationalityDAO.loadCustomNationalities();
        for (String n : loaded) {
            customNationalities.addBack(n);
        }

        roomDAO.loadFromFile(roomList);

        relinkReservationReferences();
    }

    // list declaration
    public LinkedListInterface<Reservation> getReservations() {
        return reservations;
    }

    public LinkedListInterface<Reservation> getBookingList() {
        LinkedListInterface<Reservation> result = new LinkedList<>();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (r.isDeleted() || r.getStatus() != ReservationStatus.BOOKED)
                continue;
            result.addSorted(r);
        }
        return result;
    }

    public LinkedListInterface<Reservation> getGuestQueue() {
        LinkedListInterface<Reservation> result = new LinkedList<>();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (r.isDeleted() || r.getStatus() != ReservationStatus.WAITING)
                continue;
            if (isVipReservation(r))
                continue;
            result.addSorted(r);
        }
        return result;
    }

    public LinkedListInterface<Reservation> getVipList() {
        LinkedListInterface<Reservation> result = new LinkedList<>();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (r.isDeleted() || r.getStatus() != ReservationStatus.WAITING)
                continue;
            if (isVipReservation(r)) {
                result.addSorted(r);
            }
        }
        return result;
    }

    public LinkedListInterface<Reservation> getAssignedList() {
        LinkedListInterface<Reservation> result = new LinkedList<>();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (r.isDeleted())
                continue;
            ReservationStatus s = r.getStatus();
            if (s == ReservationStatus.ASSIGNED || s == ReservationStatus.CHECKED_IN
                    || s == ReservationStatus.CHECKED_OUT) {
                result.addBack(r);
            }
        }
        return result;
    }

    private boolean isVipReservation(Reservation r) {
        PriorityReservation pr = priorityReservationController.searchPriorityReservationById(r.getReservationId());
        return pr != null && !pr.isDeleted();
    }

    private void relinkReservationReferences() {
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);

            Guest guest = getGuestById(r.getGuestId());
            if (guest != null) {
                guest.getReservations().addBack(r);
            }

            Room room = getRoomById(r.getRoomId());
            if (room != null) {
                room.getReservations().addBack(r);
            }
        }
    }

    // ===== ENTRY POINT =====

    public void runReservationModule() {
        int choice = 0;
        do {
            choice = reservationUI.getMenuChoice();
            switch (choice) {
                case 0:
                    break;
                case 1:
                    runGuestManagement();
                    break;
                case 2:
                    runReservationManagement();
                    break;
                case 3:
                    runRoomManagement();
                    break;
                case 4: // generateReport();
                    break;
                default:
                    break;
            }
            saveAll();
        } while (choice != 0);
    }

    private void saveAll() {
        reservationDAO.saveAllReservations(reservations);
        saveGuestList();
        saveRoomList();
        saveCustomNationalities();
    }

    public static void main(String[] args) {
        ReservationControl reservationControl = new ReservationControl();
        reservationControl.runReservationModule();
    }

    // ===== GUEST MANAGEMENT =====

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
            int choice = reservationUI.printGuestListMenu(pageList, page, pageCount, hasFilter, this::memberIdOf, this::unreadOf);

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
                    if (matched)
                        page++;
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched)
                        page--;
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

    private void handleGuestActions(Guest guest) {
        while (true) {
            reservationUI.printGuestDetails(guest, membershipOf(guest.getGuestId()),
                    loyaltyController.getUnreadNotificationCountByGuest(guest.getGuestId()));

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
                case 2: // View Notifications (viewing marks them all as read)
                    reservationUI.printGuestNotifications(
                            loyaltyController.getNotifications(guest.getGuestId()));
                    int marked = loyaltyController.markGuestNotificationsRead(guest.getGuestId());
                    if (marked > 0) {
                        System.out.println("  " + marked + " notification(s) marked as read.");
                    }
                    reservationUI.pressEnterToContinue();
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

    // unread notification count shown in the guest list "Notifs" column
    private Integer unreadOf(String guestId) {
        return loyaltyController.getUnreadNotificationCountByGuest(guestId);
    }

    // membership summary shown in guest details ("-" when not a member)
    private String membershipOf(String guestId) {
        Member m = loyaltyController.findMemberByGuestId(guestId);
        return m == null ? "-" : m.getMemberId() + " (" + m.getTier() + ") - " + m.getPoints() + " pts";
    }

    // case 1: register a new guest - continue with menu/ room booking
    public Guest createGuest() {
        String name = capitalizeName(reservationUI.inputName());
        if (name.equals("0"))
            return null;

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
        reservationUI.printGuestDetails(guest, "-", 0);
        reservationUI.printSuccess();
        reservationUI.pressEnterToContinue();
        return guest;
    }

    public void addNationalityIfNew(String nationality) {
        for (String d : DEFAULT_NATIONALITIES) {
            if (d.equalsIgnoreCase(nationality))
                return;
        }
        for (int i = 0; i < customNationalities.size(); i++) {
            if (customNationalities.get(i).equalsIgnoreCase(nationality))
                return;
        }
        customNationalities.addBack(nationality);
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
                    sourceList = getBookingList();
                    currentListName = "Booking List";
                    break;
                case VIEW_ASSIGNED_LIST:
                    sourceList = getAssignedList();
                    currentListName = "Assigned List";
                    break;
                default:
                    sourceList = getGuestQueue();
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
            int choice = reservationUI.printReservationListMenu(currentListName, buildQueueTableData(displayList),
                    hasFilter);

            if (hasFilter && choice == 13) {
                roomTypeFilter = null;
                continue;
            }

            switch (choice) {
                case 0:
                    return;
                case 1:
                    currentView = VIEW_BOOKING_LIST;
                    roomTypeFilter = null;
                    break;
                case 2:
                    currentView = VIEW_GUEST_QUEUE;
                    roomTypeFilter = null;
                    break;
                case 3:
                    currentView = VIEW_ASSIGNED_LIST;
                    roomTypeFilter = null;
                    break;
                case 4: {
                    int roomChoice = reservationUI.inputRoomTypeChoice();
                    if (roomChoice != 0) {
                        roomTypeFilter = intToRoomType(roomChoice);
                    }
                    break;
                }
                case 5:
                    bookRoom();
                    break;
                case 6:
                    guestArrival();
                    break;
                case 7:
                    assignRoom();
                    break;
                case 8:
                    checkIn();
                    break;
                case 9:
                    checkOut();
                    break;
                case 10:
                    checkQueuePosition();
                    break;
                case 11:
                    cancelReservation();
                    break;
                case 12:
                    deleteReservation();
                    break;
                default:
                    break;
            }
        }
    }

    // business rule: guests still CHECKED_IN past FORCE_CHECKOUT_HOUR on their expectedCheckOutDate are forcibly checked out so the room can be freed up for the guest queue
    private void forceCheckoutOverdueReservations() {
        LocalDateTime now = LocalDateTime.now();
        LinkedListInterface<Reservation> forcedOut = new LinkedList<>();
        LinkedListInterface<Reservation> assignedList = getAssignedList();

        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getStatus() != ReservationStatus.CHECKED_IN)
                continue;

            LocalDateTime deadline = r.getTimestamps().getExpectedCheckOutDate().atTime(FORCE_CHECKOUT_HOUR, 0);
            if (now.isAfter(deadline)) {
                r.setStatus(ReservationStatus.CHECKED_OUT);
                r.getTimestamps().setActualCheckOutTime(now);
                updateRoomStatus(r.getRoomId(), RoomStatus.CLEANING);
                forcedOut.addBack(r);
            }
        }

        if (forcedOut.size() > 0) {
            System.out.println("\n" + forcedOut.size() + " guest(s) were automatically checked out for exceeding the "
                    + FORCE_CHECKOUT_HOUR + ":00 checkout deadline.");
            reservationUI.printWaitingQueueTable(buildCheckOutSummaryTableData(forcedOut));
            reservationUI.pressEnterToContinue();
        }
    }

    // no-show: BOOKED reservation past its check-in date with no arrival gets
    // auto-cancelled
    private void detectNoShowReservations() {
        LocalDate today = LocalDate.now();
        LinkedListInterface<Reservation> noShows = new LinkedList<>();
        LinkedListInterface<Reservation> bookingList = getBookingList();

        for (int i = bookingList.size() - 1; i >= 0; i--) {
            Reservation r = bookingList.get(i);
            if (r.getStatus() != ReservationStatus.BOOKED)
                continue;

            if (today.isAfter(r.getTimestamps().getExpectedCheckInDate())) {
                r.setStatus(ReservationStatus.CANCELLED);

                Guest guest = getGuestById(r.getGuestId());
                if (guest != null) {
                    removeReservationFromGuest(guest, r);
                }

                handleRefund(r);
                noShows.addBack(r);
            }
        }

        if (noShows.size() > 0) {
            System.out.println("\n" + noShows.size()
                    + " advance booking(s) auto-cancelled as no-show (guest never arrived on the expected check-in date).");
            reservationUI.printWaitingQueueTable(buildNoShowTableData(noShows));
            reservationUI.pressEnterToContinue();
        }
    }

    // case 1
    public void registerGuest() {
        Guest guest = createGuest();
        if (guest == null)
            return;

        String[][] options = {
                { "1", "Book a room" },
                { "2", "Continue another guest registration" },
                { "0", "Back to menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                bookRoom(guest.getGuestId());
                break;
            case 2:
                registerGuest();
                break;
            case 0:
            default:
                break;
        }
    }

    // case 2
    public void bookRoom() {
        Guest guest = selectGuestForBooking();
        if (guest == null)
            return;
        bookRoom(guest.getGuestId());
    }

    private Guest selectGuestForBooking() {
        int page = 0;
        while (true) {
            int pageCount = Math.max(1, (guestList.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page >= pageCount)
                page = pageCount - 1;
            LinkedListInterface<Guest> pageList = pageOfGuests(guestList, page);
            int choice = reservationUI.printGuestSelectionMenu(buildGuestSelectionTableData(pageList), page, pageCount);
            if (choice == 0)
                return null;
            int action = pageList.size();
            if (choice <= action) {
                return pageList.get(choice - 1);
            }
            if (page < pageCount - 1) {
                action++;
                if (choice == action) {
                    page++;
                    continue;
                }
            }
            if (page > 0) {
                action++;
                if (choice == action) {
                    page--;
                    continue;
                }
            }
            action++;
            if (choice == action) { // Register New Guest
                Guest newGuest = createGuest();
                if (newGuest != null)
                    return newGuest;
            }
        }
    }

    public void bookRoom(String guestId) {
        System.out.println();
        int typeChoice = reservationUI.printBookingTypeMenu();
        if (typeChoice == 0)
            return;
        ReservationType reservationType = typeChoice == 1
                ? ReservationType.WALK_IN
                : ReservationType.ADVANCE_BOOKING;

        int numberOfNights = reservationUI.inputNumberOfNights();

        LocalDate expectedCheckInDate;
        if (reservationType == ReservationType.WALK_IN) {
            expectedCheckInDate = LocalDate.now();
        } else {
            expectedCheckInDate = inputValidDate("Expected check-in date (must be at least 2 days from today)",
                    LocalDate.now().plusDays(2));
            if (expectedCheckInDate == null) {
                return;
            }
        }
        LocalDate expectedCheckOutDate = expectedCheckInDate.plusDays(numberOfNights);

        LinkedListInterface<Reservation> sessionBookings = new LinkedList<>();
        boolean continueBooking = true;

        while (continueBooking) {
            int roomChoice = reservationUI.inputRoomTypeChoice();
            if (roomChoice == 0)
                break;
            RoomType roomType = intToRoomType(roomChoice);
            if (roomType == null)
                break;

            int totalRoomsOfType = countRoomsByType(roomType);
            int overlappingReservations = countOverlappingReservations(roomType, expectedCheckInDate,
                    expectedCheckOutDate);
            if (overlappingReservations >= totalRoomsOfType) {
                reservationUI.printRoomNotAvailable();
                reservationUI.pressEnterToContinue();
                continue;
            }

            int numberOfGuests = reservationUI.inputNumberOfGuests();

            ReservationTimestamps timestamps = new ReservationTimestamps(
                    LocalDateTime.now(),
                    expectedCheckInDate,
                    expectedCheckOutDate);

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
                    timestamps,
                    false);

            if (reservationType == ReservationType.WALK_IN) {
                priorityReservationController.addPriorityReservation(
                        reservation.getReservationId(), reservation.getGuestId());
            } else {
                reservation.setStatus(ReservationStatus.BOOKED);
            }

            reservations.addBack(reservation);

            Guest guest = getGuestById(guestId);
            if (guest != null) {
                guest.getReservations().addBack(reservation);
            }

            System.out.println();
            reservationUI.printSuccess();
            reservationUI.printReservationDetails(reservation);

            sessionBookings.addBack(reservation);

            continueBooking = reservationUI.askConfirmation(
                    "Book another room for this guest?",
                    "Add another room",
                    "Done booking");
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
                { "1", "Book room for another guest" },
                { "0", "Back to menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                bookRoom();
                break;
            case 0:
            default:
                break;
        }
    }

    // case 3
    public void guestArrival() {
        LinkedListInterface<Reservation> bookingList = getBookingList();
        if (bookingList.isEmpty()) {
            reservationUI.printError("No advance bookings found.");
            reservationUI.pressEnterToContinue();
            return;
        }

        LinkedListInterface<Reservation> sortedBookings = sortByExpectedCheckIn(bookingList);
        reservationUI.printBookingListForArrival(buildArrivalListTableData(sortedBookings));

        int selection = reservationUI.inputListIndex("booking", sortedBookings.size());
        if (selection == 0) {
            return;
        }

        Reservation found = sortedBookings.get(selection - 1);

        LocalDate expectedCheckInDate = found.getTimestamps().getExpectedCheckInDate();
        if (!expectedCheckInDate.equals(LocalDate.now())) {
            reservationUI.printError("Not yet the expected check-in date! Expected: " + expectedCheckInDate);
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printReservationDetails(found);

        if (!reservationUI.askConfirmation("Confirm guest arrival?", "Guest will be moved to queue",
                "Cancel arrival")) {
            reservationUI.pressEnterToContinue();
            return;
        }

        found.setStatus(ReservationStatus.WAITING);
        found.getTimestamps().setRegistrationTimestamp(LocalDateTime.now());

        priorityReservationController.addPriorityReservation(
                found.getReservationId(), found.getGuestId());

        reservationUI.printSuccess();

        reservationUI.printWaitingQueueTable(buildQueueTableData(getGuestQueue()));

        String[][] options = {
                { "1", "Continue with another guest arrival" },
                { "0", "Back to menu" }
        };

        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                guestArrival();
                break;
            case 0:
            default:
                break;
        }
    }

    // case 4
    public void assignRoom() {
        int roomChoice = reservationUI.inputRoomTypeChoice();
        if (roomChoice == 0)
            return;

        RoomType roomType = intToRoomType(roomChoice);
        if (roomType == null)
            return;

        LinkedListInterface<Room> availableRooms = getAvailableRoomsByType(roomType);
        if (availableRooms.isEmpty()) {
            reservationUI.printRoomNotAvailable();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printAvailableRoomList(buildAvailableRoomTableData(availableRooms));
        int roomSelection = reservationUI.inputListIndex("room", availableRooms.size());
        if (roomSelection == 0)
            return;

        Room availableRoom = availableRooms.get(roomSelection - 1);

        Reservation found = null;
        boolean fromVip = false;

        LinkedListInterface<Reservation> rankedVip = priorityReservationController.generateVIPQueue(getVipList());
        for (int i = 0; i < rankedVip.size(); i++) {
            Reservation r = rankedVip.get(i);
            if (r.getRoomTypeRequested() == roomType) {
                found = r;
                fromVip = true;
                break;
            }
        }

        if (found == null) {
            LinkedListInterface<Reservation> guestQueue = getGuestQueue();
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

        if (fromVip) {
            priorityReservationController.removePriorityReservationById(found.getReservationId());
        }

        found.setRoomId(availableRoom.getRoomId());
        found.setStatus(ReservationStatus.ASSIGNED);
        found.getTimestamps().setAssignedTime(LocalDateTime.now());

        availableRoom.getReservations().addBack(found);
        updateRoomStatus(availableRoom.getRoomId(), RoomStatus.OCCUPIED);

        reservationUI.printAssignmentSummary(found, availableRoom);
        reservationUI.printSuccess();

        String[][] options = {
                { "1", "Continue with room assignment" },
                { "0", "Back to menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                assignRoom();
                break;
            case 0:
            default:
                break;
        }
    }

    // case 5
    public void checkIn() {
        LinkedListInterface<Reservation> candidates = new LinkedList<>();
        LinkedListInterface<Reservation> assignedList = getAssignedList();
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
        if (selection == 0)
            return;

        Reservation found = candidates.get(selection - 1);

        if (LocalTime.now().isBefore(LocalTime.of(12, 0))) {
            reservationUI.printCannotCheckIn();
            String[][] options = {
                    { "1", "Try again" },
                    { "0", "Back to menu" }
            };
            int choice = reservationUI.showSubMenu("Next?", options);
            switch (choice) {
                case 1:
                    checkIn();
                    break;
                case 0:
                    break;
                default:
                    break;
            }
            return;
        }

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

        reservationUI.printReservationDetails(found);
        reservationUI.printSuccess();

        String[][] options = {
                { "1", "Continue with another check in" },
                { "0", "Back to menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                checkIn();
                break;
            case 0:
            default:
                break;
        }
    }

    // case 6
    public void checkOut() {
        LinkedListInterface<Reservation> checkedIn = new LinkedList<>();
        LinkedListInterface<Reservation> assignedList = getAssignedList();
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
        if (selection == 0)
            return;

        String guestId = checkedIn.get(selection - 1).getGuestId();

        LinkedListInterface<Reservation> checkedInRooms = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getGuestId().equals(guestId) && r.getStatus() == ReservationStatus.CHECKED_IN) {
                checkedInRooms.addBack(r);
            }
        }

        System.out.println("\nCurrently Checked-In Rooms:");
        for (int i = 0; i < checkedInRooms.size(); i++) {
            Reservation r = checkedInRooms.get(i);
            System.out.println("  " + (i + 1) + ". " + r.getConfirmationNumber() + " - " + r.getRoomTypeRequested());
        }

        String[][] scopeOptions = {
                { "1", "Check Out All Rooms" },
                { "2", "Check Out Selected Rooms" },
                { "0", "Cancel" }
        };
        int scopeChoice = reservationUI.showSubMenu("Check-out Option:", scopeOptions);
        if (scopeChoice == 0)
            return;

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
                            "Check out room " + (i + 1) + " (" + r.getConfirmationNumber() + " - "
                                    + r.getRoomTypeRequested() + ")?",
                            "- Yes, check out this room",
                            "- No, keep this room");
                    if (wantsToCheckOut) {
                        selected.addBack(r);
                    }
                }
                if (selected.size() == 0) {
                    reservationUI.printError("No rooms selected! You must select at least one room to check out.");
                    continue;
                }
                System.out.println("\nRooms Selected for Check-Out:");
                reservationUI.printWaitingQueueTable(buildCheckOutSelectionTableData(selected));
                boolean proceed = reservationUI.askConfirmation(
                        "Proceed with check-out for these " + selected.size() + " room(s)?",
                        "Continue with check-out",
                        "Go back and reselect");
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
        }

        System.out.println("\nCheck-Out Summary:");
        reservationUI.printWaitingQueueTable(buildCheckOutSummaryTableData(toCheckOut));
        reservationUI.printSuccess();

        String[][] options = {
                { "1", "Continue with another check out" },
                { "0", "Back to menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                checkOut();
                break;
            case 0:
            default:
                break;
        }
    }

    // case 8
    public void checkQueuePosition() {
        LinkedListInterface<Guest> queuedGuests = buildQueuedGuestList();

        if (queuedGuests.isEmpty()) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        reservationUI.printQueuedGuestList(buildGuestSelectionTableData(queuedGuests));
        int guestSelection = reservationUI.inputListIndex("guest", queuedGuests.size());
        if (guestSelection == 0)
            return;

        Guest selectedGuest = queuedGuests.get(guestSelection - 1);

        LinkedListInterface<Reservation> guestReservationsInQueue = new LinkedList<>();
        LinkedListInterface<Reservation> guestQueue = getGuestQueue();
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation r = guestQueue.get(i);
            if (r.getGuestId().equals(selectedGuest.getGuestId())) {
                guestReservationsInQueue.addBack(r);
            }
        }

        reservationUI.printWaitingQueueTable(buildGuestQueuePositionTableData(guestReservationsInQueue));
        reservationUI.pressEnterToContinue();

        String[][] options = {
                { "1", "Check another guest's queue position" },
                { "0", "Back to menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                checkQueuePosition();
                break;
            case 0:
            default:
                break;
        }
    }

    // List of guests who currently have at least one reservation in guestQueue
    private LinkedListInterface<Guest> buildQueuedGuestList() {
        LinkedListInterface<Guest> queuedGuests = new LinkedList<>();
        LinkedListInterface<Reservation> guestQueue = getGuestQueue();
        for (int i = 0; i < guestQueue.size(); i++) {
            String guestId = guestQueue.get(i).getGuestId();
            boolean alreadyAdded = false;
            for (int j = 0; j < queuedGuests.size(); j++) {
                if (queuedGuests.get(j).getGuestId().equals(guestId)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                Guest guest = getGuestById(guestId);
                if (guest != null) {
                    queuedGuests.addBack(guest);
                }
            }
        }
        return queuedGuests;
    }

    private String[][] buildGuestQueuePositionTableData(LinkedListInterface<Reservation> guestReservations) {
        LinkedListInterface<Reservation> guestQueue = getGuestQueue();
        String[][] data = new String[guestReservations.size() + 1][6];
        data[0] = new String[] { "Position", "Conf. No.", "Room Type", "Type", "Status", "Expected Check-In" };
        for (int i = 0; i < guestReservations.size(); i++) {
            Reservation r = guestReservations.get(i);
            int position = guestQueue.indexOf(r) + 1;
            data[i + 1] = new String[] {
                    String.valueOf(position),
                    r.getConfirmationNumber(),
                    r.getRoomTypeRequested().toString(),
                    r.getReservationType().toString(),
                    r.getStatus().toString(),
                    r.getTimestamps().getExpectedCheckInDate().toString()
            };
        }
        return data;
    }

    // case 9
    public void cancelReservation() {
        LinkedListInterface<Reservation> candidates = new LinkedList<>();
        LinkedListInterface<Reservation> guestQueue = getGuestQueue();
        LinkedListInterface<Reservation> vipList = getVipList();
        LinkedListInterface<Reservation> bookingList = getBookingList();
        for (int i = 0; i < guestQueue.size(); i++) {
            candidates.addBack(guestQueue.get(i));
        }
        for (int i = 0; i < vipList.size(); i++) {
            candidates.addBack(vipList.get(i));
        }
        for (int i = 0; i < bookingList.size(); i++) {
            candidates.addBack(bookingList.get(i));
        }
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (!r.isDeleted() && r.getStatus() == ReservationStatus.ASSIGNED) {
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
        if (selection == 0)
            return;

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
        ReservationStatus originalStatus = r.getStatus();
        boolean wasVip = isVipReservation(r);
        boolean wasAssigned = originalStatus == ReservationStatus.ASSIGNED;

        r.setStatus(ReservationStatus.CANCELLED);
        if (wasVip) {
            priorityReservationController.removePriorityReservationById(r.getReservationId());
        }
        if (guest != null)
            removeReservationFromGuest(guest, r);

        if (wasAssigned) {
            Room room = getRoomById(r.getRoomId());
            if (room != null) {
                for (int j = 0; j < room.getReservations().size(); j++) {
                    if (room.getReservations().get(j).getReservationId().equals(r.getReservationId())) {
                        room.getReservations().removeIndex(j);
                        break;
                    }
                }
            }
            updateRoomStatus(r.getRoomId(), RoomStatus.AVAILABLE);
        }

        handleRefund(r);
        reservationUI.printCancelled();
        afterCancelSuccess();
    }

    // case 12
    public void deleteReservation() {
        String ic = reservationUI.inputIcOrPassport();
        Guest guest = getGuestByIcOrPassport(ic);

        if (guest == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        boolean found = false;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (r.getGuestId().equals(guest.getGuestId())
                    && !r.isDeleted()
                    && (r.getStatus() == ReservationStatus.CANCELLED
                            || r.getStatus() == ReservationStatus.CHECKED_OUT)) {
                reservationUI.printReservationDetails(r);
                found = true;
            }
        }

        if (!found) {
            reservationUI
                    .printError("No deletable reservations. Only CANCELLED or CHECKED_OUT records can be deleted.");
            reservationUI.pressEnterToContinue();
            return;
        }

        String confirmationNumber = reservationUI.inputConfirmationNumber();
        if (confirmationNumber.equals("0")) {
            return;
        }

        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (r.getConfirmationNumber().equals(confirmationNumber)
                    && r.getGuestId().equals(guest.getGuestId())) {

                if (r.getStatus() != ReservationStatus.CANCELLED
                        && r.getStatus() != ReservationStatus.CHECKED_OUT) {
                    reservationUI
                            .printError("Only CANCELLED or CHECKED_OUT reservations can be deleted. Cancel it first.");
                    reservationUI.pressEnterToContinue();
                    return;
                }
                if (r.isDeleted()) {
                    reservationUI.printError("This reservation is already deleted.");
                    reservationUI.pressEnterToContinue();
                    return;
                }

                if (!reservationUI.askConfirmation(
                        "Delete this reservation from the list?",
                        "- Confirm delete (kept in file, hidden from views)",
                        "- Keep reservation")) {
                    reservationUI.pressEnterToContinue();
                    return;
                }

                r.setDeleted(true);
                reservationUI.printSuccess();
                reservationUI.pressEnterToContinue();
                return;
            }
        }

        reservationUI.printNotFound();
        reservationUI.pressEnterToContinue();
    }

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
                { "1", "Cancel another reservation" },
                { "0", "Back to menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                cancelReservation();
                break;
            case 0:
            default:
                break;
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
            if (choice == 0)
                break;

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
                    if (matched)
                        page++;
                }
                if (!matched && page > 0) {
                    matched = choice == action;
                    action++;
                    if (matched)
                        page--;
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
        if (num == 0)
            return;
        Room room = pageList.get(num - 1);
        if (room != null) {
            reservationUI.printRoomDetails(room);
            reservationUI.pressEnterToContinue();
        }
    }

    public boolean updateRoomStatus(String roomId, RoomStatus roomStatus) {
        Room room = getRoomById(roomId);
        if (room == null) {
            return false;
        }
        room.setRoomStatus(roomStatus);
        return true;
    }

    public void saveRoomList() {
        roomDAO.saveToFile(roomList);
    }

    // ===== REPORTS =====
    // public void generateReport() {
    // ReportUI reportUI = new ReportUI(reservationUI.getScanner());

    // int choice;
    // do {
    // System.out.println("\n========================================");
    // System.out.println(" RESERVATION REPORTS");
    // System.out.println("========================================");
    // System.out.println(" 1. Nationality Demand Report");
    // System.out.println(" 2. Room Type Demand Report");
    // System.out.println(" 0. Back");
    // System.out.println("========================================");
    // choice = reservationUI.inputListIndex("report option", 2);

    // if (choice == 1) {
    // LinkedListInterface<Guest> reportGuests = new LinkedList<>();
    // guestDAO.loadFromFile(reportGuests);
    // LinkedListInterface<Reservation> reportReservations = new LinkedList<>();
    // reservationDAO.loadAllReservations(reportReservations);

    // LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("registration
    // timestamp");
    // ReservationStatus status = inputReportStatusFilter();
    // ReportResult result = new NationalityReport(reportGuests, reportReservations)
    // .generate(range[0], range[1], status);
    // reportUI.printReport(result, "NATIONALITY DEMAND REPORT");
    // reportUI.pressEnterToContinue();
    // } else if (choice == 2) {
    // LinkedListInterface<Room> reportRooms = new LinkedList<>();
    // roomDAO.loadFromFile(reportRooms);
    // LinkedListInterface<Reservation> reportReservations = new LinkedList<>();
    // reservationDAO.loadAllReservations(reportReservations);

    // LocalDateTime[] range = reportUI.inputOptionalDateTimeRange("registration
    // timestamp");
    // ReservationStatus status = inputReportStatusFilter();
    // ReportResult result = new RoomTypeReport(reportRooms, reportReservations)
    // .generate(range[0], range[1], status);
    // reportUI.printReport(result, "ROOM TYPE DEMAND REPORT");
    // reportUI.pressEnterToContinue();
    // }
    // } while (choice != 0);
    // }

    private ReservationStatus inputReportStatusFilter() {
        ReservationStatus[] values = ReservationStatus.values();
        System.out.println("\nFilter by reservation status:");
        System.out.println("  0. All statuses (no filter)");
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i]);
        }
        int choice = reservationUI.inputListIndex("status option", values.length);
        if (choice == 0) {
            return null;
        }
        return values[choice - 1];
    }

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
            case 1:
                return RoomType.STANDARD_SINGLE;
            case 2:
                return RoomType.STANDARD_DOUBLE;
            case 3:
                return RoomType.STANDARD_TRIPLE;
            case 4:
                return RoomType.DELUXE_SINGLE;
            case 5:
                return RoomType.DELUXE_DOUBLE;
            case 6:
                return RoomType.DELUXE_TRIPLE;
            case 7:
                return RoomType.SUITE;
            default:
                return null;
        }
    }

    private RoomStatus intToRoomStatus(int choice) {
        switch (choice) {
            case 1:
                return RoomStatus.AVAILABLE;
            case 2:
                return RoomStatus.OCCUPIED;
            case 3:
                return RoomStatus.CLEANING;
            default:
                return null;
        }
    }

    private int countOverlappingReservations(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        int count = 0;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (r.isDeleted())
                continue;
            if (r.getRoomTypeRequested() != roomType)
                continue;
            if (r.getStatus() == ReservationStatus.CHECKED_OUT
                    || r.getStatus() == ReservationStatus.CANCELLED)
                continue;
            LocalDate rCheckIn = r.getTimestamps().getExpectedCheckInDate();
            LocalDate rCheckOut = r.getTimestamps().getExpectedCheckOutDate();
            boolean overlap = checkIn.isBefore(rCheckOut) && checkOut.isAfter(rCheckIn);
            if (overlap) {
                count++;
            }
        }
        return count;
    }

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
        data[0] = new String[] { "No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-In" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[] {
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
        data[0] = new String[] { "No.", "Guest ID", "Name", "Nationality", "Contact" };
        for (int i = 0; i < list.size(); i++) {
            Guest g = list.get(i);
            data[i + 1] = new String[] {
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
        data[0] = new String[] { "No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-In" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[] {
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
        data[0] = new String[] { "No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-Out" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[] {
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
        data[0] = new String[] { "No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-In" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[] {
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
        data[0] = new String[] { "No.", "Room ID", "Room Number", "Price/Night" };
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            data[i + 1] = new String[] {
                    String.valueOf(i + 1),
                    r.getRoomId(),
                    r.getRoomNumber(),
                    String.format("%.2f", r.getPricePerNight())
            };
        }
        return data;
    }

    private String[][] buildQueueTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][7];
        data[0] = new String[] { "No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Type", "Status" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = getGuestName(r.getGuestId());
            data[i + 1] = new String[] {
                    String.valueOf(i + 1),
                    r.getConfirmationNumber(),
                    r.getGuestId(),
                    guestName != null ? guestName : "-",
                    r.getRoomTypeRequested().toString(),
                    r.getReservationType().toString(),
                    r.getStatus().toString()
            };
        }
        return data;
    }

    private String[][] buildBookingSummaryTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[] { "No.", "Conf. No.", "Room Type", "Nights", "Check-In Date", "Status" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[] {
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
        data[0] = new String[] { "No.", "Conf. No.", "Room Type", "Nights", "Check-In Date" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[] {
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
        data[0] = new String[] { "No.", "Conf. No.", "Room Type", "Status", "Check-In Date", "Check-Out Date",
                "Actual Check-In", "Actual Check-Out" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[] {
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
    private String generateReservationId() {
        int max = 0;
        for (int i = 0; i < reservations.size(); i++) {
            max = maxIdFrom(reservations.get(i).getReservationId(), max);
        }
        return String.format("RES%03d", max + 1);
    }

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

    private String generateConfirmationNumber() {
        while (true) {
            String confirmationNumber = String.format("%08d", (int) (Math.random() * 100000000));
            boolean duplicate = false;
            for (int i = 0; i < reservations.size(); i++) {
                if (reservations.get(i).getConfirmationNumber().equals(confirmationNumber)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                return confirmationNumber;
            }
        }
    }

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
                }
            }
        }
        return String.format("GST%03d", max + 1);
    }

    // ===== FINDERS =====
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

    public LinkedList<Reservation> findReservationsByRoomType(RoomType roomType) {
        LinkedList<Reservation> reservationList = new LinkedList<>();
        LinkedListInterface<Reservation> guestQueue = getGuestQueue();
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation reservation = guestQueue.get(i);
            if (reservation.getRoomTypeRequested() == roomType) {
                reservationList.addBack(reservation);
            }
        }
        return reservationList;
    }

    public Reservation findReservationByConfirmationNumber(String confirmationNumber) {
        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
                return reservation;
            }
        }
        return null;
    }

    public Reservation findReservationByReservationId(String reservationId) {
        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            if (reservation.getReservationId().equals(reservationId)) {
                return reservation;
            }
        }
        return null;
    }

    public Reservation findReservationByRoomId(String roomId) {
        LinkedListInterface<Reservation> assignedList = getAssignedList();
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
        LinkedListInterface<Reservation> assignedList = getAssignedList();
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
        LinkedListInterface<Reservation> assignedList = getAssignedList();
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
        LinkedListInterface<Reservation> assignedList = getAssignedList();
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

    public int countRoomsByType(RoomType roomType) {
        return getRoomsByType(roomType).size();
    }

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
            value = reservationUI.inputIc();
            if (value == null)
                continue;
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
            int[] daysInMonth = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
            if (dd < 1 || dd > daysInMonth[mm - 1]) {
                reservationUI.printInvalidInput("Invalid IC format!");
                continue;
            }
            String[] validBpCodes = {
                    "01", "21", "22", "23", "24", "02", "25", "26", "27", "03", "28", "29",
                    "04", "30", "05", "31", "59", "06", "32", "33", "07", "34", "35",
                    "08", "36", "37", "38", "39", "09", "40", "10", "41", "42", "43", "44",
                    "11", "45", "46", "12", "47", "48", "49", "13", "50", "51", "52", "53",
                    "14", "54", "55", "56", "57", "15", "58", "16"
            };
            boolean valid = false;
            for (String code : validBpCodes) {
                if (code.equals(bp)) {
                    valid = true;
                    break;
                }
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
            value = reservationUI.inputPassport();
            if (value == null)
                continue;
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

    public boolean isDuplicateIc(String icOrPassport) {
        return guestExistsByIcOrPassport(icOrPassport);
    }

    public boolean isDuplicateName(String name) {
        for (int i = 0; i < guestList.size(); i++) {
            if (guestList.get(i).getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

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
    // PaymentControl (nested)
    // ------------------------------------------------------------------
    private static class PaymentControl {
        private final LinkedListInterface<Payment> paymentList = new LinkedList<>();
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
                return 0.0;
            }
            Payment payment = findPaymentByConfirmationNumber(r.getConfirmationNumber());
            if (payment == null) {
                return 0.0;
            }
            double roomCharge = reservationControl.getPriceByRoomType(r.getRoomTypeRequested()) * r.getNumberOfNights();
            double serviceCharge = roomCharge * 0.10;
            double tax = (roomCharge + serviceCharge) * 0.06;
            double share = roomCharge + serviceCharge + tax;
            double remaining = payment.getTotalAmount() - payment.getRefundedAmount();
            if (remaining < 0.005) {
                return 0.0;
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

        public LinkedListInterface<Payment> getPaymentList() {
            return paymentList;
        }

        public PaymentMethod askPaymentMethod(ReservationUI reservationUI) {
            String[][] methodOptions = {
                    { "1", "Cash" },
                    { "2", "Credit Card" },
                    { "3", "Debit Card" },
                    { "4", "E-Wallet" },
                    { "5", "Online Banking" },
                    { "0", "Cancel" }
            };
            int methodChoice = reservationUI.showSubMenu("Select Payment Method:", methodOptions);
            switch (methodChoice) {
                case 1:
                    return PaymentMethod.CASH;
                case 2:
                    return PaymentMethod.CREDIT_CARD;
                case 3:
                    return PaymentMethod.DEBIT_CARD;
                case 4:
                    return PaymentMethod.E_WALLET;
                case 5:
                    return PaymentMethod.ONLINE_BANKING;
                default:
                    return null;
            }
        }

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