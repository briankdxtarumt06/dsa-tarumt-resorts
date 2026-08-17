package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.Tier;
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

    // ====================================================================
    // Sample data generation (used when the data files are empty so the
    // Points module can be demoed immediately).
    // All dates are generated relative to LocalDateTime.now() so the
    // expiry sweep and the 7-day expiry alert features always have
    // realistic data to work with on any day the program is run.
    // ====================================================================

    /** Holds the generated sample data for the whole points module. */
    public static class SampleData {
        private final LinkedListInterface<Member> members;
        private final LinkedListInterface<Reward> rewards;
        private final LinkedListInterface<Guest> guests;

        public SampleData(LinkedListInterface<Member> members,
                LinkedListInterface<Reward> rewards,
                LinkedListInterface<Guest> guests) {
            this.members = members;
            this.rewards = rewards;
            this.guests = guests;
        }

        public LinkedListInterface<Member> getMembers() {
            return members;
        }

        public LinkedListInterface<Reward> getRewards() {
            return rewards;
        }

        public LinkedListInterface<Guest> getGuests() {
            return guests;
        }
    }

    /**
     * Builds a realistic set of members, rewards and linked guests:
     * - one member per tier (SILVER / GOLD / PLATINUM / DIAMOND)
     * - transaction histories with VALID, PARTIAL and EXPIRED states
     * - a transaction expiring within 7 days (drives the expiry alert)
     * - a transaction already past expiry with points left (drives the
     *   "Run Expiry Check" auto-removal)
     * - approved / rejected / pending redemption requests, including an
     *   approved voucher (one used, one unused)
     */
    public static SampleData generateSampleData() {
        LinkedListInterface<Member> members = new LinkedList<>();
        LinkedListInterface<Reward> rewards = new LinkedList<>();
        LinkedListInterface<Guest> guests = new LinkedList<>();

        LocalDateTime now = LocalDateTime.now();

        // ---------------- Rewards catalogue ----------------
        rewards.addBack(new Reward("R001", "RM20 Dining Voucher",
                "Voucher worth RM20 at any resort restaurant", 2000, 20.0));
        rewards.addBack(new Reward("R002", "RM50 Spa Voucher",
                "Voucher worth RM50 at the resort spa", 4500, 50.0));
        rewards.addBack(new Reward("R003", "RM100 Room Upgrade Voucher",
                "Voucher worth RM100 towards room upgrades", 8000, 100.0));
        rewards.addBack(new Reward("R004", "Free Breakfast Buffet",
                "Complimentary breakfast buffet for two at Cafe Lagoon", 1500, null));
        rewards.addBack(new Reward("R005", "Beach Cabana Half-Day",
                "Private beach cabana for half a day (9am - 3pm)", 3000, null));
        rewards.addBack(new Reward("R006", "Sunset Cruise for Two",
                "Romantic sunset cruise along the coastline for two", 5500, null));
        rewards.addBack(new Reward("R007", "RM20 Shopping Voucher",
                "Voucher worth RM20 at the resort boutique", 1800, 20.0));

        // ---------------- Guests (needed for notifications) ----------------
        Guest grace = new Guest("GST001", "Grace Lim", "900101-14-5678",
                "012-345 6789", "Malaysian", "12 Jalan Merbok, Kuala Lumpur");
        Guest ahmad = new Guest("GST002", "Ahmad Faiz bin Rahman", "880505-01-2345",
                "013-222 4455", "Malaysian", "8 Lorong Setia, Johor Bahru");
        Guest sarah = new Guest("GST003", "Sarah Tan", "950303-08-1122",
                "016-778 9900", "Malaysian", "45 Jalan Ampang, Kuala Lumpur");
        Guest david = new Guest("GST004", "David Wong", "920808-14-3344",
                "017-889 0011", "Malaysian", "23 Persiaran Gurney, Penang");
        Guest priya = new Guest("GST005", "Priya Nair", "E12345678",
                "014-556 7788", "Indian", "7 Serenity Lane, Bangalore");
        guests.addBack(grace);
        guests.addBack(ahmad);
        guests.addBack(sarah);
        guests.addBack(david);
        guests.addBack(priya);

        // ---------------- Member M001: Grace Lim (DIAMOND) ----------------
        Member m001 = new Member("M001", 6300, Tier.DIAMOND, now.minusDays(700), "GST001");
        m001.addPointTransaction(new PointTransaction("PT0001", now.minusDays(690),
                "Room stay - 5 nights", 3000, now.minusDays(325), 0, "M001"));
        m001.addPointTransaction(new PointTransaction("PT0002", now.minusDays(100),
                "Spa package - signature massage", 2000, now.plusDays(265), 1500, "M001"));
        m001.addPointTransaction(new PointTransaction("PT0003", now.minusDays(200),
                "Dining - Golden Dragon restaurant", 2500, now.plusDays(165), 2500, "M001"));
        m001.addPointTransaction(new PointTransaction("PT0004", now.minusDays(90),
                "Beach club activities", 1500, now.plusDays(275), 1500, "M001"));
        m001.addPointTransaction(new PointTransaction("PT0005", now.minusDays(358),
                "Dining - Cafe Lagoon", 800, now.plusDays(7), 800, "M001"));
        RedemptionRecord rr0001 = new RedemptionRecord("RR0001", now.minusDays(35), "M001", "R001");
        rr0001.setStatus("APPROVED");
        rr0001.setVoucherCode("VCH-RR0001-9K2M");
        rr0001.setVoucherValue(20.0);
        m001.addRedemptionRecord(rr0001);
        m001.addRedemptionRecord(new RedemptionRecord("RR0004", now.minusDays(2), "M001", "R006"));
        members.addBack(m001);

        // ---------------- Member M002: Ahmad Faiz (PLATINUM) ----------------
        Member m002 = new Member("M002", 3500, Tier.PLATINUM, now.minusDays(600), "GST002");
        m002.addPointTransaction(new PointTransaction("PT0006", now.minusDays(590),
                "Room stay - 3 nights", 2000, now.minusDays(225), 0, "M002"));
        m002.addPointTransaction(new PointTransaction("PT0007", now.minusDays(300),
                "Dining - Palm Terrace", 1500, now.plusDays(65), 1500, "M002"));
        m002.addPointTransaction(new PointTransaction("PT0008", now.minusDays(120),
                "Water sports package", 1300, now.plusDays(245), 1300, "M002"));
        m002.addPointTransaction(new PointTransaction("PT0009", now.minusDays(40),
                "Mini bar charges", 700, now.plusDays(325), 700, "M002"));
        RedemptionRecord rr0002 = new RedemptionRecord("RR0002", now.minusDays(10), "M002", "R004");
        rr0002.setStatus("REJECTED");
        m002.addRedemptionRecord(rr0002);
        members.addBack(m002);

        // ---------------- Member M003: Sarah Tan (GOLD) ----------------
        // PT0010 is already past expiry with 1000 pts still left -
        // "Run Expiry Check" will sweep it off her balance.
        Member m003 = new Member("M003", 2300, Tier.GOLD, now.minusDays(380), "GST003");
        m003.addPointTransaction(new PointTransaction("PT0010", now.minusDays(380),
                "Room stay - 2 nights", 1000, now.minusDays(15), 1000, "M003"));
        m003.addPointTransaction(new PointTransaction("PT0011", now.minusDays(200),
                "Spa - aromatherapy session", 800, now.plusDays(165), 800, "M003"));
        m003.addPointTransaction(new PointTransaction("PT0012", now.minusDays(100),
                "Dining - Cafe Lagoon", 500, now.plusDays(265), 500, "M003"));
        members.addBack(m003);

        // ---------------- Member M004: David Wong (SILVER) ----------------
        // PT0013 expires in 3 days - the 7-day alert flow will flag it.
        Member m004 = new Member("M004", 950, Tier.SILVER, now.minusDays(362), "GST004");
        m004.addPointTransaction(new PointTransaction("PT0013", now.minusDays(362),
                "Room stay - 1 night", 600, now.plusDays(3), 600, "M004"));
        m004.addPointTransaction(new PointTransaction("PT0014", now.minusDays(150),
                "Dining - Sea Breeze", 350, now.plusDays(215), 350, "M004"));
        members.addBack(m004);

        // ---------------- Member M005: Priya Nair (GOLD) ----------------
        Member m005 = new Member("M005", 2100, Tier.GOLD, now.minusDays(250), "GST005");
        m005.addPointTransaction(new PointTransaction("PT0015", now.minusDays(240),
                "Room stay - 4 nights", 1200, now.plusDays(125), 1200, "M005"));
        m005.addPointTransaction(new PointTransaction("PT0016", now.minusDays(160),
                "Spa - couple package", 900, now.plusDays(205), 500, "M005"));
        m005.addPointTransaction(new PointTransaction("PT0017", now.minusDays(60),
                "Dining - Golden Dragon restaurant", 400, now.plusDays(305), 400, "M005"));
        RedemptionRecord rr0003 = new RedemptionRecord("RR0003", now.minusDays(160), "M005", "R007");
        rr0003.setStatus("APPROVED");
        rr0003.setVoucherCode("VCH-RR0003-7PB4");
        rr0003.setVoucherValue(20.0);
        rr0003.setUsed(true);
        m005.addRedemptionRecord(rr0003);
        m005.addRedemptionRecord(new RedemptionRecord("RR0005", now.minusDays(1), "M005", "R004"));
        members.addBack(m005);

        // ---------------- Notifications for the guests ----------------
        grace.addNotification(new Notification("NT0001", "TIER_UPGRADE",
                "Congratulations! You have been upgraded to DIAMOND! You now enjoy 25% off stays & dining.",
                now.minusDays(90), true));
        grace.addNotification(new Notification("NT0002", "REDEMPTION_APPROVED",
                "Your redemption request RR0001 (RM20 Dining Voucher) has been approved. Voucher code: VCH-RR0001-9K2M (worth RM20.00).",
                now.minusDays(35), false));
        ahmad.addNotification(new Notification("NT0003", "REDEMPTION_REJECTED",
                "Your redemption request RR0002 (Free Breakfast Buffet) has been rejected.",
                now.minusDays(10), false));
        sarah.addNotification(new Notification("NT0004", "TIER_UPGRADE",
                "Congratulations! You have been upgraded to GOLD! You now enjoy 15% off stays & dining.",
                now.minusDays(100), true));
        david.addNotification(new Notification("NT0005", "POINT_EXPIRY",
                "Your 600 pts (tx PT0013) will expire on " + now.plusDays(3).toLocalDate() + ".",
                now.minusDays(1), false));
        priya.addNotification(new Notification("NT0006", "REDEMPTION_APPROVED",
                "Your redemption request RR0003 (RM20 Shopping Voucher) has been approved. Voucher code: VCH-RR0003-7PB4 (worth RM20.00).",
                now.minusDays(160), true));

        return new SampleData(members, rewards, guests);
    }
}
