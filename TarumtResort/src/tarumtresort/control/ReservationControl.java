package tarumtresort.control;

import tarumtresort.dao.ReservationDAO;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.PaymentMethod;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.ReservationType;
import tarumtresort.entity.enums.RoomStatus;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.ReservationUI;
import tarumtresort.boundary.GuestUI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import tarumtresort.entity.ReservationTimestamps;
import tarumtresort.entity.Room;

public class ReservationControl {

    // controller
    private RoomControl roomControl = new RoomControl();
    private GuestControl guestControl = new GuestControl();
    private PaymentControl paymentControl = new PaymentControl();
    
    // list declared
    private LinkedListInterface <Reservation> bookingList = new LinkedList<>();
    private LinkedListInterface<Reservation> guestQueue = new LinkedList<>();
    private LinkedListInterface<Reservation> assignedList = new LinkedList<>();

     // dao 
    private static final ReservationDAO reservationDAO = new ReservationDAO();
   
    // UI
    private ReservationUI reservationUI = new ReservationUI();
    private GuestUI guestUI = new GuestUI();

    // Constructor 
    public ReservationControl() {
        reservationDAO.loadBookingList(bookingList);
        reservationDAO.loadGuestQueue(guestQueue);
        reservationDAO.loadAssignedList(assignedList);
        
    }

    public void runReservationModule() {
        int choice = 0;
        do {
            choice = reservationUI.getMenuChoice();
            switch (choice) {
                case 0: break;
                case 1: registerGuest(); break; // registration for new guest + continue to room booking? 
                case 2: bookRoom(); break; // walk in or advance booking 
                case 3: guestArrival(); break; // advance booking guest arrive 
                case 4: assignRoom(); break; //assign room to the guest in the queue 
                case 5: checkIn(); break; // when the guest get thier room key and ready to cehck in (constrain: after 1200pm)
                case 6: checkOut(); break; // when the guest return room key + make payment (constraint: before 11am)
                case 7: viewQueue(); break; // check the queue - for the whole queue OR for only specific room type
                case 8: checkQueuePosition(); break; // check a specific position for certain client by entering the confirmation number
                case 9: cancelReservation(); break; // cancel reservation (constraint: status like waiting, advance, assigned can cancel reservation but check in and check out both cannot make cancelation)
                case 10: generateReport(); break; // generate the two report 
                default: break;
            }
        } while (choice != 0);
    }

    // case 1 - tested xprob 
    public void registerGuest() {
        Guest guest = guestControl.registerGuest();
        
        if (guest == null) return;
        
        System.out.println(" ");
        guestUI.printGuestDetails(guest);
        guestUI.printSuccess();

        reservationUI.pressEnterToContinue();
        ConsoleUtil.clearScreen();

        String[][] options = {
            {"1", "Book a room"},
            {"2","Back to menu"},
            {"3","Continue another guest registration"},
            {"0", "Back to main menu"}
        };
        
        ;
        int choice = reservationUI.showSubMenu("What would you like to do next?", options);
        
        switch (choice) {
            case 1: bookRoom(guest.getGuestId()); break;
            case 2: break;
            case 3: registerGuest();
            case 0: // back tom main menu 
            default: break;
        }
    }

    // case 2 - tested xprob
    public void bookRoom() {
        ConsoleUtil.clearScreen();

        String ic = reservationUI.inputIcOrPassport();
        
        // find guest by IC
        Guest guest = guestControl.getGuestByIcOrPassport(ic);
        if (guest == null) {
            reservationUI.printNotFound();

            String[][] options = {
                {"1", "Add Guest"},
                {"2", "Restart Booking (Re-enter IC/Passport)"},
                {"3", "Back to Module Menu"},
                {"4", "Back to Main Menu"}
            };
            int choice = reservationUI.showSubMenu("Guest not found. What would you like to do?", options);
            switch (choice) {
                case 1: registerGuest(); break;
                case 2: bookRoom(); break;
                case 3: break;
                case 4: break;
                default: break;
            }
            return;
        }
        
        bookRoom(guest.getGuestId());
    }

