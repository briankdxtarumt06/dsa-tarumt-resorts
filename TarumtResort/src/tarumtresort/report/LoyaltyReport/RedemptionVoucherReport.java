package tarumtresort.report.LoyaltyReport;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Housekeeping-style: date-range-only filter, no status/voucher-type/minCost/keyword/sort
public class RedemptionVoucherReport {

    private final LinkedListInterface<Member> memberList;
    private final LinkedListInterface<Reward> rewardList;
    private final LinkedListInterface<Guest> guestList;

    public RedemptionVoucherReport(LinkedListInterface<Member> memberList,
            LinkedListInterface<Reward> rewardList, LinkedListInterface<Guest> guestList) {
        this.memberList = memberList == null ? new LinkedList<>() : memberList;
        this.rewardList = rewardList == null ? new LinkedList<>() : rewardList;
        this.guestList = guestList == null ? new LinkedList<>() : guestList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {
        LinkedListInterface<RedemptionRecord> filtered = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            var recs = m.getRedemptionRecordList();
            for (int j = 0; j < recs.size(); j++) {
                RedemptionRecord r = recs.get(j);
                if (!inRange(r.getRedeemedDate(), from, to)) continue;
                filtered.addBack(r);
            }
        }
        // sort by redeemedDate via addSorted (RedemptionRecord implements Comparable by date)
        LinkedListInterface<RedemptionRecord> sorted = new LinkedList<>();
        for (int i = 0; i < filtered.size(); i++) sorted.addSorted(filtered.get(i));

        return new Result(toTable(sorted), buildCharts(sorted), buildSummary(sorted), buildCallouts(sorted));
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) return false;
        if (from != null && value.isBefore(from)) return false;
        if (to != null && value.isAfter(to)) return false;
        return true;
    }

    private Reward findReward(String rewardId) {
        for (int i = 0; i < rewardList.size(); i++) if (rewardList.get(i).getRewardId().equals(rewardId)) return rewardList.get(i);
        return null;
    }

    private Guest findGuest(String guestId) {
        if (guestId == null) return null;
        for (int i = 0; i < guestList.size(); i++) if (guestId.equals(guestList.get(i).getGuestId())) return guestList.get(i);
        return null;
    }

    private String guestName(Member m) {
        if (m == null || m.getGuestId() == null) return "-";
        Guest g = findGuest(m.getGuestId());
        return g == null || g.getName() == null ? "-" : g.getName();
    }

    private Member findMember(String memberId) {
        for (int i = 0; i < memberList.size(); i++) if (memberList.get(i).getMemberId().equals(memberId)) return memberList.get(i);
        return null;
    }

    private String truncateName(String text, int width) {
        if (text == null || text.length() <= width) return text == null ? "-" : text;
        return text.substring(0, width - 3) + "...";
    }

    private int redemptionCountFor(String rewardId) {
        int c = 0;
        for (int i = 0; i < memberList.size(); i++) {
            var recs = memberList.get(i).getRedemptionRecordList();
            for (int j = 0; j < recs.size(); j++) if (rewardId.equals(recs.get(j).getRewardId())) c++;
        }
        return c;
    }

    private String mostRedeemedReward() {
        String best = null; int bestCount = 0;
        for (int i = 0; i < rewardList.size(); i++) {
            Reward r = rewardList.get(i);
            int cnt = redemptionCountFor(r.getRewardId());
            if (cnt > bestCount) { bestCount = cnt; best = r.getName(); }
        }
        return bestCount == 0 ? null : best;
    }

    private String[][] toTable(LinkedListInterface<RedemptionRecord> rows) {
        String[][] table = new String[rows.size() + 1][10];
        table[0] = new String[]{"No.", "Redemption ID", "Member", "Reward", "Type", "Status", "Pts Cost", "Voucher", "Used", "Date"};
        int pending = 0, approved = 0, rejected = 0;
        for (int i = 0; i < rows.size(); i++) {
            RedemptionRecord r = rows.get(i);
            Member m = findMember(r.getMemberId());
            Reward reward = findReward(r.getRewardId());
            boolean isPercent = r.getDiscountPercent() != null;
            boolean isRM = r.getVoucherValue() != null;
            String typeText = isPercent ? r.getDiscountPercent() + "%" : (isRM ? "RM" : "Other");
            int cost = reward == null ? 0 : reward.getPointCost();
            table[i + 1] = new String[]{
                String.valueOf(i + 1), r.getRedemptionId(),
                m == null ? r.getMemberId() : r.getMemberId() + " " + guestName(m),
                reward == null ? r.getRewardId() : reward.getName(),
                typeText, r.getStatus(), String.valueOf(cost),
                r.getVoucherCode() == null ? "-" : r.getVoucherCode(),
                r.isUsed() ? "USED" : "-",
                r.getRedeemedDate() == null ? "-" : r.getRedeemedDate().toLocalDate().toString()
            };
        }
        return table;
    }

    private LinkedListInterface<ReportChart> buildCharts(LinkedListInterface<RedemptionRecord> rows) {
        int pending = 0, approved = 0, rejected = 0;
        for (int i = 0; i < rows.size(); i++) {
            String s = rows.get(i).getStatus();
            if ("PENDING".equals(s)) pending++; else if ("APPROVED".equals(s)) approved++; else rejected++;
        }
        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        ReportChart byStatus = new ReportChart("Redemptions by Status");
        byStatus.addBar("PENDING", pending, pending + " req(s)");
        byStatus.addBar("APPROVED", approved, approved + " req(s)");
        byStatus.addBar("REJECTED", rejected, rejected + " req(s)");
        charts.addBack(byStatus);

        ReportChart byReward = new ReportChart("Redemptions per Reward");
        for (int i = 0; i < rewardList.size(); i++) {
            Reward reward = rewardList.get(i);
            int cnt = redemptionCountFor(reward.getRewardId());
            if (cnt > 0) byReward.addBar(truncateName(reward.getName(), 8), cnt, cnt + " time(s)");
        }
        charts.addBack(byReward);
        return charts;
    }

    private String[] buildSummary(LinkedListInterface<RedemptionRecord> rows) {
        int pending = 0, approved = 0, rejected = 0;
        long totalCost = 0;
        int vouchersIssued = 0, vouchersUsed = 0;
        for (int i = 0; i < rows.size(); i++) {
            RedemptionRecord r = rows.get(i);
            Reward reward = findReward(r.getRewardId());
            int cost = reward == null ? 0 : reward.getPointCost();
            totalCost += cost;
            if ("PENDING".equals(r.getStatus())) pending++;
            else if ("APPROVED".equals(r.getStatus())) approved++;
            else rejected++;
            boolean voucher = r.getDiscountPercent() != null || r.getVoucherValue() != null || r.getVoucherCode() != null;
            if (voucher && "APPROVED".equals(r.getStatus())) vouchersIssued++;
            if (voucher && r.isUsed()) vouchersUsed++;
        }
        double approvalRate = (pending + approved) == 0 ? 0 : approved * 100.0 / (pending + approved);
        return new String[]{
            "TOTAL REDEMPTIONS: " + rows.size(),
            "PENDING: " + pending + " | APPROVED: " + approved + " | REJECTED: " + rejected,
            "TOTAL POINTS SPENT: " + totalCost,
            "VOUCHERS ISSUED: " + vouchersIssued + " | VOUCHERS USED: " + vouchersUsed,
            "APPROVAL RATE: " + Math.round(approvalRate) + "%"
        };
    }

    private LinkedListInterface<String> buildCallouts(LinkedListInterface<RedemptionRecord> rows) {
        LinkedListInterface<String> callouts = new LinkedList<>();
        String most = mostRedeemedReward();
        if (most != null) callouts.addBack("Most redeemed reward: " + most);
        return callouts;
    }

    public static class Result {
        private final String[][] table;
        private final LinkedListInterface<ReportChart> charts;
        private final String[] summary;
        private final LinkedListInterface<String> callouts;
        Result(String[][] table, LinkedListInterface<ReportChart> charts, String[] summary, LinkedListInterface<String> callouts) {
            this.table = table;
            this.charts = charts == null ? new LinkedList<>() : charts;
            this.summary = summary;
            this.callouts = callouts == null ? new LinkedList<>() : callouts;
        }
        public String[][] getTable() { return table; }
        public LinkedListInterface<ReportChart> getCharts() { return charts; }
        public String[] getSummary() { return summary; }
        public LinkedListInterface<String> getCallouts() { return callouts; }
    }
}
