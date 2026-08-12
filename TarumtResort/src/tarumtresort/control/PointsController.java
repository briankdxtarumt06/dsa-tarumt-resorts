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
    private final MemberDAO memberDAO;
    private final PointTransactionDAO pointTransactionDAO;
    private final RewardDAO rewardDAO;
    private final RedemptionRecordDAO redemptionRecordDAO;
    private final NotificationDAO notificationDAO;

    private final LinkedListInterface<Member> members = new LinkedList<>();
    private final LinkedListInterface<PointTransaction> pointTransactions = new LinkedList<>();
    private final LinkedListInterface<RedemptionRecord> redemptions = new LinkedList<>();
    private final LinkedListInterface<Reward> rewards = new LinkedList<>();
    private final LinkedListInterface<Notification> notifications = new LinkedList<>();

    public PointsController(MemberDAO memberDAO, PointTransactionDAO pointTransactionDAO,
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
        notificationDAO.LoadFromFile(notifications);
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
        pointTransactions.addSorted(t);
        recomputeBalance(member);
        recomputeTier(member);
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

    // ---------------------------------------------------------------
    // Expiry alerts (notifications)
    // ---------------------------------------------------------------

    /** Number of days before expiry that an alert is generated. */
    private static final int ALERT_DAYS_BEFORE_EXPIRY = 7;

    /**
     * Scans every transaction and creates a POINT_EXPIRY notification for
     * points expiring within ALERT_DAYS_BEFORE_EXPIRY days. Already-notified
     * transactions are skipped, so repeated calls never duplicate alerts.
     */
    public String generateExpiryAlerts(LocalDateTime now) {
        int created = 0;
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < pointTransactions.size(); i++) {
            PointTransaction t = pointTransactions.get(i);
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
            notifications.addSorted(new Notification(nextNotificationId(), "POINT_EXPIRY",
                    message, now, false, member.getGuestId()));
            created++;
            summary.append("  - ").append(message).append("\n");
        }
        if (created > 0) {
            notificationDAO.SaveToFile(notifications);
            return created + " expiry alert(s) generated:\n" + summary;
        }
        return "No new expiry alerts to generate.";
    }

    private boolean hasNotification(String guestId, String message) {
        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            if (n.getGuestId() != null && n.getGuestId().equals(guestId)
                    && n.getMessage().equals(message)) {
                return true;
            }
        }
        return false;
    }

    /** @return a member's notifications (via their guest id), newest first. */
    public LinkedListInterface<Notification> getNotifications(String guestId) {
        LinkedListInterface<Notification> result = new LinkedList<>();
        for (int i = 0; i < notifications.size(); i++) {
            if (guestId != null && guestId.equals(notifications.get(i).getGuestId())) {
                result.addBack(notifications.get(i));
            }
        }
        return result;
    }

    public String markNotificationRead(String notificationId) {
        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            if (n.getNotificationId().equals(notificationId)) {
                n.setRead(true);
                notificationDAO.SaveToFile(notifications);
                return "Notification " + notificationId + " marked as read.";
            }
        }
        return "Notification not found: " + notificationId;
    }

    private String nextNotificationId() {
        try {
            int max = 0;
            for (int i = 0; i < notifications.size(); i++) {
                String nid = notifications.get(i).getNotificationId();
                if (nid != null && nid.matches("NT\\d+")) {
                    int n = Integer.parseInt(nid.substring(2));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("NT%04d", max + 1);
        } catch (RuntimeException e) {
            return String.format("NT%04d", notifications.size() + 1);
        }
    }

    // ---------------------------------------------------------------
    // Tier progression (based on cumulative points ever earned)
    // ---------------------------------------------------------------

    /** Minimum cumulative earned points required for each tier. */
    private static final int[] TIER_THRESHOLDS = {0, 1000, 3000, 6000};

    /** Returns the tier for a cumulative earned total. */
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

    /**
     * Cumulative points the member has ever EARNED (sum of positive
     * pointChange). This never decreases when points are redeemed or expire,
     * so a member cannot drop a tier for using rewards.
     */
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

    /** Recomputes and sets a member's tier from their cumulative earned points. */
    public void recomputeTier(Member member) {
        Tier current = tierFor(getCumulativeEarned(member.getMemberId()));
        if (member.getTier() != current) {
            member.setTier(current);
        }
    }

    /**
     * Human-readable tier progression detail: current tier, cumulative earned,
     * and progress toward the next tier.
     */
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
}

