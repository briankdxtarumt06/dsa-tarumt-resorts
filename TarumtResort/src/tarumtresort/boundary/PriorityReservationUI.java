package tarumtresort.boundary;

import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.PriorityReservation;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

public class PriorityReservationUI {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Scanner scanner = new Scanner(System.in);

    public PriorityReservationUI() {
    }

    public PriorityReservationUI(Scanner scanner) {
        this.scanner = scanner;
    }

    // MENU

    public int getMenuChoice() {
        ConsoleUtil.clearScreen();
        System.out.println();
        System.out.println("========================================");
        System.out.println("   PRIORITY RESERVATION");
        System.out.println("========================================");
        System.out.println(" 1. View VIP Queue");
        System.out.println(" 2. List Priority Reservations");
        System.out.println(" 3. Filter by Priority Level");
        System.out.println(" 4. Override Priority Level");
        System.out.println(" 5. Exit");
        System.out.println("----------------------------------------");
        return readInt("Enter your choice");
    }

    // SELECTORS 
    public PriorityLevel selectPriorityLevel(String prompt) {
        PriorityLevel[] levels = PriorityLevel.values();
        System.out.println();
        for (int i = 0; i < levels.length; i++) {
            System.out.printf(" %d. %-10s (rank %d)%n", i + 1, levels[i].name(), levels[i].getRank());
        }
        System.out.println(" 0. Cancel");

        int index = readInt(prompt) - 1;
        if (index < 0) {
            System.out.println("Operation cancelled.");
            pause();
            return null;
        }
        if (index >= levels.length) {
            ConsoleUtil.printError("Invalid selection.");
            pause();
            return null;
        }
        return levels[index];
    }

    public String selectPriorityReservation(LinkedListInterface<PriorityReservation> list, String prompt) {
        if (list.isEmpty()) {
            System.out.println("No priority reservations recorded yet.");
            pause();
            return null;
        }
        System.out.println();
        for (int i = 0; i < list.size(); i++) {
            PriorityReservation pr = list.get(i);
            System.out.printf(" %d. %s (Priority: %s)%n", i + 1, pr.getReservationId(), pr.getPriorityLevel());
        }
        System.out.println(" 0. Cancel");

        int index = readInt(prompt) - 1;
        if (index < 0) {
            System.out.println("Operation cancelled.");
            pause();
            return null;
        }
        if (index >= list.size()) {
            ConsoleUtil.printError("Invalid selection.");
            pause();
            return null;
        }
        return list.get(index).getReservationId();
    }

    // DISPLAY 

    public void displayVIPQueue(LinkedListInterface<Reservation> queue,
            LinkedListInterface<PriorityReservation> priorities) {
        if (queue.isEmpty()) {
            System.out.println("No VIP reservations in the queue.");
            return;
        }
        System.out.println();
        String[][] rows = new String[queue.size()][6];
        for (int i = 0; i < queue.size(); i++) {
            Reservation r = queue.get(i);
            PriorityLevel level = findLevel(priorities, r.getReservationId());
            rows[i] = new String[] {
                    String.valueOf(i + 1),
                    r.getReservationId(),
                    r.getGuestId(),
                    level == null ? "-" : level.name(),
                    r.getRoomTypeRequested() == null ? "-" : r.getRoomTypeRequested().name(),
                    formatRegistration(r)
            };
        }
        TablePrinter.displayTable(
                new String[] { "Pos", "Reservation ID", "Guest ID", "Priority", "Room Type", "Registered" }, rows);
        System.out.println("Higher rank is served first. Same rank = earlier registration wins.");
    }

    public void displayPriorityReservations(LinkedListInterface<PriorityReservation> list) {
        if (list.isEmpty()) {
            System.out.println("No priority reservations recorded yet.");
            return;
        }
        System.out.println();
        String[][] rows = new String[list.size()][5];
        for (int i = 0; i < list.size(); i++) {
            PriorityReservation pr = list.get(i);
            rows[i] = new String[] {
                    pr.getReservationId(),
                    pr.getPriorityLevel().name(),
                    String.valueOf(pr.getPriorityLevel().getRank()),
                    orDash(pr.getOverriddenBy()),
                    orDash(pr.getOverrideReason())
            };
        }
        TablePrinter.displayTable(
                new String[] { "Reservation ID", "Priority", "Rank", "Overridden By", "Reason" }, rows);
    }

    public void displayDetails(PriorityReservation pr) {
        System.out.println();
        System.out.println("Reservation id  : " + pr.getReservationId());
        System.out.println("Priority level  : " + pr.getPriorityLevel()
                + " (rank " + pr.getPriorityLevel().getRank() + ")");
        System.out.println("Overridden by   : " + orDash(pr.getOverriddenBy()));
        System.out.println("Override reason : " + orDash(pr.getOverrideReason()));
    }

    public void displayOverridePreview(PriorityReservation pr, PriorityLevel newLevel,
            String staffId, String reason) {
        System.out.println();
        System.out.println("Reservation id  : " + pr.getReservationId());
        System.out.println("Current priority: " + pr.getPriorityLevel()
                + " (rank " + pr.getPriorityLevel().getRank() + ")");
        System.out.println("New priority    : " + newLevel
                + " (rank " + newLevel.getRank() + ")");
        System.out.println("Overridden by   : " + staffId);
        System.out.println("Reason          : " + reason);

        int diff = newLevel.getRank() - pr.getPriorityLevel().getRank();
        if (diff > 0) {
            ConsoleUtil.printSuccess("Effect          : PROMOTED - moves up the VIP queue");
        } else if (diff < 0) {
            ConsoleUtil.printWarning("Effect          : DEMOTED - moves down the VIP queue");
        } else {
            System.out.println("Effect          : No change in queue order");
        }
    }

    // MESSAGES
    public void showError(String message) {
        ConsoleUtil.printError(message);
        pause();
    }

    public void showMessage(String message) {
        System.out.println(message);
        pause();
    }

    public void show(String message) {
        System.out.println(message);
    }

    public void pause() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }

    // INPUT HELPERS

    public boolean confirm(String message) {
        while (true) {
            System.out.print(message + " (y/n): ");
            if (!scanner.hasNextLine()) {
                System.out.println("No more input. Exiting.");
                System.exit(0);
            }
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            ConsoleUtil.printError("Please enter y or n.");
        }
    }

    public String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            if (!scanner.hasNextLine()) {
                System.out.println("No more input. Exiting.");
                System.exit(0);
            }
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            ConsoleUtil.printError("Input cannot be empty.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            if (!scanner.hasNextLine()) {
                System.out.println("No more input. Exiting.");
                System.exit(0);
            }
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("Please enter a valid number.");
            }
        }
    }

    private PriorityLevel findLevel(LinkedListInterface<PriorityReservation> priorities, String reservationId) {
        for (int i = 0; i < priorities.size(); i++) {
            if (priorities.get(i).getReservationId().equals(reservationId)) {
                return priorities.get(i).getPriorityLevel();
            }
        }
        return null;
    }

    private String formatRegistration(Reservation r) {
        if (r.getTimestamps() == null || r.getTimestamps().getRegistrationTimestamp() == null) {
            return "-";
        }
        return r.getTimestamps().getRegistrationTimestamp().format(TIME_FMT);
    }

    private String orDash(String value) {
        return (value == null || value.isEmpty()) ? "-" : value;
    }
}