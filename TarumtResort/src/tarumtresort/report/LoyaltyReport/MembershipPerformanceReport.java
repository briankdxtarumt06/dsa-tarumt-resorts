package tarumtresort.report.LoyaltyReport;

import java.time.LocalDateTime;
import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

// Author: Brian - extracted from LoyaltyController for Housekeeping-style reports
// Simplified to date-range-only filter (like Housekeeping) - no tier/minPoints/status/promo/keyword/sort
public class MembershipPerformanceReport {

    private final ListInterface<Member> memberList;
    private final ListInterface<Guest> guestList;

    public MembershipPerformanceReport(ListInterface<Member> memberList,
            ListInterface<Guest> guestList) {
        this.memberList = memberList == null ? new DoublyLinkedList<>() : memberList;
        this.guestList = guestList == null ? new DoublyLinkedList<>() : guestList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to, Tier tierFilter) {
        LocalDateTime now = LocalDateTime.now();
        ListInterface<Member> filtered = new DoublyLinkedList<>();

        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            // skip soft-deleted members so removed records don't pollute management summary
            if (m.isDeleted()) {
                continue;
            }
            // multiple criteria: date range AND tier filter
            if (tierFilter != null && (m.getTier() == null || m.getTier() != tierFilter)) {
                continue;
            }
            if (from != null || to != null) {
                if (!hasTxInRange(m, from, to)) {
                    continue;
                }
            }
            filtered.addBack(m);
        }

        // sort by memberId via addSorted (Member implements Comparable by memberId)
        // Use LinkedList addSorted to keep sorted order like RoomCleaning does
        ListInterface<Member> sorted = new DoublyLinkedList<>();
        for (int i = 0; i < filtered.size(); i++) {
            sorted.addSorted(filtered.get(i));
        }

        return new Result(toTable(sorted, now), buildCharts(sorted), buildSummary(sorted), buildCallouts(sorted),
                criteriaText(from, to, tierFilter));
    }

    private String criteriaText(LocalDateTime from, LocalDateTime to, Tier tierFilter) {
        String range = (from == null && to == null) ? "All Time"
                : (from == null ? ".." : from.toLocalDate().toString())
                + " .. " + (to == null ? ".." : to.toLocalDate().toString());
        return "Range: " + range + " | Tier: " + (tierFilter == null ? "ALL" : tierFilter.name());
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

    private int getCumulativeEarned(String memberId) {
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getMemberId().equals(memberId)) {
                var txs = memberList.get(i).getPointTransactionList();
                int t = 0;
                for (int j = 0; j < txs.size(); j++) if (txs.get(j).getPointChange() > 0) t += txs.get(j).getPointChange();
                return t;
            }
        }
        return 0;
    }

    private String[][] toTable(ListInterface<Member> rows, LocalDateTime now) {
        String[][] table = new String[rows.size() + 1][9];
        table[0] = new String[]{"No.", "Member ID", "Name", "Tier", "Balance", "Cum Earned", "Txns", "Redemptions", "Status"};
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
                m.isDeleted() ? "DELETED" : "ACTIVE"
            };
        }
        return table;
    }

    private ListInterface<ReportChart> buildCharts(ListInterface<Member> rows) {
        ListInterface<ReportChart> charts = new DoublyLinkedList<>();
        int[] perTier = new int[Tier.values().length];
        long[] balancePerTier = new long[Tier.values().length];
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            int idx = m.getTier() == null ? 0 : m.getTier().ordinal();
            perTier[idx]++;
            balancePerTier[idx] += m.getPoints();
        }
        ReportChart byTier = new ReportChart("Members per Tier");
        ReportChart avgByTier = new ReportChart("Avg Balance per Tier");
        for (Tier t : Tier.values()) {
            int count = perTier[t.ordinal()];
            byTier.addBar(t.name(), count, count + " member(s)");
            avgByTier.addBar(t.name(), count == 0 ? 0 : Math.round(balancePerTier[t.ordinal()] / count), count == 0 ? "0" : String.valueOf(count));
        }
        charts.addBack(byTier);
        charts.addBack(avgByTier);
        return charts;
    }

    private String[] buildSummary(ListInterface<Member> rows) {
        double balanceSum = 0;
        long cumSum = 0;
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            balanceSum += m.getPoints();
            cumSum += getCumulativeEarned(m.getMemberId());
        }
        long avg = rows.size() == 0 ? 0 : Math.round(balanceSum / rows.size());
        return new String[]{
            "TOTAL MEMBERS: " + rows.size(),
            "AVG BALANCE: " + avg + " pts",
            "TOTAL PTS IN CIRCULATION: " + Math.round(balanceSum)
        };
    }

    private ListInterface<String> buildCallouts(ListInterface<Member> rows) {
        ListInterface<String> callouts = new DoublyLinkedList<>();
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
