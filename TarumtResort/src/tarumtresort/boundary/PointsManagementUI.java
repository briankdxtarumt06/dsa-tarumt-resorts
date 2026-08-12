package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.control.PointsController;
import tarumtresort.dao.MemberDAO;
import tarumtresort.dao.NotificationDAO;
import tarumtresort.dao.PointTransactionDAO;
import tarumtresort.dao.RedemptionRecordDAO;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Member;
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.Reward;

public class PointsManagementUI {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Scanner scanner;
    private final PointsController controller;

    public PointsManagementUI() {
        this(new Scanner(System.in));
    }

    public PointsManagementUI(Scanner scanner) {
        this.scanner = scanner;
        MemberDAO memberDAO = new MemberDAO();
        PointTransactionDAO pointTransactionDAO = new PointTransactionDAO();
        RewardDAO rewardDAO = new RewardDAO();
        RedemptionRecordDAO redemptionRecordDAO = new RedemptionRecordDAO();
        NotificationDAO notificationDAO = new NotificationDAO();
        this.controller = new PointsController(memberDAO, pointTransactionDAO,
                rewardDAO, redemptionRecordDAO, notificationDAO);
        // Auto-check for points expiring soon so alerts appear when the
        // module opens, not only when the user runs the generate option.
        String alert = controller.generateExpiryAlerts(LocalDateTime.now());
        if (!alert.startsWith("No new")) {
            System.out.println(alert);
        }
    }

    public static void main(String[] args) {
        new PointsManagementUI().run();
    }

