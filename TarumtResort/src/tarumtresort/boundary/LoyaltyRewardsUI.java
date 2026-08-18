package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.control.LoyaltyController;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.Tier;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

/**
 *
 * @author Brian
 *
 * Module driver for the Loyalty & Rewards subsystem.
 * Launched from TarumtResort via MainMenuUI case 5.
 *
 * Contains all four loyalty UI classes in one file:
 *   - LoyaltyRewardsUI     (module menu)
 *   - MemberManagementUI   (member CRUD + paginated list + profile)
 *   - PointsManagementUI   (points, transactions, notifications, redemptions)
 *   - RewardManagementUI   (reward catalogue CRUD)
 */
public class LoyaltyRewardsUI {

    private final Scanner scanner;

    public LoyaltyRewardsUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void run() {
        int choice;
        do {
            printMenu();
            choice = getIntInput("Enter choice (0-3): ", 0, 3);

            switch (choice) {
                case 1:
                    new LoyaltyController(scanner).runMemberMenu();
                    break;
                case 2:
                    new LoyaltyController(scanner).runRewardMenu();
                    break;
                case 3:
                    new LoyaltyController(scanner).runPointsMenu();
                    break;
                case 0:
                    System.out.println("\n  Returning to main menu...");
                    break;
                default:
                    System.out.println("\n  ✗ Invalid choice! Please try again.");
            }
        } while (choice != 0);
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  LOYALTY & REWARDS MODULE");
        System.out.println("========================================");
        System.out.println("  1. Member Management");
        System.out.println("  2. Reward Management");
        System.out.println("  3. Points & Redemption Management");
        System.out.println("  0. Back to Main Menu");
        System.out.println("========================================");
    }

