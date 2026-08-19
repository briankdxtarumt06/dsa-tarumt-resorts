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

    private static final int BANNER_WIDTH = 60;
    private static final String BANNER_LINE = "=".repeat(BANNER_WIDTH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Scanner scanner = new Scanner(System.in);

    public PriorityReservationUI() {
    }

    public PriorityReservationUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public static void printBanner(String title) {
        System.out.println("\n" + BANNER_LINE);
        System.out.println(center(title, BANNER_WIDTH));
        System.out.println(BANNER_LINE);
    }

    public static void printSeparator() {
        System.out.println(BANNER_LINE);
    }

    public static void printSection(String text) {
        if (text == null) {
            text = "";
        }
        if (text.isEmpty()) {
            System.out.println("\n" + BANNER_LINE);
            return;
        }
        String core = " " + text + " ";
        int available = BANNER_WIDTH - core.length();
        int left = available / 2;
        System.out.println("\n" + "=".repeat(left) + core + "=".repeat(available - left));
    }

    private static String center(String text, int width) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= width) {
            return text;
        }
        int pad = width - text.length();
        int left = pad / 2;
        return " ".repeat(left) + text + " ".repeat(pad - left);
    }

    public int printPriorityListMenu(LinkedListInterface<PriorityReservation> priorityReservationsList, int page,
            int pageCount, boolean hasFilter) {
        clearScreen();
        printBanner("PRIORITY RESERVATION (Page " + (page + 1) + " of " + pageCount + ")");
        if (priorityReservationsList.isEmpty()) {
            System.out.println("  (No priority reservations)");
        } else {
            String[] header = new String[] { "No", "Reservation ID", "Priority Level", "Overridden By",
                    "Override Reason" };
            String[][] rows = new String[priorityReservationsList.size()][5];
            for (int i = 0; i < priorityReservationsList.size(); i++) {
                PriorityReservation pr = priorityReservationsList.get(i);
                rows[i] = new String[] {
                        String.valueOf(i + 1),
                        pr.getReservationId(),
                        pr.getPriorityLevel().name(),
                        pr.getOverriddenBy(),
                        pr.getOverrideReason()
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        printSection("Actions");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Add Priority Reservation");
        System.out.println("  " + action++ + ". Delete Priority Reservation");
        System.out.println("  " + action++ + ". View VIP Queue");
        System.out.println("  " + action++ + ". Filter by Priority Level");
        System.out.println("  " + action++ + ". Search Priority Reservation");

        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        if (hasFilter) {
            System.out.println("  " + action++ + ". Clear Filter");
        }
        System.out.println("  0. Back");
        printSeparator();
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int getPriorityActionChoice() {
        printSection("Record Actions");
        System.out.println("  1. Update Priority Level");
        System.out.println("  2. Delete Priority Record");
        System.out.println("  3. View VIP Queue Position");
        System.out.println("  0. Back to List");
        printSeparator();
        return inputIntChoice("Enter choice", 0, 3);
    }

    public static void printDetails(String[][] details) {
        if (details == null || details.length == 0) {
            return;
        }

        int keyWidth = 0;
        for (String[] pair : details) {
            if (pair[0] != null && pair[0].length() > keyWidth) {
                keyWidth = pair[0].length();
            }
        }

        printSection("Details");
        for (String[] pair : details) {
            System.out.println(String.format("%-" + (keyWidth + 3) + "s: %s",
                    pair[0] == null ? "" : pair[0],
                    pair.length > 1 && pair[1] != null ? pair[1] : "-"));
        }
        printSeparator();
    }

    public void printPriorityDetail(PriorityReservation pr, Reservation r) {
        printDetails(new String[][] {
                { "Reservation ID", pr.getReservationId() },
                { "Guest ID", r == null ? "-" : r.getGuestId() },
                { "Priority Level", pr.getPriorityLevel().name() },
                { "Reservation Status", (r == null || r.getStatus() == null) ? "-" : r.getStatus().name() },
                { "Room Type",
                        (r == null || r.getRoomTypeRequested() == null) ? "-" : r.getRoomTypeRequested().name() },
                { "Overridden By",
                        (pr.getOverriddenBy() == null || pr.getOverriddenBy().isEmpty()) ? "-" : pr.getOverriddenBy() },
                { "Override Reason",
                        (pr.getOverrideReason() == null || pr.getOverrideReason().isEmpty()) ? "-"
                                : pr.getOverrideReason() }
        });
    }

    // VIP QUEUE (waiting members only)
    public void displayVIPQueue(LinkedListInterface<Reservation> queue,
            LinkedListInterface<PriorityReservation> priorities) {
        ConsoleUtil.clearScreen();
        printBanner("VIP QUEUE (Waiting Members)");
        if (queue.isEmpty()) {
            System.out.println("  No VIP members waiting in the queue.");
            printSeparator();
            return;
        }
        String[] header = new String[] { "Position", "Reservation ID", "Guest ID", "Priority", "Room Type","Registered" };
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
        TablePrinter.displayTable(header, rows);
        printSeparator();
    }

    // OVERRIDE
    public PriorityLevel selectPriorityLevel(String prompt) {
        System.out.println("\nSelect Priority Level:");
        System.out.println("  1. PENALTY");
        System.out.println("  2. SLIVER");
        System.out.println("  3. GOLD");
        System.out.println("  4. PLATINUM");
        System.out.println("  5. DIAMOND");
        System.out.println("  6. EMERGENCY");
        System.out.println("  0. Cancel");
        int choice = inputIntChoice(prompt, 0, 6);
        if (choice == 0) {
            showMessage("Operation cancelled.");
            return null;
        }
        return switch (choice) {
            case 1 -> PriorityLevel.PENALTY;
            case 2 -> PriorityLevel.SLIVER;
            case 3 -> PriorityLevel.GOLD;
            case 4 -> PriorityLevel.PLATINUM;
            case 5 -> PriorityLevel.DIAMOND;
            default -> PriorityLevel.EMERGENCY;
        };
    }

    public void displayOverridePreview(PriorityReservation pr, PriorityLevel newLevel,
            String staffId, String reason) {
        printSection("Override Preview");
        System.out.println("  Reservation id  : " + pr.getReservationId());
        System.out.println("  Current priority: " + pr.getPriorityLevel()
                + " (rank " + pr.getPriorityLevel().getRank() + ")");
        System.out.println("  New priority    : " + newLevel
                + " (rank " + newLevel.getRank() + ")");
        System.out.println("  Overridden by   : " + staffId);
        System.out.println("  Reason          : " + reason);

        int diff = newLevel.getRank() - pr.getPriorityLevel().getRank();
        if (diff > 0) {
            ConsoleUtil.printSuccess("  Effect          : PROMOTED - moves up the VIP queue");
        } else if (diff < 0) {
            ConsoleUtil.printWarning("  Effect          : DEMOTED - moves down the VIP queue");
        } else {
            System.out.println("  Effect          : No change in queue order");
        }
        printSeparator();
    }

    // INPUT / MESSAGES
    public int inputListIndex(String label, int max) {
        return inputIntChoice("Enter " + label + " number (0 = cancel)", 0, max);
    }

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

    public void showError(String message) {
        ConsoleUtil.printError(message);
        pause();
    }

    public void showMessage(String message) {
        System.out.println(message);
        pause();
    }

    public void pause() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }

    private int inputIntChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            if (!scanner.hasNextLine()) {
                System.out.println("No more input. Exiting.");
                System.exit(0);
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                ConsoleUtil.printError("Input cannot be empty! Please enter a number.");
                continue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    System.out.println();
                    return value;
                }
            } catch (NumberFormatException e) {
                // retry
            }
            ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
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

    private void clearScreen() {
        ConsoleUtil.clearScreen();
    }

    public void pressEnterToContinue() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }
}
