package tarumtresort.control;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Scanner;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.boundary.LoyaltyRewardsUI.MemberManagementUI;
import tarumtresort.boundary.LoyaltyRewardsUI.PointsManagementUI;
import tarumtresort.boundary.LoyaltyRewardsUI.RewardManagementUI;
import tarumtresort.dao.GuestDAO;
import tarumtresort.dao.MemberDAO;
import tarumtresort.dao.RewardDAO;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.Notification;
import tarumtresort.entity.PointTransaction;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.entity.enums.Tier;

/**
 * Combined controller for the loyalty module: member management, reward
 * catalogue management, points earning/expiry, and redemption requests.
 * Previously split across PointsController, MemberController and
 * RewardController; merged so all loyalty data is loaded once and shared.
 */
public class LoyaltyController {

    private LinkedListInterface<Member> memberList = new LinkedList<>();
    private LinkedListInterface<Reward> rewardList = new LinkedList<>();
    private LinkedListInterface<Guest> guestList = new LinkedList<>();

    private MemberDAO memberDAO = new MemberDAO();
    private RewardDAO rewardDAO = new RewardDAO();
    private GuestDAO guestDAO = new GuestDAO();

    private MemberManagementUI memberUI;
    private RewardManagementUI rewardUI;
    private PointsManagementUI pointsUI;

    public LoyaltyController() {
        this(new Scanner(System.in));
    }

    public LoyaltyController(Scanner scanner) {
        memberList = memberDAO.retrieveFromFile();
        rewardList = rewardDAO.retrieveFromFile();
        guestDAO.loadFromFile(guestList);
        memberUI = new MemberManagementUI(scanner);
        rewardUI = new RewardManagementUI(scanner);
        pointsUI = new PointsManagementUI(scanner);
        reconcileTiersOnLoad();
    }

    // ======================= MENU ENTRY POINTS =======================

