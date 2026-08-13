package tarumtresort.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import tarumtresort.control.MemberController;
import tarumtresort.dao.MemberDAO;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;
import tarumtresort.utility.ConsoleUtil;

public class MemberManagementUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Scanner scanner;
    private final MemberController controller;

    public MemberManagementUI() {
        this(new Scanner(System.in));
    }

    public MemberManagementUI(Scanner scanner) {
        this.scanner = scanner;
        this.controller = new MemberController();
    }

    public void run() {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter your choice");
            switch (choice) {
                case 1:
                    addMember();
                    break;
                case 2:
                    updateMember();
                    break;
                case 3:
                    removeMember();
                    break;
                case 4:
                    listMembers();
                    break;
                case 5:
                    viewProfile();
                    break;
                case 6:
                    System.out.println("Returning to main menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 - 6.");
            }
        } while (choice != 6);
    }

    private void printMenu() {
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
    }

    private void addMember() {
        String memberId = controller.nextMemberId();
        System.out.println("New member id: " + memberId);
        System.out.print("Guest id (linked guest account): ");
        String guestId = scanner.nextLine().trim();
        Tier tier = selectTier();
        if (tier == null) {
            return;
        }
        Member member = new Member(memberId, 0, tier, LocalDateTime.now(), guestId);
        System.out.println(controller.addMember(member));
    }

    private void updateMember() {
        String memberId = selectMember("Select a member to update");
        if (memberId == null) {
            return;
        }
        Member member = controller.findMember(memberId);
        System.out.println("Current tier: " + member.getTier());
        Tier tier = selectTier();
        if (tier == null) {
            return;
        }
        System.out.println(controller.updateMember(memberId, tier));
    }

    private void removeMember() {
        String memberId = selectMember("Select a member to remove");
        if (memberId == null) {
            return;
        }
        System.out.println(controller.removeMember(memberId));
    }

    private void listMembers() {
        if (controller.getMembers().isEmpty()) {
            System.out.println("No members registered yet.");
            return;
        }
        System.out.println();
        System.out.printf("%-10s | %-10s | %8s | %s%n", "Member ID", "Tier", "Points", "Enrolled");
        System.out.println("-------------------------------------------------------------");
        for (int i = 0; i < controller.getMembers().size(); i++) {
            Member m = controller.getMembers().get(i);
            System.out.printf("%-10s | %-10s | %8d | %s%n",
                    m.getMemberId(), m.getTier(), m.getPoints(),
                    m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT));
        }
        ConsoleUtil.pressEnterToContinue(scanner);
    }

    private void viewProfile() {
        String memberId = selectMember("Select a member");
        if (memberId == null) {
            return;
        }
        Member m = controller.findMember(memberId);
        System.out.println();
        System.out.println("Member id     : " + m.getMemberId());
        System.out.println("Tier          : " + m.getTier());
        System.out.println("Points        : " + m.getPoints());
        System.out.println("Guest id      : " + m.getGuestId());
        System.out.println("Enrolled      : " + (m.getEnrollmentDate() == null ? "-" : m.getEnrollmentDate().format(DATE_FMT)));
        System.out.println();
        ConsoleUtil.pressEnterToContinue(scanner);
    }

    private String selectMember(String prompt) {
        if (controller.getMembers().isEmpty()) {
            System.out.println("No members registered yet.");
            return null;
        }
        System.out.println();
        for (int i = 0; i < controller.getMembers().size(); i++) {
            Member m = controller.getMembers().get(i);
            System.out.printf(" %d. %s (Tier: %s)%n", i + 1, m.getMemberId(), m.getTier());
        }
        int index = readInt(prompt) - 1;
        if (index < 0 || index >= controller.getMembers().size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return controller.getMembers().get(index).getMemberId();
    }

    private Tier selectTier() {
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
