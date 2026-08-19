package tarumtresort.control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.PaymentUI;
import tarumtresort.boundary.ReservationUI;
import tarumtresort.dao.PaymentDAO;
import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Payment;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.ReservationTimestamps;
import tarumtresort.entity.Room;
import tarumtresort.entity.enums.PaymentMethod;
import tarumtresort.entity.enums.PaymentStatus;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.ReservationType;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.boundary.RoomUI;

public class ReservationControl {

    // controller
    private RoomControl roomControl = new RoomControl();
    private GuestControl guestControl = new GuestControl();
    private PaymentControl paymentControl = new PaymentControl();
    private PriorityReservationController priorityReservationController = new PriorityReservationController();

    // list declared
    private LinkedListInterface<Reservation> bookingList = new LinkedList<>();
    private LinkedListInterface<Reservation> guestQueue = new LinkedList<>();
    private LinkedListInterface<Reservation> assignedList = new LinkedList<>();
    private LinkedListInterface<Reservation> vipQueue = new LinkedList<>();

    // dao
    private static final ReservationDAO reservationDAO = new ReservationDAO();

    // UI
    private ReservationUI reservationUI = new ReservationUI();
    private RoomUI roomUI = new RoomUI();

    // Constructor
    public ReservationControl() {
        reservationDAO.loadBookingList(bookingList);
        reservationDAO.loadGuestQueue(guestQueue);
        reservationDAO.loadAssignedList(assignedList);
        reservationDAO.loadVipQueue(vipQueue);

    }

    private static final int VIEW_BOOKING_LIST = 1;
    private static final int VIEW_GUEST_QUEUE = 2;
    private static final int VIEW_ASSIGNED_LIST = 3;

    // business rule: a guest still CHECKED_IN past this hour on their
    // expectedCheckOutDate is forcibly checked out so the room can be freed up for
    // the guest queue
    private static final int FORCE_CHECKOUT_HOUR = 12;

    public void runReservationManagement() {
        int currentView = VIEW_GUEST_QUEUE;
        RoomType roomTypeFilter = null;

        while (true) {
            forceCheckoutOverdueReservations();

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
            int choice = reservationUI.printReservationListMenu(currentListName, buildQueueTableData(displayList),
                    hasFilter);

            if (hasFilter && choice == 12) {
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
                        roomTypeFilter = roomControl.intToRoomType(roomChoice);
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
                default:
                    break;
            }
        }
    }

    // business rule: guests still CHECKED_IN past FORCE_CHECKOUT_HOUR on their
    // expectedCheckOutDate are forcibly checked out so the room can be freed up for
    // the guest queue. payment is handled separately at booking time, not here.
    private void forceCheckoutOverdueReservations() {
        LocalDateTime now = LocalDateTime.now();
        LinkedListInterface<Reservation> forcedOut = new LinkedList<>();

        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getStatus() != ReservationStatus.CHECKED_IN)
                continue;

