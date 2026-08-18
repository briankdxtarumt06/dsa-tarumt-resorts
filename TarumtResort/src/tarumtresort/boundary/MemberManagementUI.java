package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

public class MemberManagementUI {
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
        System.out.println(" 5. Exit");
        System.out.println("----------------------------------------");
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