    public void bookRoom(String guestId) {
        // reservation type
        ConsoleUtil.clearScreen();
        
        String[][] typeOptions = {
            {"1", "Walk-in"},
            {"2", "Advance Booking"},
            {"0", "Back"}
        };
        int typeChoice = reservationUI.showSubMenu("Reservation Type:", typeOptions);
        if (typeChoice == 0) return;

        ReservationType reservationType = typeChoice == 1
            ? ReservationType.WALK_IN
            : ReservationType.ADVANCE_BOOKING;

        // ask once - shared for all rooms
        int numberOfNights = reservationUI.inputNumberOfNights();
        while (numberOfNights < 1) {
            reservationUI.printError("Must be at least 1!");
            numberOfNights = reservationUI.inputNumberOfNights();
        }

        LocalDate expectedCheckInDate;
        if (reservationType == ReservationType.WALK_IN) {
            expectedCheckInDate = LocalDate.now();
        } else {
            expectedCheckInDate = inputValidDate("Expected check-in date (must be at least 2 days from today)", LocalDate.now().plusDays(2));
        }
        LocalDate expectedCheckOutDate = expectedCheckInDate.plusDays(numberOfNights);

        LinkedListInterface<Reservation> sessionBookings = new LinkedList<>();

        // loop for multiple rooms
        boolean continueBooking = true;
        while (continueBooking) {

            // ask per room
            int roomChoice = reservationUI.inputRoomTypeChoice();
            if (roomChoice == 0) break;
            RoomType roomType = null;
            switch (roomChoice) {
                case 1: roomType = RoomType.STANDARD_SINGLE; break;
                case 2: roomType = RoomType.STANDARD_DOUBLE; break;
                case 3: roomType = RoomType.STANDARD_TRIPLE; break;
                case 4: roomType = RoomType.DELUXE_SINGLE; break;
                case 5: roomType = RoomType.DELUXE_DOUBLE; break;
                case 6: roomType = RoomType.DELUXE_TRIPLE; break;
                case 7: roomType = RoomType.SUITE; break;
                default: break;
            }
            if (roomType == null) break; 

            // check whether enough rooms of this type exist for the requested date range
            int totalRoomsOfType = roomControl.countRoomsByType(roomType);
            int overlappingReservations = countOverlappingReservations(roomType, expectedCheckInDate, expectedCheckOutDate);

            if (overlappingReservations >= totalRoomsOfType) {
                reservationUI.printRoomNotAvailable();
                reservationUI.pressEnterToContinue();
                continue;
            }

            int numberOfGuests = reservationUI.inputNumberOfGuests();
            while (numberOfGuests < 1) {
                reservationUI.printError("Must be at least 1!");
                numberOfGuests = reservationUI.inputNumberOfGuests();
            }

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
                false,
                timestamps
            );

            // save to correct list
            if (reservationType == ReservationType.WALK_IN) {
                guestQueue.addSorted(reservation);
                reservationDAO.saveGuestQueue(guestQueue);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            } else {
                bookingList.addSorted(reservation);
                reservationDAO.saveBookingList(bookingList);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
            }

            Guest guest = guestControl.getGuestById(guestId);
            if (guest != null) {
                guest.getReservations().addBack(reservation);
                guestControl.saveGuestList();
            }

            reservationUI.printReservationDetails(reservation);
            reservationUI.printSuccess();

            sessionBookings.addBack(reservation);

            // ask book another room
            continueBooking = reservationUI.askConfirmation(
                "Book another room for this guest?",
                "- Add another room",
                "- Done booking"
            );
        }

        if (sessionBookings.size() > 0) {
            reservationUI.printWaitingQueueTable(buildBookingSummaryTableData(sessionBookings));
            reservationUI.printSuccess();
        }