    private int getIntInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            try {
                int value = Integer.parseInt(input);

                if (value >= min && value <= max) {
                    return value;
                }

                System.out.printf(
                    "  ✗ Please enter a number between %d and %d!%n",
                    min, max
                );

            } catch (NumberFormatException e) {
                System.out.println(
                    "  ✗ Invalid input! Please enter a valid number."
                );
            }
        }
    }

    // ==================================================================
    // MemberManagementUI
    // ==================================================================
    public static class MemberManagementUI {
        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private Scanner scanner = new Scanner(System.in);

        public MemberManagementUI() {
        }

        public MemberManagementUI(Scanner scanner) {
            this.scanner = scanner;
        }

        public int getMenuChoice() {
            ConsoleUtil.clearScreen();
            System.out.println();
            System.out.println("========================================");
            System.out.println("   MEMBER MANAGEMENT");
            System.out.println("========================================");
            System.out.println(" 1. Add Member");
            System.out.println(" 2. Update Member Tier");
            System.out.println(" 3. Remove Member");
            System.out.println(" 4. List Members");
            System.out.println(" 5. View Member Profile");
            System.out.println(" 6. Exit");
            System.out.println("===========================");
            return readInt("Enter your choice");
        }

        public Member inputNewMember(String memberId, String guestId) {
            System.out.println("New member id: " + memberId);
            System.out.println("Guest id (auto-generated): " + guestId);
            Tier tier = selectTier();
            if (tier == null) {
                return null;
            }
            return new Member(memberId, 0, tier, LocalDateTime.now(), guestId);
        }

        public String selectMember(LinkedListInterface<Member> members, String prompt) {
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
            int index = readInt(prompt) - 1;
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

        /**
     * Displays members in pages of 20. The user can page back/forward and pick a
     * member (by its on-screen number) to view the full profile. Returns the chosen
     * member id, or {@code null} if the user chose to go back.
     */
    public String displayMembersPaginated(LinkedListInterface<Member> members,
            java.util.function.Function<String, Guest> guestResolver) {
        if (members.isEmpty()) {
            System.out.println("No members registered yet.");
            pause();
            return null;
        }
        final int PAGE_SIZE = 20;
        int totalPages = (members.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = 0;
        while (true) {
            ConsoleUtil.clearScreen();
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, members.size());
            System.out.println();
            System.out.println("========================================");
            System.out.println("   MEMBERS  (page " + (page + 1) + "/" + totalPages
                    + ", showing " + (start + 1) + "-" + end + " of " + members.size() + ")");
            System.out.println("========================================");
            String[][] rows = new String[end - start][7];
            for (int i = start; i < end; i++) {
                Member m = members.get(i);
                Guest g = guestResolver == null ? null : guestResolver.apply(m.getGuestId());
                rows[i - start] = new String[] {
                        String.valueOf(i - start + 1),
                        m.getMemberId(),
                        g != null && g.getName() != null ? g.getName() : "-",
                        g != null && g.getContactNumber() != null ? g.getContactNumber() : "-",
                        m.getTier().name(),
                        String.valueOf(m.getPoints()),
                        m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT)
                };
            }
            TablePrinter.displayTable(
                    new String[] { "#", "Member ID", "Name", "Phone", "Tier", "Points", "Enrolled" }, rows);
            System.out.println();
            StringBuilder nav = new StringBuilder("Enter [number] to view that member's profile");
            if (page > 0) {
                nav.append("   |   [p] Previous");
            }
            if (page < totalPages - 1) {
                nav.append("   |   [n] Next");
            }
            nav.append("   |   [0] Back to menu");
            System.out.println(nav.toString());
            String cmd = readLine("Choice");
            if (cmd == null) {
                return null;
            }
            cmd = cmd.trim();
            if (cmd.equals("0")) {
                return null;
            }
            if (cmd.equalsIgnoreCase("n")) {
                if (page < totalPages - 1) {
                    page++;
                } else {
                    ConsoleUtil.printError("Already on the last page.");
                    pause();
                }
                continue;
            }
            if (cmd.equalsIgnoreCase("p")) {
                if (page > 0) {
                    page--;
                } else {
                    ConsoleUtil.printError("Already on the first page.");
                    pause();
                }
                continue;
            }
            try {
                int sel = Integer.parseInt(cmd) - 1;
                if (sel >= 0 && sel < (end - start)) {
                    return members.get(start + sel).getMemberId();
                }
            } catch (NumberFormatException e) {
                // fall through to invalid message
            }
            ConsoleUtil.printError("Invalid choice. Enter a number shown, n, p, or 0.");
            pause();
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt + ": ");
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }

        public void displayProfile(Member m, Guest g) {
            System.out.println();
            if (m == null) {
                System.out.println("Member not found.");
                return;
            }
            System.out.println("Member id     : " + m.getMemberId());
            System.out.println("Tier          : " + m.getTier());
            System.out.println("Discount      : " + m.getTier().getDiscountPercent() + "% off stays & dining");
            System.out.println("Points        : " + m.getPoints());
            System.out.println("Guest id      : " + m.getGuestId());
            if (g != null) {
                System.out.println("Name          : " + g.getName());
                System.out.println("IC / Passport : " + g.getIcOrPassport());
                System.out.println("Phone         : " + g.getContactNumber());
                System.out.println("Nationality   : " + g.getNationality());
                System.out.println("Address       : " + g.getAddress());
            } else {
                System.out.println("Guest record  : (not found for this member)");
            }
            System.out.println("Enrolled      : " + (m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT)));
        }

        public Tier selectTier() {
            System.out.println(" 1. SILVER");
            System.out.println(" 2. GOLD");
            System.out.println(" 3. PLATINUM");
            System.out.println(" 4. DIAMOND");
            System.out.println(" 0. Cancel");
            int index = readInt("Select a tier") - 1;
            Tier[] tiers = Tier.values();
            if (index < 0) {
            ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            if (index >= tiers.length) {
                ConsoleUtil.printError("Invalid selection.");
            ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            return tiers[index];
        }

        /** Prints an error message in red and waits for the user to press Enter. */
        public void showError(String message) {
            ConsoleUtil.printError(message);
            pause();
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
    }

    // ==================================================================
    // PointsManagementUI
    // ==================================================================
    public static class PointsManagementUI {

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
            System.out.println("===========================");
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

    // ==================================================================
    // RewardManagementUI
    // ==================================================================
    public static class RewardManagementUI {

        private Scanner scanner = new Scanner(System.in);

        public RewardManagementUI() {
        }

        public RewardManagementUI(Scanner scanner) {
            this.scanner = scanner;
        }

        public int getMenuChoice() {
            ConsoleUtil.clearScreen();
            System.out.println();
            System.out.println("========================================");
            System.out.println("   REWARD MANAGEMENT");
            System.out.println("========================================");
            System.out.println(" 1. Add Reward");
            System.out.println(" 2. Remove Reward");
            System.out.println(" 3. Update Reward");
            System.out.println(" 4. List Rewards");
            System.out.println(" 5. Exit");
            System.out.println("===========================");
            return readInt("Enter your choice");
        }

        public Reward inputNewReward(String rewardId) {
            System.out.println("New reward id: " + rewardId);
            String name = "";
            while (name.isEmpty()) {
                System.out.print("Reward name (0 to cancel): ");
                name = scanner.nextLine().trim();
                if (name.equals("0")) {
                    System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
                    return null;
                }
                if (name.isEmpty()) {
                    ConsoleUtil.printError("Reward name cannot be empty.");
                }
            }
            System.out.print("Description (0 to cancel): ");
            String description = scanner.nextLine().trim();
            if (description.equals("0")) {
                System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            int cost = readInt("Point cost (0 to cancel)");
            if (cost == 0) {
                System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            while (cost < 0) {
                ConsoleUtil.printError("Point cost must be positive.");
                cost = readInt("Point cost (0 to cancel)");
                if (cost == 0) {
                    System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
                    return null;
                }
            }
            return new Reward(rewardId, name, description, cost);
        }

        public String selectRewardId(LinkedListInterface<Reward> rewards, String prompt) {
            if (rewards.isEmpty()) {
                System.out.println("No rewards in the catalogue.");
                ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            displayRewards(rewards);
            System.out.println(" 0. Cancel");
            int index = readInt(prompt) - 1;
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

        public void displayRewards(LinkedListInterface<Reward> rewards) {
            if (rewards.isEmpty()) {
                System.out.println("No rewards in the catalogue.");
                return;
            }
            System.out.println();
            String[][] rows = new String[rewards.size()][4];
            for (int i = 0; i < rewards.size(); i++) {
                Reward r = rewards.get(i);
                rows[i] = new String[] {
                        r.getRewardId(),
                        truncate(r.getName(), 22),
                        String.valueOf(r.getPointCost()),
                        r.getDescription()
                };
            }
            TablePrinter.displayTable(
                    new String[] { "Reward ID", "Name", "Cost", "Description" }, rows);
        }

        /** Prompts for a string, returning the current value if the input is empty. */
        public String promptWithDefault(String prompt, String current) {
            System.out.print(prompt + " (" + current + ") (0 to cancel): ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            return input.isEmpty() ? current : input;
        }

        public Integer promptIntWithDefault(String prompt, int current) {
            System.out.print(prompt + " (" + current + ") (0 to cancel): ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            if (input.isEmpty()) {
                return current;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("Invalid number, keeping current value.");
                return current;
            }
        }

        /** Prints an error message in red and waits for the user to press Enter. */
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
}