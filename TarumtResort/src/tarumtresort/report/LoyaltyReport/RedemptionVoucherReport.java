package tarumtresort.report.LoyaltyReport;

import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.entity.Reward;
import tarumtresort.report.ReportChart;

// Author: Imam Mahdi Ali Ang Attuko
public class RedemptionVoucherReport {

    private final ListInterface<Member> memberList;
    private final ListInterface<Reward> rewardList;
    private final ListInterface<Guest> guestList;

    public RedemptionVoucherReport(ListInterface<Member> memberList,
            ListInterface<Reward> rewardList, ListInterface<Guest> guestList) {
        this.memberList = memberList == null ? new DoublyLinkedList<>() : memberList;
        this.rewardList = rewardList == null ? new DoublyLinkedList<>() : rewardList;
        this.guestList = guestList == null ? new DoublyLinkedList<>() : guestList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to, String statusFilter) {
        ListInterface<RedemptionRecord> filtered = new DoublyLinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            // consistent soft-delete policy: skip removed members' records
            if (m.isDeleted()) continue;
            var recs = m.getRedemptionRecordList();
            for (int j = 0; j < recs.size(); j++) {
                RedemptionRecord r = recs.get(j);
                // multiple criteria: date range AND redemption status
                if (!inRange(r.getRedeemedDate(), from, to)) continue;
                if (statusFilter != null && !statusFilter.equals(r.getStatus())) continue;
                filtered.addBack(r);
            }
        }
        // sort by redeemedDate via addSorted (RedemptionRecord implements Comparable by date)
        ListInterface<RedemptionRecord> sorted = new DoublyLinkedList<>();
        for (int i = 0; i < filtered.size(); i++) sorted.addSorted(filtered.get(i));

        return new Result(toTable(sorted), buildCharts(sorted), buildSummary(sorted), buildCallouts(sorted),
                criteriaText(from, to, statusFilter));
    }

    private String criteriaText(LocalDateTime from, LocalDateTime to, String statusFilter) {
        String range = (from == null && to == null) ? "All Time"
                : (from == null ? ".." : from.toLocalDate().toString())
                + " .. " + (to == null ? ".." : to.toLocalDate().toString());
        return "Range: " + range + " | Status: " + (statusFilter == null ? "ALL" : statusFilter);
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

    private int countInRows(ListInterface<RedemptionRecord> rows, String rewardId) {
        int c = 0;
        for (int i = 0; i < rows.size(); i++) if (rewardId.equals(rows.get(i).getRewardId())) c++;
        return c;
    }

    private String mostRedeemedReward(ListInterface<RedemptionRecord> rows) {
        String best = null; int bestCount = 0;
        for (int i = 0; i < rewardList.size(); i++) {
            Reward r = rewardList.get(i);
            int cnt = countInRows(rows, r.getRewardId());
            if (cnt > bestCount) { bestCount = cnt; best = r.getName(); }
        }
        return bestCount == 0 ? null : best;
    }

    private String[][] toTable(ListInterface<RedemptionRecord> rows) {
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

    private ListInterface<ReportChart> buildCharts(ListInterface<RedemptionRecord> rows) {
        int pending = 0, approved = 0, rejected = 0;
        for (int i = 0; i < rows.size(); i++) {
            String s = rows.get(i).getStatus();
            if ("PENDING".equals(s)) pending++; else if ("APPROVED".equals(s)) approved++; else rejected++;
        }
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();
        ReportChart byStatus = new ReportChart("Redemptions by Status");
        byStatus.addBar("PENDING", pending, pending + " req(s)");
        byStatus.addBar("APPROVED", approved, approved + " req(s)");
        byStatus.addBar("REJECTED", rejected, rejected + " req(s)");
        charts.addBack(byStatus);

        ReportChart byReward = new ReportChart("Redemptions per Reward");
        for (int i = 0; i < rewardList.size(); i++) {
            Reward reward = rewardList.get(i);
            int cnt = countInRows(rows, reward.getRewardId());
            if (cnt > 0) byReward.addBar(truncateName(reward.getName(), 8), cnt, cnt + " time(s)");
        }
        charts.addBack(byReward);
        return charts;
    }

    private String[] buildSummary(ListInterface<RedemptionRecord> rows) {
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

    private ListInterface<String> buildCallouts(ListInterface<RedemptionRecord> rows) {
        ListInterface<String> callouts = new DoublyLinkedList<>();
        String most = mostRedeemedReward(rows);
        if (most != null) callouts.addBack("Most redeemed reward: " + most);
        return callouts;
    }

    public static class Result {
        private final String[][] table;
        private final ListInterface<ReportChart> charts;
        private final String[] summary;
        private final ListInterface<String> callouts;
        private final String criteria;

        Result(String[][] table, ListInterface<ReportChart> charts, String[] summary, ListInterface<String> callouts) {
            this(table, charts, summary, callouts, null);
        }

        Result(String[][] table, ListInterface<ReportChart> charts, String[] summary, ListInterface<String> callouts, String criteria) {
            this.table = table;
            this.charts = charts == null ? new DoublyLinkedList<>() : charts;
            this.summary = summary;
            this.callouts = callouts == null ? new DoublyLinkedList<>() : callouts;
            this.criteria = criteria;
        }

        public String[][] getTable() { return table; }
        public ListInterface<ReportChart> getCharts() { return charts; }
        public String[] getSummary() { return summary; }
        public ListInterface<String> getCallouts() { return callouts; }
        public String getCriteria() { return criteria; }
    }
}
