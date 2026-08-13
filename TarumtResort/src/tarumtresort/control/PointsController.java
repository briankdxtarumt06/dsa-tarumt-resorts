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
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.Tier;

public class PointsController {
    private LinkedListInterface<Member> memberList = new LinkedList<>();
    private LinkedListInterface<PointTransaction> pointTransactionList = new LinkedList<>();
    private LinkedListInterface<RedemptionRecord> redemptionList = new LinkedList<>();
    private LinkedListInterface<Reward> rewardList = new LinkedList<>();
    private LinkedListInterface<Notification> notificationList = new LinkedList<>();

    private MemberDAO memberDAO = new MemberDAO();
    private PointTransactionDAO pointTransactionDAO = new PointTransactionDAO();
    private RewardDAO rewardDAO = new RewardDAO();
    private RedemptionRecordDAO redemptionRecordDAO = new RedemptionRecordDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();

    public PointsController() {
        memberList = memberDAO.retrieveFromFile();
        pointTransactionList = pointTransactionDAO.retrieveFromFile();
        redemptionList = redemptionRecordDAO.retrieveFromFile();
        rewardList = rewardDAO.retrieveFromFile();
        notificationList = notificationDAO.retrieveFromFile();
        reconcileTiersOnLoad();
    }

    private void reconcileTiersOnLoad() {
        boolean changed = false;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            Tier correct = tierFor(getCumulativeEarned(m.getMemberId()));
            if (m.getTier() != correct) {
                m.setTier(correct);
                changed = true;
            }
        }
        if (changed) {
            memberDAO.saveToFile(memberList);
        }
    }

    public LinkedListInterface<Member> getMembers() {
        return memberList;
    }

    public LinkedListInterface<PointTransaction> getPointTransactions() {
        return pointTransactionList;
    }

    public LinkedListInterface<RedemptionRecord> getRedemptions() {
        return redemptionList;
    }

    public LinkedListInterface<Reward> getRewards() {
        return rewardList;
    }

    public Member findMember(String memberId) {
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getMemberId().equals(memberId)) {
                return memberList.get(i);
            }
        }
        return null;
    }

    public LinkedListInterface<PointTransaction> getTransactions(String memberId) {
        LinkedListInterface<PointTransaction> result = new LinkedList<>();
        for (int i = 0; i < pointTransactionList.size(); i++) {
            PointTransaction t = pointTransactionList.get(i);
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

        recomputeBalance(member);
        if (totalExpired > 0) {
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
        pointTransactionList.addSorted(t);
        recomputeBalance(member);
        recomputeTier(member, date);
        persist();
        return amount + " pts earned by " + memberId + " (expires " + expiry.toLocalDate() + "). New balance: "
                + member.getPoints();
    }

    public String requestRedemption(String memberId, String rewardId, LocalDateTime now) {
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
        redemptionList.addSorted(new RedemptionRecord(nextRedemptionId(), now, memberId, rewardId));
        persist();
        return "Redemption requested: " + reward.getName() + " (" + cost + " pts) for " + memberId
                + " - pending approval.";
    }

    public String approveRedemption(String redemptionId, LocalDateTime now) {
        RedemptionRecord record = findRedemption(redemptionId);
        if (record == null) {
            return "Redemption request not found: " + redemptionId;
        }
        if (!"PENDING".equals(record.getStatus())) {
            return "Request " + redemptionId + " is already " + record.getStatus() + ".";
        }
        Member member = findMember(record.getMemberId());
        if (member == null) {
            return "Member not found: " + record.getMemberId();
        }
        Reward reward = findReward(record.getRewardId());
        if (reward == null) {
            return "Reward not found: " + record.getRewardId();
        }

        expirePoints(member.getMemberId(), now);
        int cost = reward.getPointCost();
        if (member.getPoints() < cost) {
            return "Cannot approve " + redemptionId + ": " + member.getMemberId()
                    + " no longer has enough points (" + member.getPoints() + "/" + cost + ").";
        }

        int remainingCost = cost;
        StringBuilder breakdown = new StringBuilder();
        LinkedListInterface<PointTransaction> txs = getTransactions(member.getMemberId());
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
        record.setStatus("APPROVED");
        notifyMember(member, "REDEMPTION_APPROVED",
                "Your redemption request " + redemptionId + " (" + reward.getName() + ") has been approved.", now);
        persist();
        return "Approved " + redemptionId + " (" + reward.getName() + "):\n" + breakdown
                + "New balance for " + member.getMemberId() + ": " + member.getPoints();
    }

    public String rejectRedemption(String redemptionId, LocalDateTime now) {
        RedemptionRecord record = findRedemption(redemptionId);
        if (record == null) {
            return "Redemption request not found: " + redemptionId;
        }
        if (!"PENDING".equals(record.getStatus())) {
            return "Request " + redemptionId + " is already " + record.getStatus() + ".";
        }
        record.setStatus("REJECTED");
        Member member = findMember(record.getMemberId());
        if (member != null) {
            Reward reward = findReward(record.getRewardId());
            String rewardName = reward == null ? record.getRewardId() : reward.getName();
            notifyMember(member, "REDEMPTION_REJECTED",
                    "Your redemption request " + redemptionId + " (" + rewardName + ") has been rejected.", now);
        }
        persist();
        return "Rejected redemption request " + redemptionId + ".";
    }

    public LinkedListInterface<RedemptionRecord> getPendingRedemptions() {
        LinkedListInterface<RedemptionRecord> result = new LinkedList<>();
        for (int i = 0; i < redemptionList.size(); i++) {
            if ("PENDING".equals(redemptionList.get(i).getStatus())) {
                result.addBack(redemptionList.get(i));
            }
        }
        return result;
    }

    private RedemptionRecord findRedemption(String redemptionId) {
        for (int i = 0; i < redemptionList.size(); i++) {
            if (redemptionList.get(i).getRedemptionId().equals(redemptionId)) {
                return redemptionList.get(i);
            }
        }
        return null;
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
        for (int i = 0; i < rewardList.size(); i++) {
            if (rewardList.get(i).getRewardId().equals(rewardId)) {
                return rewardList.get(i);
            }
        }
        return null;
    }

    private void persist() {
        pointTransactionDAO.saveToFile(pointTransactionList);
        redemptionRecordDAO.saveToFile(redemptionList);
        memberDAO.saveToFile(memberList);
    }

    private String nextTransactionId() {
        try {
            int max = 0;
            for (int i = 0; i < pointTransactionList.size(); i++) {
                String tid = pointTransactionList.get(i).getTransactionId();
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
        for (int i = 0; i < pointTransactionList.size(); i++) {
            if (pointTransactionList.get(i).getTransactionId().equals(transactionId)) {
                return pointTransactionList.get(i);
            }
        }
        return null;
    }

    private String nextRedemptionId() {
        try {
            int max = 0;
            for (int i = 0; i < redemptionList.size(); i++) {
                String rid = redemptionList.get(i).getRedemptionId();
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

    private static final int ALERT_DAYS_BEFORE_EXPIRY = 7;

    public String generateExpiryAlerts(LocalDateTime now) {
        int created = 0;
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < pointTransactionList.size(); i++) {
            PointTransaction t = pointTransactionList.get(i);
            if (t.isExpired(now) || t.getRemainingPoints() <= 0) {
                continue;
            }
            boolean expiringSoon = !now.isBefore(t.getExpiryDate().minusDays(ALERT_DAYS_BEFORE_EXPIRY))
                    && now.isBefore(t.getExpiryDate());
            if (!expiringSoon) {
                continue;
            }
            Member member = findMember(t.getMemberId());
            if (member == null || member.getGuestId() == null) {
                continue;
            }
            String message = "Your " + t.getRemainingPoints() + " pts (tx " + t.getTransactionId()
                    + ") will expire on " + t.getExpiryDate().toLocalDate() + ".";
            if (hasNotification(member.getGuestId(), message)) {
                continue; // already alerted
            }
            notificationList.addSorted(new Notification(nextNotificationId(), "POINT_EXPIRY",
                    message, now, false, member.getGuestId()));
            created++;
            summary.append("  - ").append(message).append("\n");
        }
        if (created > 0) {
            notificationDAO.saveToFile(notificationList);
            return created + " expiry alert(s) generated:\n" + summary;
        }
        return "No new expiry alerts to generate.";
    }

    private boolean hasNotification(String guestId, String message) {
        for (int i = 0; i < notificationList.size(); i++) {
            Notification n = notificationList.get(i);
            if (n.getGuestId() != null && n.getGuestId().equals(guestId)
                    && n.getMessage().equals(message)) {
                return true;
            }
        }
        return false;
    }

    public LinkedListInterface<Notification> getNotifications(String guestId) {
        LinkedListInterface<Notification> result = new LinkedList<>();
        for (int i = 0; i < notificationList.size(); i++) {
            if (guestId != null && guestId.equals(notificationList.get(i).getGuestId())) {
                result.addBack(notificationList.get(i));
            }
        }
        return result;
    }

    public String markNotificationRead(String notificationId) {
        for (int i = 0; i < notificationList.size(); i++) {
            Notification n = notificationList.get(i);
            if (n.getNotificationId().equals(notificationId)) {
                n.setRead(true);
                notificationDAO.saveToFile(notificationList);
                return "Notification " + notificationId + " marked as read.";
            }
        }
        return "Notification not found: " + notificationId;
    }

    private String nextNotificationId() {
        try {
            int max = 0;
            for (int i = 0; i < notificationList.size(); i++) {
                String nid = notificationList.get(i).getNotificationId();
                if (nid != null && nid.matches("NT\\d+")) {
                    int n = Integer.parseInt(nid.substring(2));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("NT%04d", max + 1);
        } catch (RuntimeException e) {
            return String.format("NT%04d", notificationList.size() + 1);
        }
    }

    private static final int[] TIER_THRESHOLDS = {0, 1000, 3000, 6000};

    private Tier tierFor(int cumulativeEarned) {
        if (cumulativeEarned >= TIER_THRESHOLDS[3]) {
            return Tier.DIAMOND;
        }
        if (cumulativeEarned >= TIER_THRESHOLDS[2]) {
            return Tier.PLATINUM;
        }
        if (cumulativeEarned >= TIER_THRESHOLDS[1]) {
            return Tier.GOLD;
        }
        return Tier.SILVER;
    }

    public int getCumulativeEarned(String memberId) {
        int total = 0;
        LinkedListInterface<PointTransaction> txs = getTransactions(memberId);
        for (int i = 0; i < txs.size(); i++) {
            int change = txs.get(i).getPointChange();
            if (change > 0) {
                total += change;
            }
        }
        return total;
    }

    public boolean recomputeTier(Member member, LocalDateTime now) {
        Tier current = tierFor(getCumulativeEarned(member.getMemberId()));
        if (member.getTier() != current) {
            member.setTier(current);
            if (member.getGuestId() != null) {
                String message = "Congratulations! You have been upgraded to " + current + "!";
                notificationList.addSorted(new Notification(nextNotificationId(), "TIER_UPGRADE",
                        message, now, false, member.getGuestId()));
                notificationDAO.saveToFile(notificationList);
            }
            return true;
        }
        return false;
    }

    public String getTierProgress(String memberId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        int cumulative = getCumulativeEarned(memberId);
        Tier current = tierFor(cumulative);
        int currentIdx = current.ordinal();
        String line = "Member " + memberId + " - Tier: " + current
                + " (cumulative earned " + cumulative + " pts)";
        if (currentIdx < TIER_THRESHOLDS.length - 1) {
            int nextThreshold = TIER_THRESHOLDS[currentIdx + 1];
            int needed = nextThreshold - cumulative;
            line += "\n  Earn " + needed + " more pts to reach " + Tier.values()[currentIdx + 1]
                    + " (needs " + nextThreshold + " cumulative pts).";
        } else {
            line += "\n  You have reached the highest tier!";
        }
        return line;
    }

    /** Creates and persists a notification for a member (via their guest id). */
    private void notifyMember(Member member, String type, String message, LocalDateTime now) {
        if (member == null || member.getGuestId() == null) {
            return;
        }
        notificationList.addSorted(new Notification(nextNotificationId(), type, message, now, false, member.getGuestId()));
        notificationDAO.saveToFile(notificationList);
    }

}