    public void run() {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter your choice");
            switch (choice) {
                case 1:
                    viewBalance();
                    break;
                case 2:
                    earnPoints();
                    break;
                case 3:
                    redeemReward();
                    break;
                case 4:
                    runExpiryCheck();
                    break;
                case 5:
                    viewHistory();
                    break;
                case 6:
                    viewTierProgress();
                    break;
                case 7:
                    generateAlerts();
                    break;
                case 8:
                    viewNotifications();
                    break;
                case 9:
                    System.out.println("Returning to main menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 - 9.");
            }
        } while (choice != 6);
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("   POINTS & REDEMPTION MANAGEMENT");
        System.out.println("========================================");
        System.out.println(" 1. View Member Points Balance");
        System.out.println(" 2. Earn Points");
        System.out.println(" 3. Redeem Reward (oldest points first)");
        System.out.println(" 4. Run Expiry Check (auto removal)");
        System.out.println(" 5. View Transaction History");
        System.out.println(" 6. Member Tier Progression");
        System.out.println(" 7. Generate Expiry Alerts (7 days)");
        System.out.println(" 8. View Notifications");
        System.out.println(" 9. Exit");
        System.out.println("----------------------------------------");
    }

    private void viewBalance() {
        String memberId = selectMember();
        if (memberId == null) {
            return;
        }
        Member member = controller.findMember(memberId);
        int balance = controller.getAvailableBalance(memberId, LocalDateTime.now());
        System.out.printf("Member %s (Tier: %s) - Available balance: %d pts%n",
                member.getMemberId(), member.getTier(), balance);
    }

    private void earnPoints() {
        String memberId = selectMember();
        if (memberId == null) {
            return;
        }
        int amount = readInt("Points to award (positive)");
        if (amount <= 0) {
            System.out.println("Amount must be positive.");
            return;
        }
        System.out.print("Description (optional): ");
        String description = scanner.nextLine().trim();
        System.out.println(controller.earnPoints(memberId, amount, description, LocalDateTime.now()));
    }

    private void redeemReward() {
        String memberId = selectMember();
        if (memberId == null) {
            return;
        }
        String rewardId = selectReward();
        if (rewardId == null) {
            return;
        }
        System.out.println(controller.redeemPoints(memberId, rewardId, LocalDateTime.now()));
    }

    private void runExpiryCheck() {
        String memberId = selectMember();
        if (memberId == null) {
            return;
        }
        System.out.println(controller.expirePoints(memberId, LocalDateTime.now()));
    }

    private void viewHistory() {
        String memberId = selectMember();
        if (memberId == null) {
            return;
        }
        if (controller.getTransactions(memberId).isEmpty()) {
            System.out.println("No transactions found for " + memberId + ".");
            return;
        }
        System.out.println();
        System.out.printf("%-10s | %-12s | %-22s | %7s | %9s | %-12s | %s%n",
                "Tx ID", "Earned", "Description", "Change", "Remaining", "Expires", "Status");
        System.out.println("---------------------------------------------------------------------------");
        for (int i = 0; i < controller.getTransactions(memberId).size(); i++) {
            PointTransaction t = controller.getTransactions(memberId).get(i);
            System.out.printf("%-10s | %-12s | %-22s | %7d | %9d | %-12s | %s%n",
                    t.getTransactionId(),
                    t.getDate().format(DATE_FMT),
                    truncate(t.getDescription(), 22),
                    t.getPointChange(),
                    t.getRemainingPoints(),
                    t.getExpiryDate().format(DATE_FMT),
                    statusOf(t));
        }
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

    /**
     * Prints the member list and returns the chosen member id, or null if
     * there are no members or the selection is invalid.
     */
    private String selectMember() {
        if (controller.getMembers().isEmpty()) {
            System.out.println("No members registered yet.");
            return null;
        }
        System.out.println();
        for (int i = 0; i < controller.getMembers().size(); i++) {
            Member m = controller.getMembers().get(i);
            System.out.printf(" %d. %s (Tier: %s)%n", i + 1, m.getMemberId(), m.getTier());
        }
        int index = readInt("Select a member") - 1;
        if (index < 0 || index >= controller.getMembers().size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return controller.getMembers().get(index).getMemberId();
    }

    /**
     * Prints the reward catalogue and returns the chosen reward id, or null
     * if there are no rewards or the selection is invalid.
     */
    private String selectReward() {
        if (controller.getRewards().isEmpty()) {
            System.out.println("No rewards in the catalogue.");
            return null;
        }
        System.out.println();
        for (int i = 0; i < controller.getRewards().size(); i++) {
            Reward r = controller.getRewards().get(i);
            System.out.printf(" %d. %-20s - %d pts%n", i + 1, r.getName(), r.getPointCost());
        }
        int index = readInt("Select a reward") - 1;
        if (index < 0 || index >= controller.getRewards().size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return controller.getRewards().get(index).getRewardId();
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
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String truncate(String text, int width) {
        if (text == null || text.length() <= width) {
            return text;
        }
        return text.substring(0, width - 3) + "...";
    }

    private void generateAlerts() {
        System.out.println(controller.generateExpiryAlerts(LocalDateTime.now()));
    }

    private void viewNotifications() {
        String memberId = selectMember();
        if (memberId == null) {
            return;
        }
        Member member = controller.findMember(memberId);
        if (member == null || member.getGuestId() == null) {
            System.out.println("Member has no guest account linked.");
            return;
        }
        LinkedListInterface<Notification> list = controller.getNotifications(member.getGuestId());
        if (list.isEmpty()) {
            System.out.println("No notifications for " + memberId + ".");
            return;
        }
        System.out.println();
        for (int i = 0; i < list.size(); i++) {
            Notification n = list.get(i);
            System.out.printf(" %d. [%s] %s (%s)%n",
                    i + 1, n.isRead() ? "READ" : "UNREAD",
                    n.getMessage(), n.getDate().format(DATE_FMT));
        }
        System.out.print("Mark all as read? (y/n): ");
        String answer = scanner.nextLine().trim();
        if (answer.equalsIgnoreCase("y")) {
            for (int i = 0; i < list.size(); i++) {
                controller.markNotificationRead(list.get(i).getNotificationId());
            }
            System.out.println("All notifications marked as read.");
        }
    }

    private void viewTierProgress() {
        String memberId = selectMember();
        if (memberId == null) {
            return;
        }
        System.out.println(controller.getTierProgress(memberId));
    }
}