            LocalDateTime deadline = r.getTimestamps().getExpectedCheckOutDate().atTime(FORCE_CHECKOUT_HOUR, 0);
            if (now.isAfter(deadline)) {
                r.setStatus(ReservationStatus.CHECKED_OUT);
                r.getTimestamps().setActualCheckOutTime(now);
                roomControl.updateRoomStatus(r.getRoomId(), RoomStatus.CLEANING);
                forcedOut.addBack(r);
            }
        }

        if (forcedOut.size() > 0) {
            reservationDAO.saveAssignedList(assignedList);
            reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList, vipQueue);

            System.out.println("\n" + forcedOut.size() + " guest(s) were automatically checked out for exceeding the "
                    + FORCE_CHECKOUT_HOUR + ":00 checkout deadline.");
            reservationUI.printWaitingQueueTable(buildCheckOutSummaryTableData(forcedOut));
            reservationUI.pressEnterToContinue();
        }
    }

    public void runReservationModule() {
        int choice = 0;
        do {
            choice = reservationUI.getMenuChoice();
            switch (choice) {
                case 0:
                    break;
                case 1:
                    guestControl.runGuestManagement();
                    break;
                case 2:
                    runReservationManagement();
                    break;
                case 3:
                    roomControl.runRoomManagement();
                    break;
                case 4:
                    generateReport();
                    break;
                default:
                    break;
            }
        } while (choice != 0);
    }

    // case 1 - tested xprob
    public void registerGuest() {
        Guest guest = guestControl.registerGuest();

        if (guest == null)
            return;

        String[][] options = {
                { "1", "Book a room" },
                { "2", "Back to menu" },
                { "3", "Continue another guest registration" },
                { "0", "Back to main menu" }
        };

        int choice = reservationUI.showSubMenu("Next?", options);

        switch (choice) {
            case 1:
                bookRoom(guest.getGuestId());
                break;
            case 2:
                break;
            case 3:
                registerGuest();
            case 0: // back tom main menu
            default:
                break;
        }
    }

    // case 2 - tested xprob
    public void bookRoom() {

        String ic = reservationUI.inputIcOrPassport();

        if (ic.equals("0"))
            return;

        // find guest by IC
        Guest guest = guestControl.getGuestByIcOrPassport(ic);
        if (guest == null) {
            reservationUI.printNotFound();

            String[][] options = {
                    { "1", "Add Guest" },
                    { "2", "Restart Booking (Re-enter IC/Passport)" },
                    { "0", "Back to Menu" },
            };
            int choice = reservationUI.showSubMenu("Guest not found. Next?", options);
            switch (choice) {
                case 1:
                    registerGuest();
                    break;
                case 2:
                    bookRoom();
                    break;
                case 0:
                    break;
                default:
                    break;
            }
            return;
        }

        bookRoom(guest.getGuestId());
    }

    public void bookRoom(String guestId) {
        // reservation type
        System.out.println();
        int typeChoice = roomUI.printBookingTypeMenu();

        if (typeChoice == 0)
            return;

        ReservationType reservationType = typeChoice == 1
                ? ReservationType.WALK_IN
                : ReservationType.ADVANCE_BOOKING;

        // ask once, shared for all rooms in for the same guest
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
            RoomType roomType = roomControl.intToRoomType(roomChoice);
            if (roomType == null)
                break;

            // check whether enough rooms of this type exist for the requested date range
            // (also count the session's own in-memory bookings so multiple rooms
            // booked in one session do not double-book the same inventory)
            int totalRoomsOfType = roomControl.countRoomsByType(roomType);
            int overlappingReservations = countOverlappingReservations(roomType, expectedCheckInDate,
                    expectedCheckOutDate)
                    + countOverlapInList(sessionBookings, roomType, expectedCheckInDate, expectedCheckOutDate);

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
                    false,
                    timestamps);

            if (reservationType == ReservationType.WALK_IN) {
                boolean isMember = priorityReservationController.addPriorityReservation(
                        reservation.getReservationId(), reservation.getGuestId());

                if (isMember) {
                    vipQueue.addSorted(reservation);
                    reservationDAO.saveVipQueue(vipQueue);
                } else {
                    guestQueue.addSorted(reservation);
                    reservationDAO.saveGuestQueue(guestQueue);
                }
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList, vipQueue);
            } else {
                reservation.setStatus(ReservationStatus.BOOKED); 
                bookingList.addSorted(reservation);
                reservationDAO.saveBookingList(bookingList);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList, vipQueue);
            }

            Guest guest = guestControl.getGuestById(guestId);
            if (guest != null) {
                guest.getReservations().addBack(reservation);
                guestControl.saveGuestList();
            }

            System.out.println();
            reservationUI.printSuccess();
            reservationUI.printReservationDetails(reservation);

            sessionBookings.addBack(reservation);

            // ask book another room
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

            // payment collection
            Payment payment = paymentControl.processBookingPayment(sessionBookings, roomControl);
            if (payment == null) {
                reservationUI.printError("Payment was cancelled. Reservations are saved but not yet paid.");
            } else {
                reservationUI.printSuccess();
                System.out.println("Payment recorded: " + payment.getPaymentID());
            }
        }

        String[][] options = {
                { "1", "Book room for another guest" },
                { "2", "Back to module menu" },
                { "0", "Back to main menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                bookRoom();
                break;
            case 2:
                break;
            case 0:
            default:
                break;
        }
    }

    // case 3 - tested xprob
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
        if (!reservationUI.askConfirmation("Confirm guest arrival?", "Guest will be moved to queue",
                "Cancel arrival")) {
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
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList, vipQueue);

        reservationUI.printSuccess();

        // show the updated guest queue
        reservationUI.printWaitingQueueTable(buildQueueTableData(guestQueue));

        String[][] options = {
                { "1", "Continue with another guest arrival" },
                { "2", "Back to module menu" },
                { "0", "Back to main menu" }
        };

        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                guestArrival();
                break;
            case 2:
                break;
            case 0: // handle back to main menu
            default:
                break;
        }
    }

    // case 4 - tested xprob
    public void assignRoom() {
        // input room type
        int roomChoice = reservationUI.inputRoomTypeChoice();
        if (roomChoice == 0)
            return;

        RoomType roomType = roomControl.intToRoomType(roomChoice);
        if (roomType == null)
            return;

        // show all AVAILABLE rooms of this type (real-time status, set to AVAILABLE by
        LinkedListInterface<Room> availableRooms = roomControl.getAvailableRoomsByType(roomType);
        if (availableRooms.isEmpty()) {
            reservationUI.printRoomNotAvailable();
            reservationUI.pressEnterToContinue();
            return;
        }

        roomUI.printAvailableRoomList(buildAvailableRoomTableData(availableRooms));
        int roomSelection = roomUI.inputListIndex("room", availableRooms.size());
        if (roomSelection == 0)
            return;

        Room availableRoom = availableRooms.get(roomSelection - 1);

        // find first guest in queue matching room type
        Reservation found = null;

        LinkedListInterface<Reservation> vipQueue = priorityReservationController.generateVIPQueue(guestQueue);

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

        // room - do this before updateRoomStatus() so the save below persists both
        availableRoom.getReservations().addBack(found);

        // update room status (also persists the reservation just added above)
        roomControl.updateRoomStatus(availableRoom.getRoomId(), RoomStatus.OCCUPIED);

        // move to assignedList
        assignedList.addBack(found);

        // save
        reservationDAO.saveGuestQueue(guestQueue);
        reservationDAO.saveAssignedList(assignedList);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);

        reservationUI.printAssignmentSummary(found, availableRoom);
        reservationUI.printSuccess();

        String[][] options = {
                { "1", "Continue with room assignment" },
                { "2", "Back to module menu" },
                { "0", "Back to main menu" }
        };

        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                assignRoom();
                break;
            case 2:
                break;
            case 0:
            default:
                break;
        }
    }

    // case 5 - tested xprob
    public void checkIn() {
        String ic = reservationUI.inputIcOrPassport();
        Guest guest = guestControl.getGuestByIcOrPassport(ic);

        if (guest == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // find reservation in assignedList
        Reservation found = null;
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation reservation = assignedList.get(i);
            if (reservation.getGuestId().equals(guest.getGuestId())
                    && reservation.getStatus() == ReservationStatus.ASSIGNED) {
                found = reservation;
                break;
            }
        }

        if (found == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // check time validation (after 12pm)
        if (LocalTime.now().isBefore(LocalTime.of(12, 0))) {
            reservationUI.printCannotCheckIn();
            String[][] options = {
                    { "1", "Try again" },
                    { "0", "Back to module menu" }
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

        reservationUI.printReservationDetails(found);
        reservationUI.printSuccess();

        String[][] options = {
                { "1", "Continue with another check in" },
                { "2", "Back to module menu" },
                { "0", "Back to main menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                checkIn();
                break;
            case 2:
                break;
            case 0:
            default:
                break;
        }
    }

    // case 6
    public void checkOut() {
        String ic = reservationUI.inputIcOrPassport();
        Guest guest = guestControl.getGuestByIcOrPassport(ic);

        if (guest == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // gather ALL currently checked-in rooms for this guest
        LinkedListInterface<Reservation> checkedInRooms = new LinkedList<>();
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getGuestId().equals(guest.getGuestId()) && r.getStatus() == ReservationStatus.CHECKED_IN) {
                checkedInRooms.addBack(r);
            }
        }

        if (checkedInRooms.size() == 0) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // show all checked-in rooms with numbering
        System.out.println("\nCurrently Checked-In Rooms:");
        for (int i = 0; i < checkedInRooms.size(); i++) {
            Reservation r = checkedInRooms.get(i);
            System.out.println("  " + (i + 1) + ". " + r.getConfirmationNumber() + " - " + r.getRoomTypeRequested());
        }

        // ask: check out all, or select specific rooms
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

                // show summary in table form and ask for final confirmation
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

        // late check-out settlement: past the expected date, or after 11am on the
        // expected date
        LinkedListInterface<Reservation> lateRooms = new LinkedList<>();
        for (int i = 0; i < toCheckOut.size(); i++) {
            Reservation r = toCheckOut.get(i);
            LocalDate expected = r.getTimestamps().getExpectedCheckOutDate();
            long extraDays = ChronoUnit.DAYS.between(expected, now.toLocalDate());
            boolean lateSameDay = extraDays == 0 && now.toLocalTime().isAfter(LocalTime.of(11, 0));
            if (extraDays > 0 || lateSameDay) {
                lateRooms.addBack(r);
            }
        }

        if (lateRooms.size() > 0) {
            reservationUI.printError("Late check-out detected for " + lateRooms.size() + " room(s). "
                    + "Extra night(s) + RM50 fee per late room will be charged.");
            Payment latePayment = paymentControl.processLateCheckoutPayment(lateRooms, roomControl);
            if (latePayment == null) {
                reservationUI.printError("Warning: late check-out fee was NOT paid.");
            }
        }

        for (int i = 0; i < toCheckOut.size(); i++) {
            Reservation r = toCheckOut.get(i);
            r.setStatus(ReservationStatus.CHECKED_OUT);
            r.getTimestamps().setActualCheckOutTime(now);
            roomControl.updateRoomStatus(r.getRoomId(), RoomStatus.CLEANING);
            // TODO: call Brian's cleaning function.
        }

        reservationDAO.saveAssignedList(assignedList);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);

        // display final status table for the checked-out room(s)
        System.out.println("\nCheck-Out Summary:");
        reservationUI.printWaitingQueueTable(buildCheckOutSummaryTableData(toCheckOut));

        reservationUI.printSuccess();

        String[][] options = {
                { "1", "Continue with another check out" },
                { "2", "Back to module menu" },
                { "0", "Back to main menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                checkOut();
                break;
            case 2:
                break;
            case 0:
            default:
                break;
        }
    }

    // case 7 - tested xprob
    public void viewQueue() {
        String[][] options = {
                { "1", "View All Waiting Reservations" },
                { "2", "View by Room Type" },
                { "0", "Back" }
        };

        int choice = reservationUI.showSubMenu("View Queue:", options);

        switch (choice) {
            case 0:
                return;
            case 1:
                reservationUI.printWaitingQueueTable(buildQueueTableData(guestQueue));
                break;

            case 2: {
                int roomChoice = reservationUI.inputRoomTypeChoice();
                if (roomChoice == 0)
                    return;
                RoomType roomType = roomControl.intToRoomType(roomChoice);
                if (roomType == null)
                    return;
                LinkedListInterface<Reservation> filtered = findReservationsByRoomType(roomType);
                reservationUI.printWaitingQueueTable(buildQueueTableData(filtered));
                break;
            }
            default:
                return;
        }

        String[][] options2 = {
                { "1", "View all queue" },
                { "2", "View by room type" },
                { "3", "Back to module menu" },
                { "0", "Back to main menu" }
        };
        int next = reservationUI.showSubMenu("Next?", options2);
        switch (next) {
            case 1:
            case 2:
                viewQueue();
                break;
            case 3:
                break;
            case 0:
            default:
                break;
        }
    }

    // case 8 - tested xprob
    public void checkQueuePosition() {
        String icOrPassport = reservationUI.inputIcOrPassport();
        Guest guest = guestControl.getGuestByIcOrPassport(icOrPassport);

        if (guest == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // find all reservations for this guest in queue
        boolean found = false;
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation r = guestQueue.get(i);
            if (r.getGuestId().equals(guest.getGuestId())) {
                reservationUI.printQueuePosition(r.getConfirmationNumber(), i + 1);
                found = true;
            }
        }

        if (!found) {
            reservationUI.printNotFound();
        }

        reservationUI.pressEnterToContinue();

        String[][] options = {
                { "1", "Check another guest's queue position" },
                { "2", "Back to module menu" },
                { "0", "Back to main menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                checkQueuePosition();
                break;
            case 2:
                break;
            case 0:
            default:
                break;
        }
    }

    // case 9 - tested xprob
    public void cancelReservation() {
        String ic = reservationUI.inputIcOrPassport();
        Guest guest = guestControl.getGuestByIcOrPassport(ic);

        if (guest == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // find all reservations for this guest
        // check in guestQueue and bookingList
        boolean found = false;

        // check guestQueue first
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation r = guestQueue.get(i);
            if (r.getGuestId().equals(guest.getGuestId())) {
                reservationUI.printReservationDetails(r);
                found = true;
            }
        }

        // check bookingList
        for (int i = 0; i < bookingList.size(); i++) {
            Reservation r = bookingList.get(i);
            if (r.getGuestId().equals(guest.getGuestId())) {
                reservationUI.printReservationDetails(r);
                found = true;
            }
        }

        if (!found) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }

        // ask which confirmation number to cancel
        String confirmationNumber = reservationUI.inputConfirmationNumber();
        if (confirmationNumber.equals("0")) {
            return;
        }

        // find in guestQueue - not assigned to a physical room yet, so no room-side
        // cleanup needed
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation r = guestQueue.get(i);
            if (r.getConfirmationNumber().equals(confirmationNumber)
                    && r.getGuestId().equals(guest.getGuestId())) {

                if (!reservationUI.askConfirmation(
                        "Cancel this reservation?",
                        "- Confirm cancellation",
                        "- Keep reservation")) {
                    reservationUI.pressEnterToContinue();
                    return;
                }

                guestQueue.removeIndex(i);
                removeReservationFromGuest(guest, r);
                guestControl.saveGuestList();

                reservationDAO.saveGuestQueue(guestQueue);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
                handleRefund(r);
                reservationUI.printCancelled();
                afterCancelSuccess();
                return;
            }
        }

        // find in bookingList - not assigned to a physical room yet
        for (int i = 0; i < bookingList.size(); i++) {
            Reservation r = bookingList.get(i);
            if (r.getConfirmationNumber().equals(confirmationNumber)
                    && r.getGuestId().equals(guest.getGuestId())) {

                if (!reservationUI.askConfirmation(
                        "Cancel this reservation?",
                        "- Confirm cancellation",
                        "- Keep reservation")) {
                    reservationUI.pressEnterToContinue();
                    return;
                }

                bookingList.removeIndex(i);
                removeReservationFromGuest(guest, r);
                guestControl.saveGuestList();

                reservationDAO.saveBookingList(bookingList);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
                handleRefund(r);
                reservationUI.printCancelled();
                afterCancelSuccess();
                return;
            }
        }

        // check assignedList - cannot cancel if CHECKED_IN or CHECKED_OUT
        for (int i = 0; i < assignedList.size(); i++) {
            Reservation r = assignedList.get(i);
            if (r.getConfirmationNumber().equals(confirmationNumber)) {
                if (r.getStatus() == ReservationStatus.CHECKED_IN
                        || r.getStatus() == ReservationStatus.CHECKED_OUT) {
                    reservationUI.printCannotCancel();
                    reservationUI.pressEnterToContinue();
                    return;
                }

                // ASSIGNED - can cancel, free up room
                if (!reservationUI.askConfirmation("Cancel this reservation?", "- Confirm cancellation",
                        "- Keep reservation")) {
                    reservationUI.pressEnterToContinue();
                    return;
                }
                assignedList.removeIndex(i);
                removeReservationFromGuest(guest, r);
                guestControl.saveGuestList();

                // this reservation never actually checked in - remove its record
                // from the room too, so it doesn't linger as a false occupancy
                Room room = roomControl.getRoomById(r.getRoomId());
                if (room != null) {
                    for (int j = 0; j < room.getReservations().size(); j++) {
                        if (room.getReservations().get(j).getReservationId().equals(r.getReservationId())) {
                            room.getReservations().removeIndex(j);
                            break;
                        }
                    }
                }

                // updateRoomStatus() also persists the removal above
                roomControl.updateRoomStatus(r.getRoomId(), RoomStatus.AVAILABLE);
                reservationDAO.saveAssignedList(assignedList);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
                reservationUI.printCancelled();
                afterCancelSuccess();
                return;
            }
        }

        reservationUI.printNotFound();
        reservationUI.pressEnterToContinue();
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
                { "1", "Cancel another reservation" },
                { "2", "Back to module menu" },
                { "0", "Back to main menu" }
        };
        int choice = reservationUI.showSubMenu("Next?", options);
        switch (choice) {
            case 1:
                cancelReservation();
                break;
            case 2:
                break;
            case 0:
            default:
                break;
        }
    }

    // case 10 TODO: nationality of the guest & ?
    public void generateReport() {
    }

    // GENERATE
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

    // HELPER
    // count how many pending/active reservations of this room type overlap the in
    // the given date range, across bookingList + guestQueue + assignedList.
    private int countOverlappingReservations(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        int count = 0;
        count += countOverlapInList(bookingList, roomType, checkIn, checkOut);
        count += countOverlapInList(guestQueue, roomType, checkIn, checkOut);
        count += countOverlapInList(assignedList, roomType, checkIn, checkOut);
        return count;
    }

    private int countOverlapInList(LinkedListInterface<Reservation> list, RoomType roomType, LocalDate checkIn,
            LocalDate checkOut) {
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);

            if (r.getRoomTypeRequested() != roomType)
                continue;

            // a guest that has already checked out no longer occupies this date range
            if (r.getStatus() == ReservationStatus.CHECKED_OUT)
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

    // returns a new list containing the same reservations, ordered by
    // expectedCheckInDate ascending (soonest first) - does not mutate the source
    // list
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

    private String[][] buildArrivalListTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[] { "No.", "Conf. No.", "Guest ID", "Guest Name", "Room Type", "Expected Check-In" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            String guestName = guestControl.getGuestName(r.getGuestId());
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

    // build table data from queue
    private String[][] buildQueueTableData(LinkedListInterface<Reservation> list) {
        String[][] data = new String[list.size() + 1][6];
        data[0] = new String[] { "No.", "Conf. No.", "Guest ID", "Room Type", "Type", "Status" };
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            data[i + 1] = new String[] {
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
                    String.valueOf(r.getTimestamps().getActualCheckInTime()),
                    String.valueOf(r.getTimestamps().getActualCheckOutTime())
            };
        }
        return data;
    }

    // find methods
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

    public boolean reservationExists(String confirmationNumber) {
        return findReservationByConfirmationNumber(confirmationNumber) != null;
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

    // validation checking
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

    private void handleRefund(Reservation r) {
        double refund = paymentControl.refundReservation(r, roomControl);
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

        // UI
        private ReservationUI reservationUI = new ReservationUI();
        private PaymentUI paymentUI = new PaymentUI();

        public PaymentControl() {
            paymentDAO.loadFromFile(paymentList);
        }

        // called from bookRoom() - one combined payment for the whole booking session
        public Payment processBookingPayment(LinkedListInterface<Reservation> reservations, RoomControl roomControl) {
            if (reservations == null || reservations.size() == 0) {
                return null;
            }

            double totalRoomCharge = 0;
            for (int i = 0; i < reservations.size(); i++) {
                Reservation r = reservations.get(i);
                double pricePerNight = roomControl.getPriceByRoomType(r.getRoomTypeRequested());
                totalRoomCharge += pricePerNight * r.getNumberOfNights();
            }

            double serviceCharge = totalRoomCharge * 0.10;
            double tax = (totalRoomCharge + serviceCharge) * 0.06;
            double total = totalRoomCharge + serviceCharge + tax;

            paymentUI.printBill(totalRoomCharge, serviceCharge, tax, 0.0, total);

            PaymentMethod method = askPaymentMethod(reservationUI);
            if (method == null) {
                return null; // guest cancelled payment
            }

            Payment payment = new Payment(
                    generatePaymentId(),
                    totalRoomCharge,
                    serviceCharge,
                    tax,
                    total,
                    method,
                    PaymentStatus.PAID,
                    LocalDateTime.now(),
                    reservations.get(0).getConfirmationNumber());

            for (int i = 0; i < reservations.size(); i++) {
                payment.addConfirmationNumber(reservations.get(i).getConfirmationNumber());
            }

            paymentList.addBack(payment);
            paymentDAO.saveToFile(paymentList);
            return payment;
        }

        // called from checkOut() - late check-out settlement:
        // extra nights at the room's rate (+10% service, +6% tax) + RM50 flat fee per
        // late room
        public Payment processLateCheckoutPayment(LinkedListInterface<Reservation> reservations,
                RoomControl roomControl) {
            if (reservations == null || reservations.size() == 0) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();

            double extraRoomCharge = 0;
            double lateFee = 0;
            for (int i = 0; i < reservations.size(); i++) {
                Reservation r = reservations.get(i);
                LocalDate expected = r.getTimestamps().getExpectedCheckOutDate();
                long extraDays = ChronoUnit.DAYS.between(expected, now.toLocalDate());
                if (extraDays > 0) {
                    extraRoomCharge += extraDays * roomControl.getPriceByRoomType(r.getRoomTypeRequested());
                }
                lateFee += 50.0; // flat fee per late room
            }

            double serviceCharge = extraRoomCharge * 0.10;
            double tax = (extraRoomCharge + serviceCharge) * 0.06;
            double total = extraRoomCharge + serviceCharge + tax + lateFee;

            paymentUI.printBill(extraRoomCharge, serviceCharge, tax, lateFee, total);

            PaymentMethod method = askPaymentMethod(reservationUI);
            if (method == null) {
                return null; // fee not paid - checkout still proceeds with a warning
            }

            Payment payment = new Payment(
                    generatePaymentId(),
                    extraRoomCharge,
                    serviceCharge,
                    tax,
                    total,
                    method,
                    PaymentStatus.PAID,
                    now,
                    reservations.get(0).getConfirmationNumber());

            for (int i = 0; i < reservations.size(); i++) {
                payment.addConfirmationNumber(reservations.get(i).getConfirmationNumber());
            }

            paymentList.addBack(payment);
            paymentDAO.saveToFile(paymentList);
            return payment;
        }

        // refund policy: 100% refund if cancelled at least 24 hours before the
        // 12pm check-in moment; 0% refund if cancelled within the last 24 hours.
        // Called from cancelReservation() after the reservation is removed.
        public double refundReservation(Reservation r, RoomControl roomControl) {
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
            double roomCharge = roomControl.getPriceByRoomType(r.getRoomTypeRequested()) * r.getNumberOfNights();
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

        public void displayPaymentRecords() {
            paymentUI.printPaymentRecords(paymentList);
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