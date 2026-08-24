package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.function.Function;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.entity.enums.Tier;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

// Author: Imam Mahdi Ali Ang Attuko
public class LoyaltyRewardsUI {

    private final Scanner scanner;

    private static final int BANNER_WIDTH = 32;
    private static final String BANNER_LINE = "=".repeat(BANNER_WIDTH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public LoyaltyRewardsUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public int getMenuChoice() {
        return getMenuChoice(0);
    }

    public int getMenuChoice(int unreadCount) {
        printMenu(unreadCount);
        return getIntInput("Enter choice (0-5): ", 0, 5);
    }

    private void printMenu(int unreadCount) {
        ConsoleUtil.clearScreen();
        System.out.println();
        System.out.println("========================================");
        System.out.println("  LOYALTY & REWARDS MODULE");
        System.out.println("========================================");
        System.out.println("  1. Member Management");
        System.out.println("  2. Reward Management");
        System.out.println("  3. Points & Redemption Management");
        if (unreadCount > 0) {
            System.out.println("  4. Notifications (" + unreadCount + " unread)");
        } else {
            System.out.println("  4. Notifications");
        }
        System.out.println("  5. Reports");
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
                    " ?! Please enter a number between %d and %d!%n",
                    min, max
                );

            } catch (NumberFormatException e) {
                System.out.println(
                    " ?! Invalid input! Please enter a valid number."
                );
            }
        }
    }

    static String rewardValueLabel(Reward r) {
        if (r.getDiscountPercent() != null) {
            return r.getDiscountPercent() + "%";
        }
        if (r.getVoucherValue() != null) {
            return "RM" + String.format("%.2f", r.getVoucherValue());
        }
        return "-";
    }

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
            }
            ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
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

    private static String truncate(String text, int width) {
        if (text == null || text.length() <= width) {
            return text;
        }
        return text.substring(0, width - 3) + "...";
    }

    public int printMemberListMenu(ListInterface<Member> pageList, int page, int pageCount,
            boolean hasFilter, boolean hasDeleted, Function<String, Guest> guestResolver) {
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
        if (hasDeleted) {
            System.out.println("  " + action++ + ". Restore Deleted Member");
        }
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

    public int printDeletedMembersMenu(ListInterface<Member> pageList, int page, int pageCount,
            Function<String, Guest> guestResolver) {
        clearScreen();
        printBanner("DELETED MEMBERS (Page " + (page + 1) + " of " + pageCount + ")");
        if (pageList.isEmpty()) {
            System.out.println("  (No deleted members)");
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
        System.out.println("  " + action++ + ". Restore Selected Member");
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
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

    public void displayProfile(Member m, Guest g) {
        System.out.println();
        if (m == null) {
            System.out.println("Member not found.");
            return;
        }
        String[][] details = {
            {"Member ID", m.getMemberId()},
            {"Tier", m.getTier() == null ? "-" : m.getTier().name()},
            {"Discount", (m.getTier() == null ? 0 : m.getTier().getDiscountPercent()) + "% off stays & dining"},
            {"Points", String.valueOf(m.getPoints())},
            {"Guest ID", m.getGuestId() == null ? "-" : m.getGuestId()},
            {"Name", g == null ? "-" : g.getName()},
            {"IC / Passport", g == null ? "-" : g.getIcOrPassport()},
            {"Phone", g == null ? "-" : g.getContactNumber()},
            {"Nationality", g == null ? "-" : g.getNationality()},
            {"Address", g == null ? "-" : g.getAddress()},
            {"Enrolled", m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT)}
        };
        printSection("Details");
        int keyWidth = 0;
        for (String[] pair : details) {
            if (pair[0] != null && pair[0].length() > keyWidth) {
                keyWidth = pair[0].length();
            }
        }
        for (String[] pair : details) {
            System.out.println(String.format("%-" + (keyWidth + 3) + "s: %s",
                    pair[0] == null ? "" : pair[0],
                    pair[1] == null ? "-" : pair[1]));
        }
        printSeparator();
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

    public int printPointsListMenu(ListInterface<Member> pageList, int page, int pageCount,
            boolean hasFilter, Function<String, Guest> guestResolver,
            Function<Member, Integer> balanceResolver) {
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

    public String selectReward(ListInterface<Reward> rewards) {
        if (rewards.isEmpty()) {
            System.out.println("No rewards available for this member's tier.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        System.out.println();
        String[][] rows = new String[rewards.size()][5];
        for (int i = 0; i < rewards.size(); i++) {
            Reward r = rewards.get(i);
            rows[i] = new String[] {
                    String.valueOf(i + 1),
                    truncate(r.getName(), 28),
                    r.getRoomType() == null ? "Any" : r.getRoomType().name(),
                    rewardValueLabel(r),
                    r.getPointCost() + " pts"
            };
        }
        TablePrinter.displayTable(new String[] { "#", "Reward", "Room Type", "Value", "Points" }, rows);
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

    public void displayTransactions(ListInterface<PointTransaction> txs) {
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

    public void displayNotifications(ListInterface<Notification> list) {
        if (list.isEmpty()) {
            System.out.println("No notifications for this member.");
            return;
        }
        System.out.println();
        for (int i = 0; i < list.size(); i++) {
            Notification n = list.get(i);
            System.out.printf(" %d. [%s] %s (%s)%n",
                    i + 1, n.isRead() ? "READ" : "UNREAD",
                    n.getMessage(), n.getDate() == null ? "-" : n.getDate().format(DATE_FMT));
        }
    }

    public boolean confirmMarkAllRead() {
        System.out.print("Mark all as read? (y/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("y");
    }

    public String selectPendingRequest(ListInterface<RedemptionRecord> pending) {
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

    private String statusOf(PointTransaction t) {
        if (t.getExpiryDate() != null && !LocalDateTime.now().isBefore(t.getExpiryDate())) {
            return "EXPIRED";
        }
        if (t.getRemainingPoints() < t.getPointChange()) {
            return "PARTIAL";
        }
        return "VALID";
    }

    public int printRewardListMenu(ListInterface<Reward> pageList, int page, int pageCount,
            boolean hasFilter, boolean hasDeleted, String sortLabel) {
        clearScreen();
        printBanner("REWARD MANAGEMENT (Page " + (page + 1) + " of " + pageCount + ")");
        if (sortLabel != null && !sortLabel.isEmpty()) {
            System.out.println("  Sort: " + sortLabel);
        }
        if (pageList.isEmpty()) {
            System.out.println("  (No rewards in the catalogue)");
        } else {
            String[] header = new String[] { "No.", "Reward ID", "Name", "Min Tier", "Room Type", "Value", "Cost (pts)" };
            String[][] rows = new String[pageList.size()][7];
            for (int i = 0; i < pageList.size(); i++) {
                Reward r = pageList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), r.getRewardId(),
                    truncate(r.getName(), 30),
                    r.getMinTier() == null ? "SILVER" : r.getMinTier().name(),
                    r.getRoomType() == null ? "Any" : r.getRoomType().name(),
                    rewardValueLabel(r),
                    String.valueOf(r.getPointCost())
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        printSection("Actions");
        int action = 1;
        System.out.println("  " + action++ + ". View Details");
        System.out.println("  " + action++ + ". Add New Reward");
        System.out.println("  " + action++ + ". Filter by Min Tier");
        System.out.println("  " + action++ + ". Sort by Points");
        if (hasDeleted) {
            System.out.println("  " + action++ + ". Restore Deleted Reward");
        }
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

    public int printDeletedRewardsMenu(ListInterface<Reward> pageList, int page, int pageCount) {
        clearScreen();
        printBanner("DELETED REWARDS (Page " + (page + 1) + " of " + pageCount + ")");
        if (pageList.isEmpty()) {
            System.out.println("  (No deleted rewards)");
        } else {
            String[] header = new String[] { "No.", "Reward ID", "Name", "Min Tier", "Room Type", "Value", "Cost (pts)" };
            String[][] rows = new String[pageList.size()][7];
            for (int i = 0; i < pageList.size(); i++) {
                Reward r = pageList.get(i);
                rows[i] = new String[] {
                    String.valueOf(i + 1), r.getRewardId(),
                    truncate(r.getName(), 30),
                    r.getMinTier() == null ? "SILVER" : r.getMinTier().name(),
                    r.getRoomType() == null ? "Any" : r.getRoomType().name(),
                    rewardValueLabel(r),
                    String.valueOf(r.getPointCost())
                };
            }
            TablePrinter.displayTable(header, rows);
        }
        printSection("Actions");
        int action = 1;
        System.out.println("  " + action++ + ". Restore Selected Reward");
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
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
        System.out.println("Min tier     : " + (r.getMinTier() == null ? "SILVER" : r.getMinTier().name()));
        System.out.println("Room type    : " + (r.getRoomType() == null ? "Any" : r.getRoomType().name()));
        System.out.println("Voucher value: " + rewardValueLabel(r));
    }

    public Tier inputMinTierFilter() {
        System.out.println();
        int choice = inputIntChoice(
                "Filter by min tier (1=SILVER, 2=GOLD, 3=PLATINUM, 4=DIAMOND, 0=cancel)", 0, 4);
        if (choice == 0) {
            System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return Tier.values()[choice - 1];
    }

    public Reward inputNewReward(String rewardId) {
        System.out.println("New reward id: " + rewardId);
        String name = inputRewardName();
        if (name == null) {
            return null;
        }
        String description = inputRewardDescription();
        if (description == null) {
            return null;
        }
        int cost = inputPointCost();
        if (cost == 0) {
            return null;
        }
        Tier minTier = inputMinTier();
        RoomType roomType = inputRoomType();
        int voucherType = inputVoucherType();
        Double voucherValue = null;
        Integer discountPercent = null;
        if (voucherType == 1) {
            int value = inputVoucherValue();
            if (value <= 0) {
                System.out.println("Operation cancelled.");
                ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            voucherValue = (double) value;
        } else if (voucherType == 2) {
            discountPercent = inputDiscountPercent();
        }

        return new Reward(rewardId, name, description, cost, voucherValue, minTier, roomType, discountPercent);
    }

    public String inputRewardName() {
        while (true) {
            System.out.print("Enter Reward Name (0 to cancel): ");
            String name = scanner.nextLine().trim();
            if (name.equals("0")) {
                System.out.println("Operation cancelled.");
                ConsoleUtil.pressEnterToContinue(scanner);
                return null;
            }
            if (!name.isEmpty()) {
                return name;
            }
            ConsoleUtil.printError("Reward name cannot be empty.");
        }
    }

    public String inputRewardDescription() {
        System.out.print("Enter Description (0 to cancel): ");
        String description = scanner.nextLine().trim();
        if (description.equals("0")) {
            System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        return description;
    }

    public int inputPointCost() {
        while (true) {
            int cost = readInt("Enter Point Cost (0 to cancel)");
            if (cost == 0) {
                System.out.println("Operation cancelled.");
                ConsoleUtil.pressEnterToContinue(scanner);
                return 0;
            }
            if (cost > 0) {
                return cost;
            }
            ConsoleUtil.printError("Point cost must be positive.");
        }
    }

    public Tier inputMinTier() {
        System.out.println("Select Min Tier:");
        Tier[] tiers = Tier.values();
        for (int i = 0; i < tiers.length; i++) {
            System.out.println("  " + (i + 1) + ". " + tiers[i]);
        }
        int choice = inputIntChoice("Enter min tier", 1, tiers.length);
        return tiers[choice - 1];
    }

    public RoomType inputRoomType() {
        System.out.println("Select Room Type:");
        System.out.println("  0. Any / Generic");
        RoomType[] types = RoomType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + (i + 1) + ". " + types[i]);
        }
        int choice = inputIntChoice("Enter room type", 0, types.length);
        return choice == 0 ? null : types[choice - 1];
    }

    public int inputVoucherType() {
        System.out.println("Select Voucher Type:");
        System.out.println("  0. Not a voucher");
        System.out.println("  1. Fixed RM");
        System.out.println("  2. Percentage (%)");
        return inputIntChoice("Enter voucher type", 0, 2);
    }

    public int inputVoucherValue() {
        while (true) {
            int value = readInt("Enter Voucher Value in RM (0 to cancel)");
            if (value == 0) {
                System.out.println("Operation cancelled.");
                ConsoleUtil.pressEnterToContinue(scanner);
                return 0;
            }
            if (value > 0) {
                return value;
            }
            ConsoleUtil.printError("Voucher value must be positive.");
        }
    }

    public int inputDiscountPercent() {
        return inputIntChoice("Enter discount percent", 1, 100);
    }

    public void printRewardCreationSummary(Reward reward) {
        printSection("Reward Summary");
        System.out.println("  Reward ID   : " + reward.getRewardId());
        System.out.println("  Name        : " + reward.getName());
        System.out.println("  Description : " + reward.getDescription());
        System.out.println("  Point Cost  : " + reward.getPointCost() + " pts");
        System.out.println("  Min Tier    : " + (reward.getMinTier() == null ? "SILVER" : reward.getMinTier().name()));
        System.out.println("  Room Type   : " + (reward.getRoomType() == null ? "Any" : reward.getRoomType().name()));
        if (reward.getDiscountPercent() != null) {
            System.out.println("  Voucher     : " + reward.getDiscountPercent() + "% OFF");
        } else if (reward.getVoucherValue() != null) {
            System.out.println("  Voucher     : RM" + String.format("%.2f", reward.getVoucherValue()));
        } else {
            System.out.println("  Voucher     : -");
        }
        printSeparator();
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

    public Tier promptTierWithDefault(String prompt, Tier current) {
        System.out.print(prompt + " (1=SILVER, 2=GOLD, 3=PLATINUM, 4=DIAMOND)"
                + " (" + current + ") (0 to cancel): ");
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
            int idx = Integer.parseInt(input);
            if (idx >= 1 && idx <= Tier.values().length) {
                return Tier.values()[idx - 1];
            }
        } catch (NumberFormatException e) {
        }
        ConsoleUtil.printError("Invalid tier, keeping current value.");
        return current;
    }

    public RoomType promptRoomTypeWithDefault(String prompt, RoomType current) {
        System.out.print(prompt + " (1-7 = type, 'none' = generic, empty = keep"
                + " (" + (current == null ? "Any" : current.name()) + ")): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return current;
        }
        if (input.equalsIgnoreCase("none")) {
            return null;
        }
        try {
            int idx = Integer.parseInt(input);
            if (idx >= 1 && idx <= RoomType.values().length) {
                return RoomType.values()[idx - 1];
            }
        } catch (NumberFormatException e) {
        }
        ConsoleUtil.printError("Invalid room type, keeping current value.");
        return current;
    }

    public Double promptDoubleWithDefault(String prompt, Double current) {
        System.out.print(prompt + " (" + (current == null ? "none" : "RM" + current) + ") (0 to cancel): ");
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
            double value = Double.parseDouble(input);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException e) {
        }
        ConsoleUtil.printError("Invalid value, keeping current value.");
        return current;
    }

    public Integer promptVoucherTypeWithDefault(Reward reward) {
        String currentType = reward.getDiscountPercent() != null
                ? reward.getDiscountPercent() + "% OFF"
                : (reward.getVoucherValue() != null ? "RM" + reward.getVoucherValue() : "not a voucher");
        System.out.print("New voucher type (1=Fixed RM, 2=Percentage %, empty=keep"
                + " (" + currentType + "), 0=cancel): ");
        String input = scanner.nextLine().trim();
        if (input.equals("0")) {
            System.out.println("Operation cancelled.");
            ConsoleUtil.pressEnterToContinue(scanner);
            return null;
        }
        if (input.isEmpty()) {
            return 0; 
        }
        if (input.equals("1") || input.equals("2")) {
            return Integer.parseInt(input);
        }
        ConsoleUtil.printError("Invalid voucher type, keeping current value.");
        return 0;
    }

    public Integer promptPercentWithDefault(String prompt, Integer current) {
        System.out.print(prompt + " (" + (current == null ? "none" : current + "%") + ") (0 to cancel): ");
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
            int value = Integer.parseInt(input);
            if (value >= 1 && value <= 100) {
                return value;
            }
        } catch (NumberFormatException e) {
        }
        ConsoleUtil.printError("Invalid percent (1-100), keeping current value.");
        return current;
    }

    public int printMemberListMenu(String[][] rows, int page, int pageCount, int totalUnread) {
        ConsoleUtil.clearScreen();
        printBanner("NOTIFICATION CENTRE (Page " + (page + 1) + " of " + pageCount + ")");
        if (totalUnread > 0) {
            System.out.println("  " + totalUnread + " unread notification(s) across all members");
        }
        if (rows.length == 0) {
            System.out.println("  (No members)");
        } else {
            TablePrinter.displayTable(
                    new String[] { "No.", "Member ID", "Name", "Unread" }, rows);
        }
        printSection("Actions");
        int action = 1;
        System.out.println("  " + action++ + ". View Notifications");
        System.out.println("  " + action++ + ". Mark All Read (All Members)");
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        System.out.println("  0. Back");
        printSeparator();
        return inputIntChoice("Enter choice", 0, action - 1);
    }

    public int printMemberNotificationsMenu(String memberId, String memberName,
            String[][] rows, int page, int pageCount) {
        ConsoleUtil.clearScreen();
        printBanner("NOTIFICATIONS - " + memberId
                + (memberName == null ? "" : " (" + memberName + ")")
                + (pageCount > 1 ? " (Page " + (page + 1) + " of " + pageCount + ")" : ""));
        if (rows.length == 0) {
            System.out.println("  (No notifications for this member)");
        } else {
            TablePrinter.displayTable(
                    new String[] { "No.", "Type", "Message", "Date", "Status" }, rows);
        }
        printSection("Actions");
        int action = 1;
        System.out.println("  " + action++ + ". Mark All Read");
        System.out.println("  " + action++ + ". Delete Notification");
        if (page < pageCount - 1) {
            System.out.println("  " + action++ + ". Next Page");
        }
        if (page > 0) {
            System.out.println("  " + action++ + ". Previous Page");
        }
        System.out.println("  0. Back to Members");
        printSeparator();
        return inputIntChoice("Enter choice", 0, action - 1);
    }
    
    public int getReportMenuChoice() {
        ConsoleUtil.clearScreen();
        printBanner("LOYALTY & REWARDS REPORTS");
        System.out.println("  1. Membership & Tier Performance Report");
        System.out.println("  2. Redemption & Voucher Report");
        System.out.println("  0. Back");
        printSeparator();
        return inputIntChoice("Enter choice", 0, 2);
    }

    public int inputListIndex(String entityLabel, int max) {
        return inputListChoice(entityLabel, "view", max);
    }

    public int inputListIndex(String entityLabel, String actionVerb, int max) {
        return inputListChoice(entityLabel, actionVerb, max);
    }

    private int inputListChoice(String entityLabel, String actionVerb, int max) {
        return inputIntChoice("Enter " + entityLabel + " number to " + actionVerb + " (0 = cancel)", 0, max);
    }

    public boolean confirm(String message) {
        System.out.println("\n==========Confirmation==========");
        System.out.println("  " + message);
        System.out.println("  [Y] YES");
        System.out.println("  [N] NO");
        System.out.println("=================================");
        System.out.print("Your choice (y/n): ");
        String line = scanner.nextLine().trim();
        return !line.isEmpty() && Character.toLowerCase(line.charAt(0)) == 'y';
    }

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
}
