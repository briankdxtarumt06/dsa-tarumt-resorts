package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.MemberManagementUI;
import tarumtresort.dao.MemberDAO;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;
import java.util.Scanner;

public class MemberController {
    private LinkedListInterface<Member> memberList = new LinkedList<>();
    private MemberDAO memberDAO = new MemberDAO();
    private MemberManagementUI memberUI;

    public MemberController() {
        this(new Scanner(System.in));
    }
    
    public MemberController(Scanner scanner) {
        memberList = memberDAO.retrieveFromFile();
        memberUI = new MemberManagementUI(scanner);
    }

    public static void main(String[] args) {
        new MemberController().run();
    }

    /** Drives the member management menu until the user exits. */
    public void run() {
        int choice;
        do {
            choice = memberUI.getMenuChoice();
            switch (choice) {
                case 1:
                    addMemberFlow();
                    break;
                case 2:
                    updateMemberFlow();
                    break;
                case 3:
                    removeMemberFlow();
                    break;
                case 4:
                    memberUI.displayMembers(memberList);
                    memberUI.pause();
                    break;
                case 5:
                    viewProfileFlow();
                    break;
                case 6:
                    memberUI.show("Returning to main menu...");
                    break;
                default:
                    memberUI.show("Invalid choice. Please enter 1 - 6.");
            }
        } while (choice != 6);
    }

    private void addMemberFlow() {
        Member member = memberUI.inputNewMember(nextMemberId());
        memberUI.showMessage(addMember(member));
    }

    private void updateMemberFlow() {
        String memberId = memberUI.selectMember(memberList, "Select a member to update");
        if (memberId == null) {
            return;
        }
        Member member = findMember(memberId);
        memberUI.show("Current tier: " + member.getTier());
        Tier tier = memberUI.selectTier();
        if (tier == null) {
            return;
        }
        memberUI.showMessage(updateMember(memberId, tier));
    }

    private void removeMemberFlow() {
        String memberId = memberUI.selectMember(memberList, "Select a member to remove");
        if (memberId == null) {
            return;
        }
        memberUI.showMessage(removeMember(memberId));
    }

    private void viewProfileFlow() {
        String memberId = memberUI.selectMember(memberList, "Select a member");
        if (memberId == null) {
            return;
        }
        memberUI.displayProfile(findMember(memberId));
        memberUI.pause();
    }

    // ---------------------------------------------------------------
    // Business logic
    // ---------------------------------------------------------------

    public LinkedListInterface<Member> getMembers() {
        return memberList;
    }

    public Member findMember(String memberId) {
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getMemberId().equals(memberId)) {
                return memberList.get(i);
            }
        }
        return null;
    }

    /** Registers a new member and persists. */
    public String addMember(Member member) {
        if (member == null || member.getMemberId() == null) {
            return "Member cannot be null and must have an id.";
        }
        if (findMember(member.getMemberId()) != null) {
            return "Member id already exists: " + member.getMemberId();
        }
        memberList.addSorted(member);
        persist();
        return "Member added: " + member.getMemberId() + " (Tier: " + member.getTier() + ").";
    }

    /** Removes a member by id and persists. */
    public String removeMember(String memberId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        LinkedListInterface<Member> kept = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            if (!memberList.get(i).getMemberId().equals(memberId)) {
                kept.addBack(memberList.get(i));
            }
        }
        memberList.clear();
        for (int i = 0; i < kept.size(); i++) {
            memberList.addBack(kept.get(i));
        }
        persist();
        return "Member removed: " + memberId + ".";
    }

    /** Updates a member's tier and persists. */
    public String updateMember(String memberId, Tier tier) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        member.setTier(tier);
        persist();
        return "Member " + memberId + " updated to tier " + tier + ".";
    }

    /** Generates the next available member id, e.g. M005. */
    public String nextMemberId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) {
                String mid = memberList.get(i).getMemberId();
                if (mid != null && mid.matches("M\\d+")) {
                    int n = Integer.parseInt(mid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("M%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("M%03d", memberList.size() + 1);
        }
    }

    private void persist() {
        memberDAO.saveToFile(memberList);
    }
}
