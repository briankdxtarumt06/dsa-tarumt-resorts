package tarumtresort.report.LoyaltyReport;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Brian - extracted from LoyaltyController for Housekeeping-style reports
// Simplified to date-range-only filter (like Housekeeping) - no tier/minPoints/status/promo/keyword/sort
public class MembershipPerformanceReport {

    private final LinkedListInterface<Member> memberList;
    private final LinkedListInterface<Guest> guestList;

    public MembershipPerformanceReport(LinkedListInterface<Member> memberList,
            LinkedListInterface<Guest> guestList) {
        this.memberList = memberList == null ? new LinkedList<>() : memberList;
        this.guestList = guestList == null ? new LinkedList<>() : guestList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();
        LinkedListInterface<Member> filtered = new LinkedList<>();

        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            // keep active and deleted both? Housekeeping shows non-deleted only, but membership shows both with Status column.
            // For date-range-only version, show all members that have a transaction in range, or all if no range.
            if (from != null || to != null) {
                if (!hasTxInRange(m, from, to)) {
                    continue;
                }
            }
            filtered.addBack(m);
        }

        // sort by memberId via addSorted (Member implements Comparable by memberId)
        // Use LinkedList addSorted to keep sorted order like RoomCleaning does
        LinkedListInterface<Member> sorted = new LinkedList<>();
        for (int i = 0; i < filtered.size(); i++) {
            sorted.addSorted(filtered.get(i));
        }

        return new Result(toTable(sorted, now), buildCharts(sorted), buildSummary(sorted), buildCallouts(sorted));
    }

    private boolean hasTxInRange(Member m, LocalDateTime from, LocalDateTime to) {
        var txs = m.getPointTransactionList();
        for (int i = 0; i < txs.size(); i++) {
            LocalDateTime d = txs.get(i).getDate();
            if (d != null && !d.isBefore(from == null ? LocalDateTime.MIN : from)
                    && !d.isAfter(to == null ? LocalDateTime.MAX : to)) {
                return true;
            }
        }
        return false;
    }

    private Guest findGuest(String guestId) {
        if (guestId == null) return null;
        for (int i = 0; i < guestList.size(); i++) {
            if (guestId.equals(guestList.get(i).getGuestId())) return guestList.get(i);
        }
        return null;
    }

    private String guestName(Member m) {
        Guest g = m == null || m.getGuestId() == null ? null : findGuest(m.getGuestId());
        return g == null || g.getName() == null ? "-" : g.getName();
    }

    private String promoText(Member m, LocalDateTime now) {
        if (m.hasActivePromotion(now)) return m.promotionLabel(now);
        if (m.getPromotionName() != null) return m.getPromotionName() + " (expired)";
        return "-";
    }

    private int getCumulativeEarned(String memberId) {
        int total = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (!m.getMemberId().equals(memberId)) continue;
            var txs = m.getPointTransactionList();
            for (int j = 0; j < txs.size(); j++) {
                int change = txs.get(j).getPointChange();
                if (change > 0) total += change;
            }
        }
        // also check direct member lookup
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getMemberId().equals(memberId)) {
                var txs = memberList.get(i).getPointTransactionList();
                int t = 0;
                for (int j = 0; j < txs.size(); j++) if (txs.get(j).getPointChange() > 0) t += txs.get(j).getPointChange();
                return t;
            }
        }
        return total;
    }

    private double balanceSumOfTier(Tier t) {
        double sum = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (m.getTier() == t && !m.isDeleted()) sum += m.getPoints();
        }
        return sum;
    }

    private int memberCountOfTier(Tier t) {
        int c = 0;
        for (int i = 0; i < memberList.size(); i++) if (!memberList.get(i).isDeleted() && memberList.get(i).getTier() == t) c++;
        return c;
    }

    private String[][] toTable(LinkedListInterface<Member> rows, LocalDateTime now) {
        String[][] table = new String[rows.size() + 1][10];
        table[0] = new String[]{"No.", "Member ID", "Name", "Tier", "Balance", "Cum Earned", "Txns", "Redemptions", "Promotion", "Status"};
        int[] perTier = new int[Tier.values().length];
        double balanceSum = 0;
        long cumSum = 0;
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            int cumulative = getCumulativeEarned(m.getMemberId());
            perTier[m.getTier() == null ? 0 : m.getTier().ordinal()]++;
            balanceSum += m.getPoints();
            cumSum += cumulative;
            table[i + 1] = new String[]{
                String.valueOf(i + 1), m.getMemberId(), guestName(m),
                m.getTier() == null ? "-" : m.getTier().name(),
                String.valueOf(m.getPoints()),
                String.valueOf(cumulative),
                String.valueOf(m.getPointTransactionList().size()),
                String.valueOf(m.getRedemptionRecordList().size()),
                promoText(m, now),
                m.isDeleted() ? "DELETED" : "ACTIVE"
            };
        }
        return table;
    }

    private LinkedListInterface<ReportChart> buildCharts(LinkedListInterface<Member> rows) {
        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        int[] perTier = new int[Tier.values().length];
        for (int i = 0; i < rows.size(); i++) perTier[rows.get(i).getTier() == null ? 0 : rows.get(i).getTier().ordinal()]++;
        ReportChart byTier = new ReportChart("Members per Tier");
        ReportChart avgByTier = new ReportChart("Avg Balance per Tier");
        double totalBalance = 0;
        for (int i = 0; i < rows.size(); i++) totalBalance += rows.get(i).getPoints();
        for (Tier t : Tier.values()) {
            byTier.addBar(t.name(), perTier[t.ordinal()], perTier[t.ordinal()] + " member(s)");
            int count = memberCountOfTier(t);
            avgByTier.addBar(t.name(), count == 0 ? 0 : Math.round(balanceSumOfTier(t) / count), count == 0 ? "0" : String.valueOf(count));
        }
        charts.addBack(byTier);
        charts.addBack(avgByTier);
        return charts;
    }

    private String[] buildSummary(LinkedListInterface<Member> rows) {
        double balanceSum = 0;
        long cumSum = 0;
        int withPromo = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            balanceSum += m.getPoints();
            cumSum += getCumulativeEarned(m.getMemberId());
            if (m.hasActivePromotion(now)) withPromo++;
        }
        long avg = rows.size() == 0 ? 0 : Math.round(balanceSum / rows.size());
        return new String[]{
            "TOTAL MEMBERS: " + rows.size(),
            "AVG BALANCE: " + avg + " pts",
            "TOTAL PTS IN CIRCULATION: " + Math.round(balanceSum),
            "MEMBERS WITH ACTIVE PROMOTION: " + withPromo
        };
    }

    private LinkedListInterface<String> buildCallouts(LinkedListInterface<Member> rows) {
        LinkedListInterface<String> callouts = new LinkedList<>();
        if (rows.isEmpty()) return callouts;
        Member top = rows.get(0);
        for (int i = 1; i < rows.size(); i++) {
            if (getCumulativeEarned(rows.get(i).getMemberId()) > getCumulativeEarned(top.getMemberId())) top = rows.get(i);
        }
        callouts.addBack("Top member by cumulative earnings: " + top.getMemberId() + " " + guestName(top) + " (" + getCumulativeEarned(top.getMemberId()) + " pts)");
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
