package tarumtresort.control;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.MemberDAO;
import tarumtresort.dao.PromotionDAO;
import tarumtresort.entity.Member;
import tarumtresort.entity.Promotion;
import tarumtresort.entity.enums.Tier;

public class MemberController {
    private final MemberDAO memberDAO;
    private final LinkedListInterface<Member> members = new LinkedList<>();
    private final LinkedListInterface<Promotion> promotions = new LinkedList<>();

    public MemberController(MemberDAO memberDAO, PromotionDAO promotionDAO) {
        this.memberDAO = memberDAO;
        promotionDAO.loadFromFile(promotions);
        memberDAO.loadFromFile(members);
        if (promotions.isEmpty()) {
            seedDefaultPromotions();
        }
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

    /** @return the promotions that apply to the given tier (personalized offers). */
    public LinkedListInterface<Promotion> getPromotionsForTier(Tier tier) {
        LinkedListInterface<Promotion> result = new LinkedList<>();
        for (int i = 0; i < promotions.size(); i++) {
            Promotion p = promotions.get(i);
            if (p.getMinTier().compareTo(tier) <= 0) {
                result.addBack(p);
            }
        }
        return result;
    }

    private void seedDefaultPromotions() {
        promotions.addSorted(new Promotion("P001", "Welcome Drink", "Free welcome drink at the pool bar", Tier.SILVER));
        promotions.addSorted(new Promotion("P002", "Early Check-in", "Check-in from 12pm at no extra charge", Tier.GOLD));
        promotions.addSorted(new Promotion("P003", "10% Off Spa", "10% discount on spa packages", Tier.GOLD));
        promotions.addSorted(new Promotion("P004", "Room Upgrade", "Free upgrade to deluxe room (subject to availability)", Tier.PLATINUM));
        promotions.addSorted(new Promotion("P005", "Diamond Lounge Access", "Complimentary access to the Diamond lounge", Tier.DIAMOND));
        persist();
    }

    private void persist() {
        memberDAO.saveToFile(members);
    }

    /** Generates the next available member id, e.g. M005. */
    public String nextMemberId() {
        try {
            int max = 0;
            for (int i = 0; i < members.size(); i++) {
                String mid = members.get(i).getMemberId();
                if (mid != null && mid.matches("M\\d+")) {
                    int n = Integer.parseInt(mid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("M%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("M%03d", members.size() + 1);
        }
    }

}
