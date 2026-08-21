package tarumtresort.boundary;

import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Inquiry;
import tarumtresort.entity.Payment;
import tarumtresort.entity.Reservation;
import tarumtresort.entity.Room;
import tarumtresort.entity.Task;
import tarumtresort.entity.enums.InquiryStatus;
import tarumtresort.entity.enums.InquiryType;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.utility.TablePrinter;

/**
 *
 * @author Wen Ling
 *
 */
public class InquiryUI {

    private Scanner scanner = new Scanner(System.in);

    public InquiryUI() {
    }

    public InquiryUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public int getMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  FRONT-DESK INQUIRY MODULE");
        System.out.println("========================================");
        System.out.println("  1. Create Inquiry");
        System.out.println("  2. Process Next Inquiry");
        System.out.println("  3. View Pending Queue");
        System.out.println("  4. Cancel Inquiry");
        System.out.println("  5. View All Inquiries");
        System.out.println("  6. Search Inquiry");
        System.out.println("  7. Generate Report");
        System.out.println("  0. Back to Main Menu");
        System.out.println("========================================");
        return readIntInRange("Enter choice (0-7): ", 0, 7);
}

    public int getSearchMenuChoice() {
        System.out.println("\n--- Search Inquiry ---");
        System.out.println("  1. By Inquiry ID");
        System.out.println("  2. By Confirmation Number");
        System.out.println("  0. Back");
        return readIntInRange("Enter choice (0-2): ", 0, 2);
    }

    public int getReportMenuChoice() {
        System.out.println("\n--- Reports ---");
        System.out.println("  1. Pending Inquiry Overview Report");
        System.out.println("  2. Room Type Inquiry Distribution Report");
        System.out.println("  0. Back");
        return readIntInRange("Enter choice (0-2): ", 0, 2);
    }

    public String inputConfirmationNumber() {
        System.out.print("Enter 8-digit Confirmation Number: ");
        return scanner.nextLine().trim();
    }

    public String inputInquiryId() {
        System.out.print("Enter Inquiry ID: ");
        return scanner.nextLine().trim();
    }

    public InquiryType inputInquiryType() {
        System.out.println("Select Query Type:");
        InquiryType[] types = InquiryType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + (i + 1) + ". " + types[i] + " (" + types[i].getPriority() + ")");
        }
        int choice = readIntInRange("Enter choice (1-" + types.length + "): ", 1, types.length);
        return types[choice - 1];
    }

    public InquiryStatus inputInquiryStatusFilter() {
        System.out.println("Filter by Status:");
        InquiryStatus[] statuses = InquiryStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.println("  " + (i + 1) + ". " + statuses[i]);
        }
        System.out.println("  0. All Statuses");
        int choice = readIntInRange("Enter choice (0-" + statuses.length + "): ", 0, statuses.length);
        return choice == 0 ? null : statuses[choice - 1];
    }

    public String inputDescription() {
        System.out.print("Enter Description: ");
        return scanner.nextLine().trim();
    }

    public boolean inputConfirmation(String message) {
        System.out.print(message + " (Y/N): ");
        return scanner.nextLine().trim().equalsIgnoreCase("Y");
    }

    public void printInquiryDetails(String details) {
        System.out.println("\n--- Inquiry Details ---");
        System.out.println(details);
    }

    public String buildInquiryDetails(Inquiry inquiry) {
        StringBuilder sb = new StringBuilder();
        sb.append("Inquiry ID       : ").append(inquiry.getInquiryId()).append("\n");
        sb.append("Confirmation No. : ").append(inquiry.getConfirmationNumber()).append("\n");
        sb.append("Type             : ").append(inquiry.getInquiryType())
                .append(" (").append(inquiry.getInquiryType().getPriority()).append(")\n");
        sb.append("Description      : ").append(inquiry.getDescription()).append("\n");
        sb.append("Status           : ").append(inquiry.getStatus()).append("\n");
        sb.append("Created Time     : ").append(inquiry.getCreatedTime()).append("\n");
        if (inquiry.getResolvedTime() != null) {
            sb.append("Resolved Time    : ").append(inquiry.getResolvedTime()).append("\n");
        }
        return sb.toString();
    }

    public String buildRoomAvailabilityInfo(Reservation reservation, int availableCount, int totalRooms, Room assignedRoom) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reservation Status : ").append(reservation.getStatus()).append("\n");
        sb.append("Requested Room Type: ").append(reservation.getRoomTypeRequested()).append("\n");

        if (reservation.getStatus() == ReservationStatus.WAITING) {
            sb.append("Available Rooms of this Type: ").append(availableCount)
              .append(" out of ").append(totalRooms).append("\n");
        } else {
            if (assignedRoom != null) {
                sb.append("Assigned Room No.  : ").append(assignedRoom.getRoomNumber()).append("\n");
                sb.append("Room Status        : ").append(assignedRoom.getRoomStatus()).append("\n");
            }
        }
        return sb.toString();
    }

    public void printAdditionalInfo(Inquiry inquiry, Object extra) {
        System.out.println("\n--- Additional Information ---");

        if (extra == null) {
            if (inquiry.getInquiryType() == InquiryType.BILLINGDETAILS) {
                System.out.println("No payment record found yet (guest may not have checked out).");
            } else {
                System.out.println("No additional information found.");
            }
            return;
        }

        if (extra instanceof Guest) {
            Guest guest = (Guest) extra;
            System.out.println("Guest ID       : " + guest.getGuestId());
            System.out.println("Name           : " + guest.getName());
            System.out.println("IC/Passport    : " + guest.getIcOrPassport());
            System.out.println("Contact Number : " + guest.getContactNumber());
            System.out.println("Nationality    : " + guest.getNationality());
            System.out.println("Address        : " + guest.getAddress());
        } else if (extra instanceof Payment) {
            Payment payment = (Payment) extra;
            System.out.println("Payment ID     : " + payment.getPaymentID());
            StringBuilder resIds = new StringBuilder();
            LinkedListInterface<String> paymentResIds = payment.getReservationIds();
            if (paymentResIds != null) {
                for (int j = 0; j < paymentResIds.size(); j++) {
                    if (paymentResIds.get(j) != null) {
                        if (resIds.length() > 0) {
                            resIds.append(", ");
                        }
                        resIds.append(paymentResIds.get(j));
                    }
                }
            }
            if (resIds.length() == 0) {
                resIds.append("-");
            }
            System.out.println("Reservation ID : " + resIds);
            System.out.println("Room Charge    : RM " + String.format("%.2f", payment.getRoomCharge()));
            System.out.println("Service Charge : RM " + String.format("%.2f", payment.getServiceCharge()));
            System.out.println("Tax            : RM " + String.format("%.2f", payment.getTax()));
            System.out.println("Total Amount   : RM " + String.format("%.2f", payment.getTotalAmount()));
            System.out.println("Payment Method : " + payment.getPaymentMethod());
            System.out.println("Payment Status : " + payment.getPaymentStatus());
            System.out.println("Payment Date   : " + payment.getPaymentDateTime());
            if (payment.getRefundedAmount() > 0) {
                System.out.println("Refunded Amount: RM " + String.format("%.2f", payment.getRefundedAmount()));
                System.out.println("Refund Date    : " + payment.getRefundDateTime());
            }
        } else if (extra instanceof Task) {
            Task task = (Task) extra;
            System.out.println("Housekeeping Task Created: " + task.getTaskId());
            System.out.println("Room: " + task.getRoomId());
            System.out.println("Status: " + task.getTaskStatus());
            System.out.println("Scheduled Start: " + tarumtresort.utility.DateTimeUtil.readable(task.getStartDateTime()));
        } else {
            System.out.println(extra.toString());
        }
    }

    public void listAllInquiries(String[][] data) {
        if (data.length <= 1) {
            System.out.println("\nNo inquiries to display.");
            return;
        }
        System.out.println();
        printTable(data);
    }

    private void printTable(String[][] data) {
        if (data.length == 0) {
            return;
        }
        String[] header = data[0];
        String[][] rows = new String[data.length - 1][];
        System.arraycopy(data, 1, rows, 0, data.length - 1);
        TablePrinter.displayTable(header, rows);
    }

    public void printMessage(String message) {
        System.out.println("\n" + message);
    }

    public void printSuccess() {
        System.out.println("\n   Operation successful.");
    }

    public void printNotFound() {
        System.out.println("\n   Record not found.");
    }

    public void printCancelled() {
        System.out.println("\n  Inquiry cancelled.");
    }

    public void printExitMessage() {
        System.out.println("\nExiting Inquiry Module.");
    }

    public void printInvalidChoice() {
        System.out.println("\n Invalid choice! Please try again.");
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) {
                continue;
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to error below
            }
            System.out.println("Please enter a number between " + min + " and " + max + "!");
        }
    }
}