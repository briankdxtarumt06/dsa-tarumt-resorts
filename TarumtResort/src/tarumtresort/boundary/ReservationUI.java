package tarumtresort.boundary;

import tarumtresort.entity.*;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class ReservationUI {

    Scanner scanner = new Scanner(System.in);

    // MENU
    public int getMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  WALK-IN REGISTRATION & BOOKING MODULE");
        System.out.println("========================================");
        System.out.println("  1. Register Guest");
        System.out.println("  2. Assign Room to Next Guest");
        System.out.println("  3. Check In");
        System.out.println("  4. Check Out");
        System.out.println("  5. View Queue");
        System.out.println("  6. Check Queue Position");
        System.out.println("  7. Cancel Reservation");
        System.out.println("  8. Reports");
        System.out.println("  0. Exit");
        System.out.println("========================================");
        return inputIntChoice("Enter choice", 0, 8);
    }

    public int getViewQueueMenuChoice() {
        System.out.println("\n---- VIEW QUEUE ----");
        System.out.println("  1. View All Waiting Reservations");
        System.out.println("  2. View by Room Type");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    public int getReportMenuChoice() {
        System.out.println("\n---- REPORTS ----");
        System.out.println("  1. Room Type Demand Report");
        System.out.println("  2. Average Wait Time by Room Type Report");
        System.out.println("  0. Back");
        return inputIntChoice("Enter choice", 0, 2);
    }

    // INPUT METHODS
    public String inputGuestId() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter Guest ID: ");
            input = scanner.nextLine();
            if (input.trim().isEmpty())
                System.out.println("  ✗ Guest ID cannot be empty!");
        }
        return input.trim();
    }

    public RoomType inputRoomType() {
        System.out.println("  1. STANDARD SINGLE");
        System.out.println("  2. STANDARD DOUBLE");
        System.out.println("  3. STANDARD TRIPLE");
        System.out.println("  4. DELUXE SINGLE");
        System.out.println("  5. DELUXE DOUBLE");
        System.out.println("  6. DELUXE TRIPLE");
        System.out.println("  7. SUITE");
        int choice = inputIntChoice("Enter room type", 1, 7);
        switch (choice) {
            case 1: return RoomType.STANDARD_SINGLE;
            case 2: return RoomType.STANDARD_DOUBLE;
            case 3: return RoomType.STANDARD_TRIPLE;
            case 4: return RoomType.DELUXE_SINGLE;
            case 5: return RoomType.DELUXE_DOUBLE;
            case 6: return RoomType.DELUXE_TRIPLE;
            default: return RoomType.SUITE;
        }
    }

    public int inputNumberOfGuests() {
        int num = 0;
        while (num < 1) {
            System.out.print("Enter number of guests: ");
            if (scanner.hasNextInt()) {
                num = scanner.nextInt();
                scanner.nextLine();
                if (num < 1)
                    System.out.println("  ✗ Must be at least 1!");
            } else {
                System.out.println("  ✗ Invalid input! Please enter a number.");
                scanner.nextLine();
            }
        }
        return num;
    }

    public int inputNumberOfNights() {
        int num = 0;
        while (num < 1) {
            System.out.print("Enter number of nights: ");
            if (scanner.hasNextInt()) {
                num = scanner.nextInt();
                scanner.nextLine();
                if (num < 1)
                    System.out.println("  ✗ Must be at least 1!");
            } else {
                System.out.println("  ✗ Invalid input! Please enter a number.");
                scanner.nextLine();
            }
        }
        return num;
    }

    public ReservationType inputReservationType() {
        System.out.println("  1. WALK-IN");
        System.out.println("  2. ADVANCE BOOKING");
        int choice = inputIntChoice("Enter reservation type", 1, 2);
        return choice == 1 ? ReservationType.WALK_IN : ReservationType.ADVANCE_BOOKING;
    }

    public LocalDate inputExpectedCheckInDate() {
        LocalDate date = null;
        while (date == null) {
            System.out.print("Enter expected check-in date (YYYY-MM-DD): ");
            String input = scanner.nextLine();
            try {
                date = LocalDate.parse(input);
                if (date.isBefore(LocalDate.now())) {
                    System.out.println("  ✗ Check-in date cannot be in the past!");
                    date = null;
                }
            } catch (DateTimeParseException e) {
                System.out.println("  ✗ Invalid date format! Please use YYYY-MM-DD.");
            }
        }
        return date;
    }

    public String inputConfirmationNumber() {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print("Enter confirmation number (8 digits): ");
            input = scanner.nextLine();
            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Confirmation number cannot be empty!");
            } else if (!input.matches("\\d{8}")) {
                System.out.println("  ✗ Confirmation number must be 8 digits!");
                input = "";
            }
        }
        return input.trim();
    }

    public boolean inputConfirmation(String message) {
        System.out.print(message + " (Y/N): ");
        String input = scanner.nextLine();
        return input.equalsIgnoreCase("Y");
    }

    public Reservation inputReservationDetails(String reservationId, String confirmationNumber) {
        System.out.println("\n--- Register New Guest ---");

        String guestId = inputGuestId();

        System.out.println("\nSelect Room Type:");
        RoomType roomType = inputRoomType();

        int numberOfGuests = inputNumberOfGuests();
        int numberOfNights = inputNumberOfNights();

        System.out.println("\nSelect Reservation Type:");
        ReservationType reservationType = inputReservationType();

        LocalDate expectedCheckInDate;
        if (reservationType == ReservationType.WALK_IN) {
            expectedCheckInDate = LocalDate.now();
        } else {
            expectedCheckInDate = inputExpectedCheckInDate();
        }

        ReservationTimestamps timestamps = new ReservationTimestamps(
            LocalDateTime.now(),
            expectedCheckInDate,
            expectedCheckInDate.plusDays(numberOfNights)
        );

        System.out.println();
        return new Reservation(
            reservationId,
            confirmationNumber,
            guestId,
            null,
            roomType,
            numberOfGuests,
            numberOfNights,
            reservationType,
            ReservationStatus.WAITING,
            timestamps
        );
    }

    // MULTI-ROOM BOOKING (one guest, several rooms in one session)

    // shared info collected once at the start of the booking session
    public String[] inputBookingSessionStart() {
        // returns [guestId] — kept as array in case more shared fields are added later
        System.out.println("\n--- Register New Guest (Booking Session) ---");
        String guestId = inputGuestId();
        return new String[]{guestId};
    }

    public ReservationType inputSessionReservationType() {
        System.out.println("\nSelect Reservation Type for this booking:");
        return inputReservationType();
    }

    public LocalDate inputSessionCheckInDate(ReservationType reservationType) {
        if (reservationType == ReservationType.WALK_IN) {
            return LocalDate.now();
        }
        return inputExpectedCheckInDate();
    }

    // collects ONE room's details (room type / guests / nights), reusing shared session info
    public Reservation inputSingleRoomBooking(String reservationId, String confirmationNumber,
                                               String guestId, ReservationType reservationType,
                                               LocalDate expectedCheckInDate) {

        System.out.println("\n--- Room Booking ---");
        System.out.println("Select Room Type:");
        RoomType roomType = inputRoomType();

        int numberOfGuests = inputNumberOfGuests();
        int numberOfNights = inputNumberOfNights();

        ReservationTimestamps timestamps = new ReservationTimestamps(
            LocalDateTime.now(),
            expectedCheckInDate,
            expectedCheckInDate.plusDays(numberOfNights)
        );

        System.out.println();
        return new Reservation(
            reservationId,
            confirmationNumber,
            guestId,
            null,
            roomType,
            numberOfGuests,
            numberOfNights,
            reservationType,
            ReservationStatus.WAITING,
            timestamps
        );
    }

    public boolean inputAddAnotherRoom() {
        return inputConfirmation("\nAdd another room to this booking?");
    }

    // prints a short summary line for each reservation created in one session
    public void printBookingSessionSummary(java.util.List<Reservation> reservations) {
        System.out.println("\n=== Booking Session Summary ===");
        System.out.println("Total rooms booked: " + reservations.size());
        for (Reservation r : reservations) {
            System.out.println("  - " + r.getRoomTypeRequested()
                + "  | Confirmation No.: " + r.getConfirmationNumber()
                + "  | Reservation ID: " + r.getReservationId());
        }
        System.out.println("================================");
    }

    // DISPLAY / OUTPUT METHODS
    public void printReservationDetails(Reservation r) {
        System.out.println("\n--- Reservation Details ---");
        System.out.println("Reservation ID     : " + r.getReservationId());
        System.out.println("Confirmation No.   : " + r.getConfirmationNumber());
        System.out.println("Guest ID           : " + r.getGuestId());
        System.out.println("Room Type          : " + r.getRoomTypeRequested());
        System.out.println("No. of Guests      : " + r.getNumberOfGuests());
        System.out.println("No. of Nights      : " + r.getNumberOfNights());
        System.out.println("Reservation Type   : " + r.getReservationType());
        System.out.println("Status             : " + r.getStatus());
        System.out.println("Expected Check-In  : " + r.getTimestamps().getExpectedCheckInDate());
        System.out.println("Expected Check-Out : " + r.getTimestamps().getExpectedCheckOutDate());
        System.out.println("Registered At      : " + r.getTimestamps().getRegistrationTimestamp());
        System.out.println("----------------------------");
    }

    public void listAllReservations(String[][] data) {
        System.out.println("\n--- Current Reservation Queue ---");
        if (data.length <= 1) {
            System.out.println("  No reservations in queue.");
            return;
        }
        printSimpleTable(data);
    }

    public void printQueuePosition(String confirmationNumber, int position) {
        if (position == -1) {
            printNotFound();
            return;
        }
        System.out.println("\nConfirmation No. " + confirmationNumber + " -> Queue Position #" + position);
    }

    public void printReport(String[][] data, String title) {
        System.out.println("\n--- " + title + " ---");
        if (data.length <= 1) {
            System.out.println("  No data available.");
            return;
        }
        printSimpleTable(data);
    }

    // MESSAGE METHODS
    public void printSuccess() {
        System.out.println("\n  ✓ Operation successful!");
    }

    public void printNotFound() {
        System.out.println("\n  ✗ Record not found!");
    }

    public void printCannotCheckIn() {
        System.out.println("\n  ✗ Cannot check in. Expected check-in date not yet reached!");
    }

    public void printRoomNotAvailable() {
        System.out.println("\n  ✗ No available room for the requested room type!");
    }

    public void printCancelled() {
        System.out.println("\n  ✓ Reservation cancelled successfully!");
    }

    public void printExitMessage() {
        System.out.println("\n  Exiting Walk-In Registration & Booking Module. Goodbye!");
    }

    public void printInvalidChoice() {
        System.out.println("\n  ✗ Invalid choice! Please try again.");
    }

    // HELPER METHODS
    private int inputIntChoice(String prompt, int min, int max) {
        int choice = -1;
        while (choice < min || choice > max) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
                if (choice < min || choice > max)
                    System.out.println("  ✗ Please enter a number between " + min + " and " + max + "!");
            } else {
                System.out.println("  ✗ Invalid input! Please enter a number.");
                scanner.nextLine();
            }
        }
        System.out.println();
        return choice;
    }

    // simple println-based table (replaces old Table.printTable dependency)
    private void printSimpleTable(String[][] data) {
        if (data.length == 0) return;

        int[] widths = new int[data[0].length];
        for (String[] row : data) {
            for (int col = 0; col < row.length; col++) {
                if (row[col] != null && row[col].length() > widths[col]) {
                    widths[col] = row[col].length();
                }
            }
        }

        for (int r = 0; r < data.length; r++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < data[r].length; col++) {
                String cell = data[r][col] == null ? "" : data[r][col];
                sb.append(String.format("%-" + (widths[col] + 3) + "s", cell));
            }
            System.out.println(sb.toString());

            // underline after header row
            if (r == 0) {
                int totalWidth = 0;
                for (int w : widths) totalWidth += w + 3;
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < totalWidth; i++) line.append('-');
                System.out.println(line.toString());
            }
        }
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}