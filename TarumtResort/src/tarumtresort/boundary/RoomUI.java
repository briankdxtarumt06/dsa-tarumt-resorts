package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Room;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

public class RoomUI {

    private Scanner scanner = new Scanner(System.in);

    public int printRoomListMenu(LinkedListInterface<Room> pageList, int page, int pageCount, boolean hasFilter) {
        ConsoleUtil.clearScreen();
        System.out.println("\n==============================");
        System.out.println("  ROOM MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
        System.out.println("==============================");

        if (pageList.isEmpty()) {
            System.out.println("  (No room records)");
        } else {
            String[] header = {"No.", "Room ID", "Room No.", "Room Type", "Status", "Price/Night"};
            String[][] rows = new String[pageList.size()][6];
            for (int i = 0; i < pageList.size(); i++) {
                Room r = pageList.get(i);
                rows[i] = new String[]{
                    String.valueOf(i + 1), r.getRoomId(), r.getRoomNumber(),
                    r.getRoomType().toString(), r.getRoomStatus().toString(),
                    String.format("%.2f", r.getPricePerNight())
                };
            }
            TablePrinter.displayTable(header, rows);
        }

        System.out.println("==========Actions==========");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Filter by Room Type");
        System.out.println("  " + action++ + ". Filter by Room Status");
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

        System.out.println("===========================");
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int inputRoomTypeChoice() {
        System.out.println();
        System.out.println("==========Room Type==========");
        System.out.println("  1. STANDARD SINGLE");
        System.out.println("  2. STANDARD DOUBLE");
        System.out.println("  3. STANDARD TRIPLE");
        System.out.println("  4. DELUXE SINGLE");
        System.out.println("  5. DELUXE DOUBLE");
        System.out.println("  6. DELUXE TRIPLE");
        System.out.println("  7. SUITE");
        System.out.println("  0. Cancel");
        System.out.println("=============================");
        return inputIntChoice("Enter choice", 0, 7);
    }

    public int inputRoomStatusChoice() {
        System.out.println();
        System.out.println("==========Room Status==========");
        System.out.println("  1. AVAILABLE");
        System.out.println("  2. OCCUPIED");
        System.out.println("  3. CLEANING");
        System.out.println("  0. Cancel");
        System.out.println("===============================");
        return inputIntChoice("Enter choice", 0, 3);
    }

    public void printAvailableRoomList(String[][] data) {
        if (data.length <= 1) {
            System.out.println("\nNo available rooms found.");
            return;
        }

        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        for (int i = 1; i < data.length; i++) {
            rows[i - 1] = data[i];
        }

        System.out.println("\nAvailable Rooms:");
        TablePrinter.displayTable(header, rows);
    }

    public void printRoomDetails(Room room) {
        String[] header = {"Field", "Value"};
        String[][] rows = {
            {"Room ID", room.getRoomId()},
            {"Room Number", room.getRoomNumber()},
            {"Room Type", room.getRoomType().toString()},
            {"Room Status", room.getRoomStatus().toString()},
            {"Price Per Night", "RM " + String.format("%.2f", room.getPricePerNight())},
            {"Total Reservations on Record", String.valueOf(room.getReservations().size())}
        };
        TablePrinter.displayTable(header, rows);
    }

    public int printBookingTypeMenu(){
        System.out.println("==========Booking Type==========");
        System.out.println("  1. Walk-in");
        System.out.println("  2. Advance Booking");
        System.out.println("  0. Back");
        System.out.println("================================");
        return inputIntChoice("Enter choice", 0, 2);
    }

    public void printNoRecords() {
        ConsoleUtil.printError("No records to view!");
    }

    public int inputListIndex(String entityLabel, int max) {
        return inputIntChoice("Enter " + entityLabel + " number to view (0 = cancel)", 0, max);
    }

    private int inputIntChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
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
    
    public void pressEnterToContinue() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }
}