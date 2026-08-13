package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;
import tarumtresort.utility.ConsoleUtil;

/**
 * Pure input/output for member profiles. No business logic - the
 * MemberController decides what to do and calls these methods.
 */
public class MemberManagementUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Scanner scanner = new Scanner(System.in);

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
        System.out.println("----------------------------------------");
        return readInt("Enter your choice");
    }

    /** Prompts for a new member's details and returns it. */
    public Member inputNewMember(String memberId) {
        System.out.println("New member id: " + memberId);
        System.out.print("Guest id (linked guest account): ");
        String guestId = scanner.nextLine().trim();
        Tier tier = selectTier();
        if (tier == null) {
            tier = Tier.SILVER;
        }
        return new Member(memberId, 0, tier, LocalDateTime.now(), guestId);
    }

    /** Lists the members and returns the chosen member id, or null. */
    public String selectMember(LinkedListInterface<Member> members, String prompt) {
        if (members.isEmpty()) {
            System.out.println("No members registered yet.");
            return null;
        }
        System.out.println();
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            System.out.printf(" %d. %s (Tier: %s)%n", i + 1, m.getMemberId(), m.getTier());
        }
        int index = readInt(prompt) - 1;
        if (index < 0 || index >= members.size()) {
            System.out.println("Invalid selection.");
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
        System.out.printf("%-10s | %-10s | %8s | %s%n", "Member ID", "Tier", "Points", "Enrolled");
        System.out.println("-------------------------------------------------------------");
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            System.out.printf("%-10s | %-10s | %8d | %s%n",
                    m.getMemberId(), m.getTier(), m.getPoints(),
                    m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT));
        }
    }

    public void displayProfile(Member m) {
        System.out.println();
        System.out.println("Member id     : " + m.getMemberId());
        System.out.println("Tier          : " + m.getTier());
        System.out.println("Points        : " + m.getPoints());
        System.out.println("Guest id      : " + m.getGuestId());
        System.out.println("Enrolled      : " + (m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT)));
    }

    public Tier selectTier() {
        System.out.println(" 1. SILVER");
        System.out.println(" 2. GOLD");
        System.out.println(" 3. PLATINUM");
        System.out.println(" 4. DIAMOND");
        int index = readInt("Select a tier") - 1;
        Tier[] tiers = Tier.values();
        if (index < 0 || index >= tiers.length) {
            System.out.println("Invalid selection.");
            return null;
        }
        return tiers[index];
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
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
