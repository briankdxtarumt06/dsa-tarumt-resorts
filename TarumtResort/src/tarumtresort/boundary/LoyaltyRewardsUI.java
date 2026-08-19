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
        new LoyaltyController(scanner).run();
    }

    public int getMenuChoice() {
        printMenu();
        return getIntInput("Enter choice (0-3): ", 0, 3);
    }

    private void printMenu() {
        ConsoleUtil.clearScreen();
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

        /**
         * Member list page: paginated table + action bar (mirrors
         * HousekeepingUI.printStaffListMenu). Returns the chosen action number.
         */
        public int printMemberListMenu(LinkedListInterface<Member> pageList, int page, int pageCount,
                boolean hasFilter, java.util.function.Function<String, Guest> guestResolver) {
            clearScreen();
            printBanner("MEMBER MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
            if (pageList.isEmpty()) {
                System.out.println("  (No member records)");
            } else {
                String[] header = new String[] { "No.", "Member ID", "Name", "Tier", "Points" };
                String[][] rows = new String[pageList.size()][5];
                for (int i = 0; i < pageList.size(); i++) {
                    Member m = pageList.get(i);
                    Guest g = guestResolver == null ? null : guestResolver.apply(m.getGuestId());
                    rows[i] = new String[] {
                        String.valueOf(i + 1), m.getMemberId(),
                        g != null && g.getName() != null ? g.getName() : "-",
                        m.getTier() == null ? "-" : m.getTier().name(),
                        String.valueOf(m.getPoints())
                    };
                }
                TablePrinter.displayTable(header, rows);
            }
            printSection("Actions");
            int action = 1;
            System.out.println("  " + action++ + ". View Details");
            System.out.println("  " + action++ + ". Filter by Tier");
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

        public int getMemberActionChoice() {
            printSection("Member Actions");
            System.out.println("  1. Update Member Tier");
            System.out.println("  2. Remove Member");
            System.out.println("  0. Back to List");
            printSeparator();
            return inputIntChoice("Enter choice", 0, 2);
        }

        public String inputTierFilter() {
            System.out.println("\nSelect Tier:");
            Tier[] tiers = Tier.values();
            for (int i = 0; i < tiers.length; i++) {
                System.out.println("  " + (i + 1) + ". " + tiers[i]);
            }
            return tiers[inputIntChoice("Enter tier", 1, tiers.length) - 1].name();
        }

        public int inputListIndex(String entityLabel, int max) {
            return inputIntChoice("Enter " + entityLabel + " number to view (0 = cancel)", 0, max);
        }

        // ----- banner / menu helpers (mirror HousekeepingUI) -----
        private static final int BANNER_WIDTH = 32;
        private static final String BANNER_LINE = "=".repeat(BANNER_WIDTH);

        private static void printBanner(String title) {
            System.out.println("\n" + BANNER_LINE);
            System.out.println(center(title, BANNER_WIDTH));
            System.out.println(BANNER_LINE);
        }

        private static void printSeparator() {
            System.out.println(BANNER_LINE);
        }

        private static void printSection(String text) {
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

        private void clearScreen() {
            ConsoleUtil.clearScreen();
        }

        private int inputIntChoice(String prompt, int min, int max) {
            while (true) {
                System.out.print(prompt + " (" + min + "-" + max + "): ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    int value = Integer.parseInt(line);
                    if (value >= min && value <= max) {
                        System.out.println();
                        return value;
                    }
                } catch (NumberFormatException e) {
                    // continue retry until integer input
                }
                ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
            }
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

        /**
         * Member points list page: paginated table + action bar (mirrors
         * HousekeepingUI.printStaffListMenu). Returns the chosen action number.
         */
        public int printPointsListMenu(LinkedListInterface<Member> pageList, int page, int pageCount,
                boolean hasFilter, java.util.function.Function<String, Guest> guestResolver,
                java.util.function.Function<Member, Integer> balanceResolver) {
            clearScreen();
            printBanner("POINTS & REDEMPTION MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
            if (pageList.isEmpty()) {
                System.out.println("  (No member records)");
            } else {
                String[] header = new String[] { "No.", "Member ID", "Name", "Tier", "Points" };
                String[][] rows = new String[pageList.size()][5];
                for (int i = 0; i < pageList.size(); i++) {
                    Member m = pageList.get(i);
                    Guest g = guestResolver == null ? null : guestResolver.apply(m.getGuestId());
                    Integer balance = balanceResolver == null ? null : balanceResolver.apply(m);
                    rows[i] = new String[] {
                        String.valueOf(i + 1), m.getMemberId(),
                        g != null && g.getName() != null ? g.getName() : "-",
                        m.getTier() == null ? "-" : m.getTier().name(),
                        balance == null ? "-" : String.valueOf(balance)
                    };
                }
                TablePrinter.displayTable(header, rows);
            }
            printSection("Actions");
            int action = 1;
            System.out.println("  " + action++ + ". View Details");
            System.out.println("  " + action++ + ". Earn Points");
            System.out.println("  " + action++ + ". Request Redemption");
            System.out.println("  " + action++ + ". Process Redemption Requests");
            System.out.println("  " + action++ + ". Generate Expiry Alerts");
            System.out.println("  " + action++ + ". Filter by Tier");
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

        public int getMemberPointsActionChoice() {
            printSection("Member Points Actions");
            System.out.println("  1. Run Expiry Check");
            System.out.println("  2. View Transaction History");
            System.out.println("  3. View Tier Progression");
            System.out.println("  4. View Notifications");
            System.out.println("  0. Back to List");
            printSeparator();
            return inputIntChoice("Enter choice", 0, 4);
        }

        public String inputTierFilter() {
            System.out.println("\nSelect Tier:");
            Tier[] tiers = Tier.values();
            for (int i = 0; i < tiers.length; i++) {
                System.out.println("  " + (i + 1) + ". " + tiers[i]);
            }
            return tiers[inputIntChoice("Enter tier", 1, tiers.length) - 1].name();
        }

        public int inputListIndex(String entityLabel, int max) {
            return inputIntChoice("Enter " + entityLabel + " number to view (0 = cancel)", 0, max);
        }

        // ----- banner / menu helpers (mirror HousekeepingUI) -----
        private static final int BANNER_WIDTH = 32;
        private static final String BANNER_LINE = "=".repeat(BANNER_WIDTH);

        private static void printBanner(String title) {
            System.out.println("\n" + BANNER_LINE);
            System.out.println(center(title, BANNER_WIDTH));
            System.out.println(BANNER_LINE);
        }

        private static void printSeparator() {
            System.out.println(BANNER_LINE);
        }

        private static void printSection(String text) {
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

        private void clearScreen() {
            ConsoleUtil.clearScreen();
        }

        private int inputIntChoice(String prompt, int min, int max) {
            while (true) {
                System.out.print(prompt + " (" + min + "-" + max + "): ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    int value = Integer.parseInt(line);
                    if (value >= min && value <= max) {
                        System.out.println();
                        return value;
                    }
                } catch (NumberFormatException e) {
                    // continue retry until integer input
                }
                ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
            }
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

        public int printRewardListMenu(LinkedListInterface<Reward> pageList, int page, int pageCount,
                boolean hasFilter) {
            clearScreen();
            printBanner("REWARD MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
            if (pageList.isEmpty()) {
                System.out.println("  (No rewards in the catalogue)");
            } else {
                String[] header = new String[] { "No.", "Reward ID", "Name", "Cost (pts)" };
                String[][] rows = new String[pageList.size()][4];
                for (int i = 0; i < pageList.size(); i++) {
                    Reward r = pageList.get(i);
                    rows[i] = new String[] {
                        String.valueOf(i + 1), r.getRewardId(),
                        truncate(r.getName(), 22),
                        String.valueOf(r.getPointCost())
                    };
                }
                TablePrinter.displayTable(header, rows);
            }
            printSection("Actions");
            int action = 1;
            System.out.println("  " + action++ + ". View Details");
            System.out.println("  " + action++ + ". Add New Reward");
            System.out.println("  " + action++ + ". Filter by Name");
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

        public int getRewardActionChoice() {
            printSection("Reward Actions");
            System.out.println("  1. Update Reward");
            System.out.println("  2. Remove Reward");
            System.out.println("  0. Back to List");
            printSeparator();
            return inputIntChoice("Enter choice", 0, 2);
        }

        public void displayRewardDetails(Reward r) {
            System.out.println();
            if (r == null) {
                System.out.println("Reward not found.");
                return;
            }
            System.out.println("Reward ID    : " + r.getRewardId());
            System.out.println("Name         : " + r.getName());
            System.out.println("Description  : " + r.getDescription());
            System.out.println("Point cost   : " + r.getPointCost() + " pts");
            System.out.println("Voucher value: " + (r.getVoucherValue() == null ? "-" : "RM" + r.getVoucherValue()));
        }

        public String inputNameKeyword() {
            System.out.print("\nEnter name keyword (0 to cancel): ");
            String keyword = scanner.nextLine().trim();
            if (keyword.isEmpty() || keyword.equals("0")) {
                System.out.println("Operation cancelled.");
                ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            return keyword;
        }

        public int inputListIndex(String entityLabel, int max) {
            return inputIntChoice("Enter " + entityLabel + " number to view (0 = cancel)", 0, max);
        }

        // ----- banner / menu helpers (mirror HousekeepingUI) -----
        private static final int BANNER_WIDTH = 32;
        private static final String BANNER_LINE = "=".repeat(BANNER_WIDTH);

        private static void printBanner(String title) {
            System.out.println("\n" + BANNER_LINE);
            System.out.println(center(title, BANNER_WIDTH));
            System.out.println(BANNER_LINE);
        }

        private static void printSeparator() {
            System.out.println(BANNER_LINE);
        }

        private static void printSection(String text) {
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

        private void clearScreen() {
            ConsoleUtil.clearScreen();
        }

        private int inputIntChoice(String prompt, int min, int max) {
            while (true) {
                System.out.print(prompt + " (" + min + "-" + max + "): ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    int value = Integer.parseInt(line);
                    if (value >= min && value <= max) {
                        System.out.println();
                        return value;
                    }
                } catch (NumberFormatException e) {
                    // continue retry until integer input
                }
                ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
            }
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