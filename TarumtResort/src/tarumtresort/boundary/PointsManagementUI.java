package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Member;
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

public class PointsManagementUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Scanner scanner = new Scanner(System.in);

    public PointsManagementUI() {
    }

    public PointsManagementUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public int getMenuChoice() {
        ConsoleUtil.clearScreen();
        System.out.println();
        System.out.println("========================================");
        System.out.println("   POINTS & REDEMPTION MANAGEMENT");
        System.out.println("========================================");
        System.out.println(" 1. View Member Points Balance");
        System.out.println(" 2. Earn Points");
        System.out.println(" 3. Request Reward Redemption");
        System.out.println(" 4. Run Expiry Check (auto removal)");
        System.out.println(" 5. View Transaction History");
        System.out.println(" 6. Member Tier Progression");
        System.out.println(" 7. Generate Expiry Alerts (7 days)");
        System.out.println(" 8. View Notifications");
        System.out.println(" 9. Process Redemption Requests");
        System.out.println("10. Exit");
        System.out.println("----------------------------------------");
        return readInt("Enter your choice");
    }

    /** Lists the members and returns the chosen member id, or null. */
    public String selectMember(LinkedListInterface<Member> members) {
        if (members.isEmpty()) {
            System.out.println("No members registered yet.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        System.out.println();
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            System.out.printf(" %d. %s (Tier: %s)%n", i + 1, m.getMemberId(), m.getTier());
        }
        System.out.println(" 0. Cancel");
        int index = readInt("Select a member") - 1;
        if (index < 0) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        if (index >= members.size()) {
            ConsoleUtil.printError("Invalid selection.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return members.get(index).getMemberId();
    }

    /** Lists the rewards and returns the chosen reward id, or null. */
    public String selectReward(LinkedListInterface<Reward> rewards) {
        if (rewards.isEmpty()) {
            System.out.println("No rewards in the catalogue.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        System.out.println();
        String[][] rows = new String[rewards.size()][3];
        for (int i = 0; i < rewards.size(); i++) {
            Reward r = rewards.get(i);
            rows[i] = new String[] {
                    String.valueOf(i + 1),
                    r.getName(),
                    r.getPointCost() + " pts"
            };
        }
        TablePrinter.displayTable(new String[] { "#", "Reward", "Points" }, rows);
        System.out.println(" 0. Cancel");
        int index = readInt("Select a reward") - 1;
        if (index < 0) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        if (index >= rewards.size()) {
            ConsoleUtil.printError("Invalid selection.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return rewards.get(index).getRewardId();
    }

    public int inputAmount() {
        int amount = readInt("Points to award (0 to cancel)");
        if (amount == 0) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return 0;
        }
        while (amount < 0) {
            ConsoleUtil.printError("Amount must be positive.");
            amount = readInt("Points to award (0 to cancel)");
            if (amount == 0) {
                System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
                return 0;
            }
        }
        return amount;
    }

    public String inputDescription() {
        System.out.print("Description (0 to cancel): ");
        String desc = scanner.nextLine().trim();
        if (desc.equals("0")) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return desc;
    }

    public void displayBalance(Member member, int balance) {
        System.out.printf("Member %s (Tier: %s) - Available balance: %d pts%n",
                member.getMemberId(), member.getTier(), balance);
    }

    public void displayTransactions(LinkedListInterface<PointTransaction> txs) {
        if (txs.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        System.out.println();
        String[][] rows = new String[txs.size()][7];
        for (int i = 0; i < txs.size(); i++) {
            PointTransaction t = txs.get(i);
            rows[i] = new String[] {
                    t.getTransactionId(),
                    t.getDate().format(DATE_FMT),
                    truncate(t.getDescription(), 22),
                    String.valueOf(t.getPointChange()),
                    String.valueOf(t.getRemainingPoints()),
                    t.getExpiryDate().format(DATE_FMT),
                    statusOf(t)
            };
        }
        TablePrinter.displayTable(
                new String[] { "Tx ID", "Earned", "Description", "Change", "Remaining", "Expires", "Status" },
                rows);
    }

    public void displayNotifications(LinkedListInterface<Notification> list) {
        if (list.isEmpty()) {
            System.out.println("No notifications for this member.");
            return;
        }
        System.out.println();
        for (int i = 0; i < list.size(); i++) {
            Notification n = list.get(i);
            System.out.printf(" %d. [%s] %s (%s)%n",
                    i + 1, n.isRead() ? "READ" : "UNREAD",
                    n.getMessage(), n.getDate().format(DATE_FMT));
        }
    }

    /** @return true if the user wants to mark all notifications as read. */
    public boolean confirmMarkAllRead() {
        System.out.print("Mark all as read? (y/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("y");
    }

    /** Lists pending requests and returns the chosen redemption id, or null. */
    public String selectPendingRequest(LinkedListInterface<RedemptionRecord> pending) {
        if (pending.isEmpty()) {
            System.out.println("No pending redemption requests.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        System.out.println();
        for (int i = 0; i < pending.size(); i++) {
            RedemptionRecord r = pending.get(i);
            System.out.printf(" %d. %s - member %s, reward %s (%s)%n",
                    i + 1, r.getRedemptionId(), r.getMemberId(), r.getRewardId(),
                    r.getRedeemedDate().format(DATE_FMT));
        }
        System.out.println(" 0. Cancel");
        int index = readInt("Select a request to process") - 1;
        if (index < 0) {
            System.out.println("Operation cancelled.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        if (index >= pending.size()) {
            ConsoleUtil.printError("Invalid selection.");
        ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return pending.get(index).getRedemptionId();
    }

    /** Prints an error message in red and waits for the user to press Enter. */
    public void showError(String message) {
        ConsoleUtil.printError(message);
        pause();
    }

    /** @return "a" to approve, "r" to reject, or null for anything else. */
    public String approveOrReject() {
        System.out.print("Approve or reject? (a/r/c to cancel): ");
        String answer = scanner.nextLine().trim();
        if (answer.equalsIgnoreCase("a")) {
            return "a";
        }
        if (answer.equalsIgnoreCase("r")) {
            return "r";
        }
        return null;
    }

    /** Prints a message and waits for the user to press Enter. */
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

    private String statusOf(PointTransaction t) {
        if (t.getExpiryDate() != null && !LocalDateTime.now().isBefore(t.getExpiryDate())) {
            return "EXPIRED";
        }
        if (t.getRemainingPoints() < t.getPointChange()) {
            return "PARTIAL";
        }
        return "VALID";
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

    private static String truncate(String text, int width) {
        if (text == null || text.length() <= width) {
            return text;
        }
        return text.substring(0, width - 3) + "...";
    }
}