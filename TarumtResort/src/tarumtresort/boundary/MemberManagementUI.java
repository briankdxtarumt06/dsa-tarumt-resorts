package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
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
            tier = Tier.SILVER;
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

    public void displayMembers(LinkedListInterface<Member> members) {
        if (members.isEmpty()) {
            System.out.println("No members registered yet.");
            return;
        }
        System.out.println();
        String[][] rows = new String[members.size()][4];
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            rows[i] = new String[] {
                    m.getMemberId(),
                    m.getTier().name(),
                    String.valueOf(m.getPoints()),
                    m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT)
            };
        }
        TablePrinter.displayTable(
                new String[] { "Member ID", "Tier", "Points", "Enrolled" }, rows);
    }

    public void displayProfile(Member m) {
        System.out.println();
        System.out.println("Member id     : " + m.getMemberId());
        System.out.println("Tier          : " + m.getTier());
        System.out.println("Discount      : " + m.getTier().getDiscountPercent() + "% off stays & dining");
        System.out.println("Points        : " + m.getPoints());
        System.out.println("Guest id      : " + m.getGuestId());
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
            System.out.println("Operation cancelled.");
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
