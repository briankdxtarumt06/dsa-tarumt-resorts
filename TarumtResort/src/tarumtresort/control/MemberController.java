package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.MemberDAO;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;

public class MemberController {
    private final MemberDAO memberDAO;
    private final LinkedListInterface<Member> members = new LinkedList<>();

    public MemberController(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
        memberDAO.LoadFromFile(members);
    }

    public LinkedListInterface<Member> getMembers() {
        return members;
    }

    public Member findMember(String memberId) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getMemberId().equals(memberId)) {
                return members.get(i);
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
        members.addSorted(member);
        persist();
        return "Member added: " + member.getMemberId() + " (Tier: " + member.getTier() + ").";
    }

    public String removeMember(String memberId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        LinkedListInterface<Member> kept = new LinkedList<>();
        for (int i = 0; i < members.size(); i++) {
            if (!members.get(i).getMemberId().equals(memberId)) {
                kept.addBack(members.get(i));
            }
        }
        members.clear();
        for (int i = 0; i < kept.size(); i++) {
            members.addBack(kept.get(i));
        }
        persist();
        return "Member removed: " + memberId + ".";
    }

    public String updateMember(String memberId, Tier tier) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        member.setTier(tier);
        persist();
        return "Member " + memberId + " updated to tier " + tier + ".";
    }

    private void persist() {
        memberDAO.SaveToFile(members);
    }
}