    /** Member management menu (was MemberController.run). */
    public void runMemberMenu() {
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
                    listMembersFlow();
                    break;
                case 0:
                    memberUI.showMessage("Returning to main menu...");
                    break;
                default:
                    memberUI.showError("Invalid choice. Please enter 1 - 4 or 0 to exit.");
            }
        } while (choice != 0);
    }

    /** Reward catalogue menu (was RewardController.run). */
    public void runRewardMenu() {
        int choice;
        do {
            choice = rewardUI.getMenuChoice();
            switch (choice) {
                case 1:
                    addRewardFlow();
                    break;
                case 2:
                    removeRewardFlow();
                    break;
                case 3:
                    updateRewardFlow();
                    break;
                case 4:
                    rewardUI.displayRewards(rewardList);
                    rewardUI.pause();
                    break;
                case 0:
                    rewardUI.showMessage("Returning to main menu...");
                    break;
                default:
                    rewardUI.showError("Invalid choice. Please enter 1 - 4 or 0 to exit.");
            }
        } while (choice != 0);
    }

    /** Points, redemption and notification menu (was PointsController.run). */
    public void runPointsMenu() {
        String alert = generateExpiryAlerts(LocalDateTime.now());
        if (!alert.startsWith("No new")) {
            pointsUI.show(alert);
        }
        int choice;
        do {
            choice = pointsUI.getMenuChoice();
            switch (choice) {
                case 1:
                    viewBalanceFlow();
                    break;
                case 2:
                    earnPointsFlow();
                    break;
                case 3:
                    requestRedemptionFlow();
                    break;
                case 4:
                    runExpiryCheckFlow();
                    break;
                case 5:
                    viewHistoryFlow();
                    break;
                case 6:
                    viewTierProgressFlow();
                    break;
                case 7:
                    pointsUI.showMessage(generateExpiryAlerts(LocalDateTime.now()));
                    break;
                case 8:
                    viewNotificationsFlow();
                    break;
                case 9:
                    processRedemptionRequestsFlow();
                    break;
                case 0:
                    pointsUI.showMessage("Returning to main menu...");
                    break;
                default:
                    pointsUI.showError("Invalid choice. Please enter 1 - 9 or 0 to exit.");
            }
        } while (choice != 0);
    }

    // ======================= MEMBER MANAGEMENT =======================

    private void addMemberFlow() {
        String guestId = generateGuestId();
        Member member = memberUI.inputNewMember(nextMemberId(), guestId);
        if (member == null) {
            memberUI.showMessage("Operation cancelled.");
            return;
        }
        memberUI.showMessage(addMember(member));
    }

    // scan the loaded guest list for the highest "GSTxxx" id, malformed ids are skipped
    private String generateGuestId() {
        int max = 0;
        for (int i = 0; i < guestList.size(); i++) {
            String guestId = guestList.get(i).getGuestId();
            if (guestId != null && guestId.startsWith("GST")) {
                try {
                    int number = Integer.parseInt(guestId.substring(3));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("GST%03d", max + 1);
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

    /** Paginated member list; selecting a member opens its full profile. */
    private void listMembersFlow() {
        while (true) {
            String memberId = memberUI.displayMembersPaginated(memberList, this::findGuest);
            if (memberId == null) {
                return;
            }
            Member member = findMember(memberId);
            if (member == null) {
                continue;
            }
            memberUI.displayProfile(member, findGuest(member.getGuestId()));
            memberUI.pause();
        }
    }

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

    public String addMember(Member member) {
        if (member == null || member.getMemberId() == null) {
            return "Member cannot be null and must have an id.";
        }
        if (findMember(member.getMemberId()) != null) {
            return "Member id already exists: " + member.getMemberId();
        }
        memberList.addSorted(member);
        persistMembers();
        return "Member added: " + member.getMemberId() + " (Tier: " + member.getTier() + ").";
    }

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
        persistMembers();
        return "Member removed: " + memberId + ".";
    }

    public String updateMember(String memberId, Tier tier) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        member.setTier(tier);
        persistMembers();
        return "Member " + memberId + " updated to tier " + tier + ".";
    }

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

    // ======================= REWARD MANAGEMENT =======================

    private void addRewardFlow() {
        Reward reward = rewardUI.inputNewReward(nextRewardId());
        if (reward == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        rewardUI.showMessage(addReward(reward));
    }

    private void removeRewardFlow() {
        String rewardId = rewardUI.selectRewardId(rewardList, "Select a reward to remove");
        if (rewardId == null) {
            return;
        }
        rewardUI.showMessage(removeReward(rewardId));
    }

    private void updateRewardFlow() {
        String rewardId = rewardUI.selectRewardId(rewardList, "Select a reward to update");
        if (rewardId == null) {
            return;
        }
        Reward reward = findReward(rewardId);
        String name = rewardUI.promptWithDefault("New name", reward.getName());
        if (name == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        String description = rewardUI.promptWithDefault("New description", reward.getDescription());
        if (description == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        Integer cost = rewardUI.promptIntWithDefault("New point cost", reward.getPointCost());
        if (cost == null) {
            rewardUI.showMessage("Operation cancelled.");
            return;
        }
        rewardUI.showMessage(updateReward(rewardId, name, description, cost));
    }

    public LinkedListInterface<Reward> getRewards() {
        return rewardList;
    }

    public Reward findReward(String rewardId) {
        for (int i = 0; i < rewardList.size(); i++) {
            if (rewardList.get(i).getRewardId().equals(rewardId)) {
                return rewardList.get(i);
            }
        }
        return null;
    }

    public String addReward(Reward reward) {
        if (reward == null || reward.getRewardId() == null) {
            return "Reward cannot be null and must have an id.";
        }
        if (findReward(reward.getRewardId()) != null) {
            return "Reward id already exists: " + reward.getRewardId();
        }
        rewardList.addSorted(reward);
        persistRewards();
        return "Reward added: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    public String removeReward(String rewardId) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        LinkedListInterface<Reward> kept = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            if (!rewardList.get(i).getRewardId().equals(rewardId)) {
                kept.addBack(rewardList.get(i));
            }
        }
        rewardList.clear();
        for (int i = 0; i < kept.size(); i++) {
            rewardList.addBack(kept.get(i));
        }
        persistRewards();
        return "Reward removed: " + reward.getName() + " (" + rewardId + ").";
    }

    public String updateReward(String rewardId, String name, String description, int pointCost) {
        Reward reward = findReward(rewardId);
        if (reward == null) {
            return "Reward not found: " + rewardId;
        }
        reward.setName(name);
        reward.setDescription(description);
        reward.setPointCost(pointCost);
        LinkedListInterface<Reward> reordered = new LinkedList<>();
        for (int i = 0; i < rewardList.size(); i++) {
            reordered.addSorted(rewardList.get(i));
        }
        rewardList.clear();
        for (int i = 0; i < reordered.size(); i++) {
            rewardList.addBack(reordered.get(i));
        }
        persistRewards();
        return "Reward updated: " + reward.getName() + " (" + reward.getPointCost() + " pts).";
    }

    public String nextRewardId() {
        try {
            int max = 0;
            for (int i = 0; i < rewardList.size(); i++) {
                String rid = rewardList.get(i).getRewardId();
                if (rid != null && rid.matches("R\\d+")) {
                    int n = Integer.parseInt(rid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                }
            }
            return String.format("R%03d", max + 1);
        } catch (RuntimeException e) {
            return String.format("R%03d", rewardList.size() + 1);
        }
    }

    // ======================= POINTS & REDEMPTION =======================

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

    private void viewBalanceFlow() {
        String memberId = pointsUI.selectMember(memberList);
        if (memberId == null) {
            return;
        }
        Member member = findMember(memberId);
        pointsUI.displayBalance(member, getAvailableBalance(memberId, LocalDateTime.now()));
        pointsUI.pause();
    }

    private void earnPointsFlow() {
        String memberId = pointsUI.selectMember(memberList);
        if (memberId == null) {
            return;
        }
        int amount = pointsUI.inputAmount();
        if (amount == 0) {
            pointsUI.showMessage("Operation cancelled.");
            return;
        }
        String description = pointsUI.inputDescription();
        if (description == null) {
            pointsUI.showMessage("Operation cancelled.");
            return;
        }
        pointsUI.showMessage(earnPoints(memberId, amount, description, LocalDateTime.now()));
    }

    private void requestRedemptionFlow() {
        String memberId = pointsUI.selectMember(memberList);
        if (memberId == null) {
            return;
        }
        String rewardId = pointsUI.selectReward(rewardList);
        if (rewardId == null) {
            return;
        }
        pointsUI.showMessage(requestRedemption(memberId, rewardId, LocalDateTime.now()));
    }

    private void runExpiryCheckFlow() {
        String memberId = pointsUI.selectMember(memberList);
        if (memberId == null) {
            return;
        }
        pointsUI.showMessage(expirePoints(memberId, LocalDateTime.now()));
    }

    private void viewHistoryFlow() {
        String memberId = pointsUI.selectMember(memberList);
        if (memberId == null) {
            return;
        }
        pointsUI.displayTransactions(getTransactions(memberId));
        pointsUI.pause();
    }

    private void viewTierProgressFlow() {
        String memberId = pointsUI.selectMember(memberList);
        if (memberId == null) {
            return;
        }
        pointsUI.showMessage(getTierProgress(memberId));
    }

    private void viewNotificationsFlow() {
        String memberId = pointsUI.selectMember(memberList);
        if (memberId == null) {
            return;
        }
        Member member = findMember(memberId);
        if (member == null || member.getGuestId() == null) {
            pointsUI.showMessage("Member has no guest account linked.");
            return;
        }
        LinkedListInterface<Notification> list = getNotifications(member.getGuestId());
        pointsUI.displayNotifications(list);
        if (pointsUI.confirmMarkAllRead()) {
            for (int i = 0; i < list.size(); i++) {
                markNotificationRead(list.get(i).getNotificationId());
            }
            pointsUI.show("All notifications marked as read.");
        }
        pointsUI.pause();
    }

    private void processRedemptionRequestsFlow() {
        LinkedListInterface<RedemptionRecord> pending = getPendingRedemptions();
        String redemptionId = pointsUI.selectPendingRequest(pending);
        if (redemptionId == null) {
            return;
        }
        String answer = pointsUI.approveOrReject();
        if (answer == null) {
            pointsUI.showMessage("Operation cancelled.");
            return;
        }
        if ("a".equals(answer)) {
            pointsUI.showMessage(approveRedemption(redemptionId, LocalDateTime.now()));
        } else {
            pointsUI.showMessage(rejectRedemption(redemptionId, LocalDateTime.now()));
        }
    }

    public LinkedListInterface<PointTransaction> getPointTransactions() {
        LinkedListInterface<PointTransaction> result = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<PointTransaction> list = memberList.get(i).getPointTransactionList();
            for (int j = 0; j < list.size(); j++) {
                result.addBack(list.get(j));
            }
        }
        return result;
    }

    public LinkedListInterface<RedemptionRecord> getRedemptions() {
        LinkedListInterface<RedemptionRecord> result = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < list.size(); j++) {
                result.addBack(list.get(j));
            }
        }
        return result;
    }

    public LinkedListInterface<PointTransaction> getTransactions(String memberId) {
        LinkedListInterface<PointTransaction> result = new LinkedList<>();
        Member member = findMember(memberId);
        if (member == null) {
            return result;
        }
        LinkedListInterface<PointTransaction> list = member.getPointTransactionList();
        for (int i = 0; i < list.size(); i++) {
            result.addBack(list.get(i));
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
            persistMembers();
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
        member.getPointTransactionList().addSorted(t);
        recomputeBalance(member);
        recomputeTier(member, date);
        persistMembers();
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
        member.getRedemptionRecordList().addSorted(new RedemptionRecord(nextRedemptionId(), now, memberId, rewardId));
        persistMembers();
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

        // voucher-type rewards: issue a redeemable code and lock in the RM value
        String voucherNote = "";
        if (reward.getVoucherValue() != null && reward.getVoucherValue() > 0) {
            record.setVoucherCode(generateVoucherCode(redemptionId));
            record.setVoucherValue(reward.getVoucherValue());
            voucherNote = " Voucher code: " + record.getVoucherCode()
                    + " (worth RM" + String.format("%.2f", reward.getVoucherValue()) + ").";
        }
        notifyMember(member, "REDEMPTION_APPROVED",
                "Your redemption request " + redemptionId + " (" + reward.getName() + ") has been approved."
                        + voucherNote, now);
        persistMembers();
        return "Approved " + redemptionId + " (" + reward.getName() + "):\n" + breakdown
                + (voucherNote.isEmpty() ? "" : "  - voucher " + record.getVoucherCode()
                        + " worth RM" + String.format("%.2f", record.getVoucherValue()) + "\n")
                + "New balance for " + member.getMemberId() + ": " + member.getPoints();
    }

    /**
     * Returns the member's unused, approved voucher redemptions - the vouchers
     * that can be offered to the customer at payment time.
     *
     * @param memberId the member to look up
     * @return approved, unused vouchers (empty list if none)
     */
    public LinkedListInterface<RedemptionRecord> getAvailableVouchers(String memberId) {
        LinkedListInterface<RedemptionRecord> result = new LinkedList<>();
        Member member = findMember(memberId);
        if (member == null) {
            return result;
        }
        LinkedListInterface<RedemptionRecord> list = member.getRedemptionRecordList();
        for (int i = 0; i < list.size(); i++) {
            RedemptionRecord r = list.get(i);
            if ("APPROVED".equals(r.getStatus())
                    && !r.isUsed()
                    && r.getVoucherCode() != null
                    && r.getVoucherValue() != null && r.getVoucherValue() > 0) {
                result.addBack(r);
            }
        }
        return result;
    }

    /**
     * Marks an approved, unused voucher as used. Called by the payment flow
     * once the voucher value has been applied to the customer's bill.
     *
     * @param memberId     the member who owns the voucher
     * @param redemptionId the redemption (voucher) to mark used
     * @return a result message for the UI
     */
    public String useVoucher(String memberId, String redemptionId) {
        Member member = findMember(memberId);
        if (member == null) {
            return "Member not found: " + memberId;
        }
        LinkedListInterface<RedemptionRecord> list = member.getRedemptionRecordList();
        for (int i = 0; i < list.size(); i++) {
            RedemptionRecord r = list.get(i);
            if (r.getRedemptionId().equals(redemptionId)) {
                if (!"APPROVED".equals(r.getStatus())) {
                    return "Voucher " + redemptionId + " is not approved ("
                            + r.getStatus() + ").";
                }
                if (r.isUsed()) {
                    return "Voucher " + redemptionId + " has already been used.";
                }
                if (r.getVoucherCode() == null) {
                    return "Redemption " + redemptionId + " is not a voucher.";
                }
                r.setUsed(true);
                persistMembers();
                return "Voucher " + r.getVoucherCode() + " (RM"
                        + String.format("%.2f", r.getVoucherValue()) + ") marked as used.";
            }
        }
        return "Voucher not found: " + redemptionId;
    }

    /** Generates a unique-looking voucher code, e.g. VCH-RR0001-8F3K. */
    private String generateVoucherCode(String redemptionId) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder("VCH-").append(redemptionId).append('-');
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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
        persistMembers();
        return "Rejected redemption request " + redemptionId + ".";
    }

    public LinkedListInterface<RedemptionRecord> getPendingRedemptions() {
        LinkedListInterface<RedemptionRecord> result = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < list.size(); j++) {
                if ("PENDING".equals(list.get(j).getStatus())) {
                    result.addBack(list.get(j));
                }
            }
        }
        return result;
    }

    private RedemptionRecord findRedemption(String redemptionId) {
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<RedemptionRecord> list = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).getRedemptionId().equals(redemptionId)) {
                    return list.get(j);
                }
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

    private void persistMembers() {
        memberDAO.saveToFile(memberList);
    }

    private void persistRewards() {
        rewardDAO.saveToFile(rewardList);
    }

    private String nextTransactionId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) {
                LinkedListInterface<PointTransaction> tlist = memberList.get(i).getPointTransactionList();
                for (int j = 0; j < tlist.size(); j++) {
                    String tid = tlist.get(j).getTransactionId();
                    if (tid != null && tid.matches("PT\\d+")) {
                        int n = Integer.parseInt(tid.substring(2));
                        if (n > max) {
                            max = n;
                        }
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
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<PointTransaction> tlist = memberList.get(i).getPointTransactionList();
            for (int j = 0; j < tlist.size(); j++) {
                if (tlist.get(j).getTransactionId().equals(transactionId)) {
                    return tlist.get(j);
                }
            }
        }
        return null;
    }

    private String nextRedemptionId() {
        try {
            int max = 0;
            for (int i = 0; i < memberList.size(); i++) {
                LinkedListInterface<RedemptionRecord> rlist = memberList.get(i).getRedemptionRecordList();
                for (int j = 0; j < rlist.size(); j++) {
                    String rid = rlist.get(j).getRedemptionId();
                    if (rid != null && rid.matches("RR\\d+")) {
                        int n = Integer.parseInt(rid.substring(2));
                        if (n > max) {
                            max = n;
                        }
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
        for (int i = 0; i < memberList.size(); i++) {
            LinkedListInterface<PointTransaction> tlist = memberList.get(i).getPointTransactionList();
            for (int j = 0; j < tlist.size(); j++) {
            PointTransaction t = tlist.get(j);
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
            notifyMember(member, "POINT_EXPIRY", message, now);
            created++;
            summary.append("  - ").append(message).append("\n");
            }
        }
        if (created > 0) {
            return created + " expiry alert(s) generated:\n" + summary;
        }
        return "No new expiry alerts to generate.";
    }

    private boolean hasNotification(String guestId, String message) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return false;
        }
        LinkedListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getMessage().equals(message)) {
                return true;
            }
        }
        return false;
    }

    public LinkedListInterface<Notification> getNotifications(String guestId) {
        Guest guest = findGuest(guestId);
        if (guest == null) {
            return new LinkedList<>();
        }
        LinkedListInterface<Notification> result = new LinkedList<>();
        LinkedListInterface<Notification> list = guest.getNotificationList();
        for (int i = 0; i < list.size(); i++) {
            result.addBack(list.get(i));
        }
        return result;
    }

    public String markNotificationRead(String notificationId) {
        for (int i = 0; i < guestList.size(); i++) {
            LinkedListInterface<Notification> list = guestList.get(i).getNotificationList();
            for (int j = 0; j < list.size(); j++) {
                Notification n = list.get(j);
                if (n.getNotificationId().equals(notificationId)) {
                    n.setRead(true);
                    guestDAO.saveToFile(guestList);
                    return "Notification " + notificationId + " marked as read.";
                }
            }
        }
        return "Notification not found: " + notificationId;
    }

    private String nextNotificationId() {
        try {
            int max = 0;
            for (int i = 0; i < guestList.size(); i++) {
                LinkedListInterface<Notification> list = guestList.get(i).getNotificationList();
                for (int j = 0; j < list.size(); j++) {
                    String nid = list.get(j).getNotificationId();
                    if (nid != null && nid.matches("NT\\d+")) {
                        int n = Integer.parseInt(nid.substring(2));
                        if (n > max) {
                            max = n;
                        }
                    }
                }
            }
            return String.format("NT%04d", max + 1);
        } catch (RuntimeException e) {
            return String.format("NT%04d", 1);
        }
    }

    public Guest findGuest(String guestId) {
        for (int i = 0; i < guestList.size(); i++) {
            if (guestList.get(i).getGuestId().equals(guestId)) {
                return guestList.get(i);
            }
        }
        return null;
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
                String message = "Congratulations! You have been upgraded to " + current
                        + "! You now enjoy " + current.getDiscountPercent() + "% off stays & dining.";
                notifyMember(member, "TIER_UPGRADE", message, now);
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

    private void notifyMember(Member member, String type, String message, LocalDateTime now) {
        if (member == null || member.getGuestId() == null) {
            return;
        }
        Guest guest = findGuest(member.getGuestId());
        if (guest == null) {
            return;
        }
        guest.addNotification(new Notification(nextNotificationId(), type, message, now, false));
        guestDAO.saveToFile(guestList);
    }
}