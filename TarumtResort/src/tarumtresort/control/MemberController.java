package tarumtresort.control;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.dao.MemberDAO;
import tarumtresort.dao.NotificationDAO;
import tarumtresort.dao.PointTransactionDAO;
import tarumtresort.dao.RedemptionRecordDAO;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Member;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;

public class MemberController {

    private final MemberDAO memberDAO;
    private final PointTransactionDAO pointTransactionDAO;
    private final RewardDAO rewardDAO;
    private final RedemptionRecordDAO redemptionRecordDAO;
    private final NotificationDAO notificationDAO;

    private final LinkedListInterface<Member> members = new LinkedList<>();
    private final LinkedListInterface<PointTransaction> pointTransactions = new LinkedList<>();
    private final LinkedListInterface<RedemptionRecord> redemptions = new LinkedList<>();
    private final LinkedListInterface<Reward> rewards = new LinkedList<>();

    public MemberController(MemberDAO memberDAO, PointTransactionDAO pointTransactionDAO,
            RewardDAO rewardDAO, RedemptionRecordDAO redemptionRecordDAO,
            NotificationDAO notificationDAO) {
        this.memberDAO = memberDAO;
        this.pointTransactionDAO = pointTransactionDAO;
        this.rewardDAO = rewardDAO;
        this.redemptionRecordDAO = redemptionRecordDAO;
        this.notificationDAO = notificationDAO;
        memberDAO.LoadFromFile(members);
        pointTransactionDAO.LoadFromFile(pointTransactions);
        redemptionRecordDAO.LoadFromFile(redemptions);
        rewardDAO.LoadFromFile(rewards);
    }

    public LinkedListInterface<Member> getMembers() {
        return members;
    }

    public LinkedListInterface<PointTransaction> getPointTransactions() {
        return pointTransactions;
    }

    public LinkedListInterface<RedemptionRecord> getRedemptions() {
        return redemptions;
    }

    public LinkedListInterface<Reward> getRewards() {
        return rewards;
    }

    public Member findMember(String memberId) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getMemberId().equals(memberId)) {
                return members.get(i);
            }
        }
        return null;
    }

    public LinkedListInterface<PointTransaction> getTransactions(String memberId) {
        LinkedListInterface<PointTransaction> result = new LinkedList<>();
        for (int i = 0; i < pointTransactions.size(); i++) {
            PointTransaction t = pointTransactions.get(i);
            if (t.getMemberId().equals(memberId)) {
                result.addBack(t);
            }
        }
        return result;
    }

    public String expirePoints(String memberId, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }

        int totalExpired = 0;
        StringBuilder report = new StringBuilder();
        LinkedListInterface<PointTransaction> txs = getTransactions(memberId);
        for (int i = 0; i < txs.size(); i++) {
            PointTransaction t = txs.get(i);
            if (t.isExpired(now)) {
                totalExpired += t.getRemainingPoints();
                report.append("  - ").append(t.getRemainingPoints())
                        .append(" pts expired (tx ").append(t.getTransactionId())
                        .append(", earned ").append(t.getDate()).append("\n");
                t.setRemainingPoints(0);
            }
        }

        if (totalExpired > 0) {
            recomputeBalance(member);
            persist();
            return totalExpired + " point(s) expired and removed from " + memberId + "'s balance:\n" + report;
        }
        return "No points expired for " + memberId + ".";
    }

    public String earnPoints(String memberId, int amount, String description, LocalDateTime date) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        if (amount <= 0) {
            return "Amount must be positive.";
        }
        expirePoints(memberId, date);

        LocalDateTime expiry = date.plusDays(365);
        PointTransaction t = new PointTransaction(nextTransactionId(), date,
                description == null || description.isBlank() ? "Points earned" : description,
                amount, expiry, amount, memberId);
        pointTransactions.addSorted(t);
        recomputeBalance(member);
        persist();
        return amount + " pts earned by " + memberId + " (expires " + expiry.toLocalDate() + "). New balance: "
                + member.getPoints();
    }

    public String redeemPoints(String memberId, String rewardId, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }

        expirePoints(memberId, now);
        int cost = reward.getPointCost();
        if (member.getPoints() < cost) {
            return "Insufficient points: " + memberId + " needs " + cost + " pts but only has "
                    + member.getPoints() + ".";
        }

        int remainingCost = cost;
        StringBuilder breakdown = new StringBuilder();
        LinkedListInterface<PointTransaction> txs = getTransactions(memberId);
        for (int i = 0; i < txs.size() && remainingCost > 0; i++) {
            PointTransaction t = txs.get(i);
            int used = Math.min(t.getRemainingPoints(), remainingCost);
            if (used > 0) {
                t.setRemainingPoints(t.getRemainingPoints() - used);
                remainingCost -= used;
                breakdown.append("  - ").append(used).append(" pts from tx ")
                        .append(t.getTransactionId()).append(" (earned ").append(t.getDate()).append("\n");
            }
        }

        recomputeBalance(member);
        redemptions.addSorted(new RedemptionRecord(nextRedemptionId(), now, memberId, rewardId));
        persist();
        return "Redeemed \"" + reward.getName() + "\" for " + cost + " pts:\n" + breakdown
                + "New balance for " + memberId + ": " + member.getPoints();
    }

    public int getAvailableBalance(String memberId, LocalDateTime now) {
        Member member = findMember(memberId);
        if (member == null) {
            return 0;
        }
        expirePoints(memberId, now);
        return member.getPoints();
    }

    private void recomputeBalance(Member member) {
        int sum = 0;
        LinkedListInterface<PointTransaction> txs = getTransactions(member.getMemberId());
        for (int i = 0; i < txs.size(); i++) {
            sum += txs.get(i).getRemainingPoints();
        }
        member.setPoints(sum);
    }

    private Reward findReward(String rewardId) {
        for (int i = 0; i < rewards.size(); i++) {
            if (rewards.get(i).getRewardId().equals(rewardId)) {
                return rewards.get(i);
            }
        }
        return null;
    }

    private void persist() {
        pointTransactionDAO.SaveToFile(pointTransactions);
        redemptionRecordDAO.SaveToFile(redemptions);
        memberDAO.SaveToFile(members);
    }

    private String nextTransactionId() {
        try {
            int max = 0;
            for (int i = 0; i < pointTransactions.size(); i++) {
                String tid = pointTransactions.get(i).getTransactionId();
                if (tid != null && tid.matches("PT\\d+")) {
                    int n = Integer.parseInt(tid.substring(2));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            String id = String.format("PT%04d", max + 1);
            while (findTransaction(id) != null) {
                max++;
                id = String.format("PT%04d", max + 1);
            }
            return id;
        } catch (RuntimeException e) {
            return String.format("PT%04d", (int) (Math.random() * 9000) + 1000);
        }
    }

    private PointTransaction findTransaction(String transactionId) {
        for (int i = 0; i < pointTransactions.size(); i++) {
            if (pointTransactions.get(i).getTransactionId().equals(transactionId)) {
                return pointTransactions.get(i);
            }
        }
        return null;
    }

    private String nextRedemptionId() {
        try {
            int max = 0;
            for (int i = 0; i < redemptions.size(); i++) {
                String rid = redemptions.get(i).getRedemptionId();
                if (rid != null && rid.matches("RR\\d+")) {
                    int n = Integer.parseInt(rid.substring(2));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("RR%04d", max + 1);
        } catch (RuntimeException e) {
            return String.format("RR%04d", (int) (Math.random() * 9000) + 1000);
        }
    }
}