        String[][] options = {
            {"1", "Book room for another guest"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("What would you like to do next?", options);
        switch (choice) {
            case 1: bookRoom(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }

    // case 3 - tested xprob 
    public void guestArrival() {
        ConsoleUtil.clearScreen();

        // find guest by IC
        String ic = reservationUI.inputIcOrPassport();
        Guest guest = guestControl.getGuestByIcOrPassport(ic);
        
        if (guest == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }
        
        // find advance booking in bookingList
        Reservation found = null;
        int foundIndex = -1;
        
        for (int i = 0; i < bookingList.size(); i++) {
            Reservation r = bookingList.get(i);
            if (r.getGuestId().equals(guest.getGuestId())) {
                found = r;
                foundIndex = i;
                break;
            }
        }
        
        if (found == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }
        
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
        bookingList.removeIndex(foundIndex);
        found.getTimestamps().setRegistrationTimestamp(LocalDateTime.now());
        guestQueue.addSorted(found);
        
        // save both lists
        reservationDAO.saveBookingList(bookingList);
        reservationDAO.saveGuestQueue(guestQueue);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
        
        reservationUI.printSuccess();
        

        String[][] options = {
            {"1", "Continue with another guest arrival"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };

        int choice = reservationUI.showSubMenu("What would you like to do next?", options);
        switch (choice) {
            case 1: guestArrival(); break;
            case 2: break;
            case 0: // handle back to main menu
            default: break;
        }
    }

    // case 4 - tested xprob 
    public void assignRoom() {
        // input room type
        int roomChoice = reservationUI.inputRoomTypeChoice();
        if (roomChoice == 0) return;
        
        RoomType roomType;
        switch (roomChoice) {
            case 1: roomType = RoomType.STANDARD_SINGLE; break;
            case 2: roomType = RoomType.STANDARD_DOUBLE; break;
            case 3: roomType = RoomType.STANDARD_TRIPLE; break;
            case 4: roomType = RoomType.DELUXE_SINGLE; break;
            case 5: roomType = RoomType.DELUXE_DOUBLE; break;
            case 6: roomType = RoomType.DELUXE_TRIPLE; break;
            case 7: roomType = RoomType.SUITE; break;
            default: return;
        }
        
        // find available room (real-time status, set to AVAILABLE by housekeeping module after cleaning)
        Room availableRoom = roomControl.getAvailableRoom(roomType);
        if (availableRoom == null) {
            reservationUI.printRoomNotAvailable();
            reservationUI.pressEnterToContinue();
            return;
        }
        
        // find first guest in queue matching room type
        Reservation found = null;
        int foundIndex = -1;
        for (int i = 0; i < guestQueue.size(); i++) {
            Reservation r = guestQueue.get(i);
            if (r.getRoomTypeRequested() == roomType) {
                found = r;
                foundIndex = i;
                break;
            }
        }
        
        if (found == null) {
            reservationUI.printNotFound();
            reservationUI.pressEnterToContinue();
            return;
        }
        
        // assign room
        guestQueue.removeIndex(foundIndex);
        found.setRoomId(availableRoom.getRoomId());
        found.setStatus(ReservationStatus.ASSIGNED);
        found.getTimestamps().setAssignedTime(LocalDateTime.now());

        // this is the ONLY place a reservation gets recorded against a specific
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
        
        reservationUI.printReservationDetails(found);
        reservationUI.printSuccess();
        
        String[][] options = {
            {"1", "Continue with room assignment"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };

        int choice = reservationUI.showSubMenu("What would you like to do next?", options);
        switch (choice) {
            case 1: assignRoom(); break;  
            case 2: break;
            case 0:
            default: break;
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
            if (reservation.getGuestId().equals(guest.getGuestId()) && reservation.getStatus() == ReservationStatus.ASSIGNED) {
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
                {"1", "Try again"},
                {"2", "Back to module menu"},
                {"0", "Back to main menu"}
            };
            int choice = reservationUI.showSubMenu("What would you like to do?", options);
            switch (choice) {
                case 1: checkIn(); break;
                case 2: break;
                case 0: // TODO: connect to main menu
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
        
        reservationUI.printSuccess();
        
        String[][] options = {
            {"1", "Continue with another check in"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("What would you like to do next?", options);
        switch (choice) {
            case 1: checkIn(); break;
            case 2: break;
            case 0:
            default: break;
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
                String[] header = {"No.", "Conf. No.", "Room Type", "Nights", "Check-In Date"};
                String[][] rows = new String[selected.size()][5];
                for (int i = 0; i < selected.size(); i++) {
                    Reservation r = selected.get(i);
                    rows[i] = new String[]{
                        String.valueOf(i + 1),
                        r.getConfirmationNumber(),
                        r.getRoomTypeRequested().toString(),
                        String.valueOf(r.getNumberOfNights()),
                        r.getTimestamps().getExpectedCheckInDate().toString()
                    };
                }
                TablePrinter.displayTable(header, rows);

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

        boolean isLateCheckout = false;
        for (int i = 0; i < toCheckOut.size(); i++) {
            LocalDate expectedCheckOutDate = toCheckOut.get(i).getTimestamps().getExpectedCheckOutDate();
            LocalDateTime checkoutDeadline = expectedCheckOutDate.atTime(11, 0);
            if (now.isAfter(checkoutDeadline)) {
                isLateCheckout = true;
                break;
            }
        }

        PaymentMethod method = paymentControl.askPaymentMethod(reservationUI);
        if (method == null) {
            reservationUI.pressEnterToContinue();
            return; // guest cancelled payment
        }

        if (!reservationUI.askConfirmation(
                "Confirm check out for " + toCheckOut.size() + " room(s)?",
                "- Selected room(s) will be checked out",
                "- Cancel check out")) {
            reservationUI.pressEnterToContinue();
            return;
        }

        paymentControl.processGroupCheckoutPayment(toCheckOut, roomControl, isLateCheckout, method);

        for (int i = 0; i < toCheckOut.size(); i++) {
            Reservation r = toCheckOut.get(i);
            r.setStatus(ReservationStatus.CHECKED_OUT);
            r.getTimestamps().setActualCheckOutTime(now);
            roomControl.updateRoomStatus(r.getRoomId(), RoomStatus.CLEANING);
        }

        reservationDAO.saveAssignedList(assignedList);
        reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);

        // display final status table for the checked-out room(s)
        System.out.println("\nCheck-Out Summary:");
        String[] header = {"No.", "Conf. No.", "Room Type", "Status", "Check-In Date", "Check-Out Date", "Actual Check-In", "Actual Check-Out"};
        String[][] rows = new String[toCheckOut.size()][8];
        for (int i = 0; i < toCheckOut.size(); i++) {
            Reservation r = toCheckOut.get(i);
            rows[i] = new String[]{
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
        TablePrinter.displayTable(header, rows);

        reservationUI.printSuccess();

        String[][] options = {
            {"1", "Continue with another check out"},
            {"2", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int choice = reservationUI.showSubMenu("What would you like to do next?", options);
        switch (choice) {
            case 1: checkOut(); break;
            case 2: break;
            case 0:
            default: break;
        }
    }
    
    // case 7 - tested xprob
    public void viewQueue() {
        String[][] options = {
            {"1", "View All Waiting Reservations"},
            {"2", "View by Room Type"},
            {"0", "Back"}
        };
        
        int choice = reservationUI.showSubMenu("View Queue:", options);
        
        switch (choice) {
            case 0: return;
            case 1:
                reservationUI.printWaitingQueueTable(buildQueueTableData(guestQueue));
                break;

            case 2: {
                int roomChoice = reservationUI.inputRoomTypeChoice();
                if (roomChoice == 0) return;
                RoomType roomType = null;
                switch (roomChoice) {
                    case 1: roomType = RoomType.STANDARD_SINGLE; break;
                    case 2: roomType = RoomType.STANDARD_DOUBLE; break;
                    case 3: roomType = RoomType.STANDARD_TRIPLE; break;
                    case 4: roomType = RoomType.DELUXE_SINGLE; break;
                    case 5: roomType = RoomType.DELUXE_DOUBLE; break;
                    case 6: roomType = RoomType.DELUXE_TRIPLE; break;
                    case 7: roomType = RoomType.SUITE; break;
                }
                if (roomType == null) return;
                LinkedListInterface<Reservation> filtered = findReservationsByRoomType(roomType);
                reservationUI.printWaitingQueueTable(buildQueueTableData(filtered));
                break;
            }
            default: return; 
        }
        
        String[][] options2 = {
            {"1", "View all queue"},
            {"2", "View by room type"},
            {"3", "Back to module menu"},
            {"0", "Back to main menu"}
        };
        int next = reservationUI.showSubMenu("What would you like to do next?", options2);
        switch (next) {
            case 1: case 2: viewQueue(); break;
            case 3: break;
            case 0: default: break;
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

        // find in guestQueue - not assigned to a physical room yet, so no room-side cleanup needed
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

                // remove from guest's own reservation list too
                for (int j = 0; j < guest.getReservations().size(); j++) {
                    if (guest.getReservations().get(j).getReservationId().equals(r.getReservationId())) {
                        guest.getReservations().removeIndex(j);
                        break;
                    }
                }
                guestControl.saveGuestList();

                reservationDAO.saveGuestQueue(guestQueue);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
                reservationUI.printCancelled();
                reservationUI.pressEnterToContinue();
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

                // remove from guest's own reservation list too
                for (int j = 0; j < guest.getReservations().size(); j++) {
                    if (guest.getReservations().get(j).getReservationId().equals(r.getReservationId())) {
                        guest.getReservations().removeIndex(j);
                        break;
                    }
                }
                guestControl.saveGuestList();

                reservationDAO.saveBookingList(bookingList);
                reservationDAO.saveAllReservations(bookingList, guestQueue, assignedList);
                reservationUI.printCancelled();
                reservationUI.pressEnterToContinue();
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
                } else {
                    // ASSIGNED - can cancel, free up room
                    if (!reservationUI.askConfirmation( "Cancel this reservation?", "- Confirm cancellation",  "- Keep reservation")) {
                        reservationUI.pressEnterToContinue();
                        return;
                    }
                    assignedList.removeIndex(i);

                    // remove from guest's own reservation list too
                    for (int j = 0; j < guest.getReservations().size(); j++) {
                        if (guest.getReservations().get(j).getReservationId().equals(r.getReservationId())) {
                            guest.getReservations().removeIndex(j);
                            break;
                        }
                    }
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
                }
                reservationUI.pressEnterToContinue();
                return;
            }
        }

        reservationUI.printNotFound();
        reservationUI.pressEnterToContinue();
    }

    // case 10 TODO: nationality of the guest & ? 
    public void generateReport() {}

    // GENERATE
    // generate reservation id
    private String generateReservationId() {

        int max = 0;

        // check booking list
        for (int i = 0; i < bookingList.size(); i++) {
            String reservationId = bookingList.get(i).getReservationId();
            int number = Integer.parseInt(reservationId.substring(3));
            if (number > max) {
                max = number;
            }
        }

        // check waiting queue
        for (int i = 0; i < guestQueue.size(); i++) {
            String reservationId = guestQueue.get(i).getReservationId();
            int number = Integer.parseInt(reservationId.substring(3));
            if (number > max) {
                max = number;
            }
        }

        // check assigned list
        for (int i = 0; i < assignedList.size(); i++) {
            String reservationId = assignedList.get(i).getReservationId();
            int number = Integer.parseInt(reservationId.substring(3));
            if (number > max) {
                max = number;
            }
        }

        return String.format("RES%03d", max + 1);
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
            String input = reservationUI.inputDate(prompt);
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

    public static void main(String[] args) {
        ReservationControl reservationControl = new ReservationControl();
        reservationControl.runReservationModule();
    }

}