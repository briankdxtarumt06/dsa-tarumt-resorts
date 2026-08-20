package tarumtresort.report.LoyaltyReport;

import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Guest;
import tarumtresort.entity.Member;
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.ReportChart;

// Housekeeping-style: date-range-only; window derived from range (if no range, no expiry filter)
public class PointExpiryReport {

    private static final int[] TIER_THRESHOLDS = {0, 1000, 3000, 6000};

    private final LinkedListInterface<Member> memberList;
    private final LinkedListInterface<Guest> guestList;

    public PointExpiryReport(LinkedListInterface<Member> memberList, LinkedListInterface<Guest> guestList) {
        this.memberList = memberList == null ? new LinkedList<>() : memberList;
        this.guestList = guestList == null ? new LinkedList<>() : guestList;
    }

    public Result generate(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();
        LinkedListInterface<Member> filtered = new LinkedList<>();
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (m.isDeleted()) continue;
            // date-range filter: keep member if they have any transaction in range, or if no range, keep all
            if (from != null || to != null) {
                boolean hasTx = false;
                var txs = m.getPointTransactionList();
                for (int j = 0; j < txs.size(); j++) {
                    LocalDateTime d = txs.get(j).getDate();
                    if (d != null && !d.isBefore(from == null ? LocalDateTime.MIN : from) && !d.isAfter(to == null ? LocalDateTime.MAX : to)) { hasTx = true; break; }
                }
                if (!hasTx) continue;
            }
            filtered.addBack(m);
        }
        LinkedListInterface<Member> sorted = new LinkedList<>();
        for (int i = 0; i < filtered.size(); i++) sorted.addSorted(filtered.get(i));

        // For expiry, derive window from date range: if range provided, window = days between from/to, else use 30 as default for chart
        int windowForChart = 30;
        if (from != null && to != null) {
            windowForChart = (int) java.time.Duration.between(from, to).toDays();
            if (windowForChart <= 0) windowForChart = 30;
        }

        return new Result(toTable(sorted, windowForChart, now), buildCharts(sorted, windowForChart, now), buildSummary(sorted, windowForChart, now), buildCallouts(sorted, windowForChart, now));
    }

    private Guest findGuest(String guestId) {
        if (guestId == null) return null;
        for (int i = 0; i < guestList.size(); i++) if (guestId.equals(guestList.get(i).getGuestId())) return guestList.get(i);
        return null;
    }

    private String guestName(Member m) {
        Guest g = m == null || m.getGuestId() == null ? null : findGuest(m.getGuestId());
        return g == null || g.getName() == null ? "-" : g.getName();
    }

    private int getCumulativeEarned(String memberId) {
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getMemberId().equals(memberId)) {
                int t = 0;
                var txs = memberList.get(i).getPointTransactionList();
                for (int j = 0; j < txs.size(); j++) if (txs.get(j).getPointChange() > 0) t += txs.get(j).getPointChange();
                return t;
            }
        }
        return 0;
    }

    private int expiringWithin(Member m, int windowDays, LocalDateTime now) {
        int sum = 0;
        var txs = m.getPointTransactionList();
        for (int i = 0; i < txs.size(); i++) {
            var t = txs.get(i);
            if (t.getRemainingPoints() > 0 && t.getExpiryDate() != null && !t.isExpired(now) && t.getExpiryDate().isBefore(now.plusDays(windowDays))) sum += t.getRemainingPoints();
        }
        return sum;
    }

    private int memberCountOfTier(Tier t) {
        int c = 0;
        for (int i = 0; i < memberList.size(); i++) if (!memberList.get(i).isDeleted() && memberList.get(i).getTier() == t) c++;
        return c;
    }

    private int expiringOfTier(Tier t, int window, LocalDateTime now) {
        int sum = 0;
        for (int i = 0; i < memberList.size(); i++) {
            Member m = memberList.get(i);
            if (!m.isDeleted() && m.getTier() == t) sum += expiringWithin(m, window, now);
        }
        return sum;
    }

    private String[][] toTable(LinkedListInterface<Member> rows, int window, LocalDateTime now) {
        String[][] table = new String[rows.size() + 1][9];
        table[0] = new String[]{"No.", "Member ID", "Name", "Tier", "Balance", "Cum Earned", "Next Tier", "Pts to Next", "Expiring <= " + window + "d"};
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            int cumulative = getCumulativeEarned(m.getMemberId());
            int expiring = window > 0 ? expiringWithin(m, window, now) : 0;
            Tier current = m.getTier() == null ? Tier.SILVER : m.getTier();
            String nextTier = "-"; String ptsToNext = "-";
            if (current.ordinal() < Tier.values().length - 1) {
                Tier next = Tier.values()[current.ordinal() + 1];
                nextTier = next.name();
                ptsToNext = String.valueOf(TIER_THRESHOLDS[current.ordinal() + 1] - cumulative);
            }
            table[i + 1] = new String[]{
                String.valueOf(i + 1), m.getMemberId(), guestName(m),
                m.getTier() == null ? "-" : m.getTier().name(),
                String.valueOf(m.getPoints()),
                String.valueOf(cumulative),
                nextTier, ptsToNext,
                String.valueOf(expiring)
            };
        }
        return table;
    }

    private LinkedListInterface<ReportChart> buildCharts(LinkedListInterface<Member> rows, int window, LocalDateTime now) {
        LinkedListInterface<ReportChart> charts = new LinkedList<>();
        ReportChart byTier = new ReportChart("Members per Tier");
        ReportChart expTier = new ReportChart("Expiring Pts per Tier");
        for (Tier t : Tier.values()) {
            byTier.addBar(t.name(), memberCountOfTier(t), memberCountOfTier(t) + " member(s)");
            expTier.addBar(t.name(), expiringOfTier(t, window, now), "pts");
        }
        charts.addBack(byTier);
        charts.addBack(expTier);
        return charts;
    }

    private String[] buildSummary(LinkedListInterface<Member> rows, int window, LocalDateTime now) {
        long cumSum = 0;
        long expiringSum = 0;
        int nearExpiry = 0;
        for (int i = 0; i < rows.size(); i++) {
            Member m = rows.get(i);
            cumSum += getCumulativeEarned(m.getMemberId());
            int expiring = window > 0 ? expiringWithin(m, window, now) : 0;
            expiringSum += expiring;
            if (expiring > 0) nearExpiry++;
        }
        long avg = rows.size() == 0 ? 0 : Math.round((double) cumSum / rows.size());
        return new String[]{
            "TOTAL MEMBERS: " + rows.size(),
            "AVG CUMULATIVE EARNED: " + avg + " pts",
            "MEMBERS WITH POINTS EXPIRING <= " + window + "d: " + nearExpiry,
            "TOTAL EXPIRING POINTS: " + expiringSum
        };
    }

    private LinkedListInterface<String> buildCallouts(LinkedListInterface<Member> rows, int window, LocalDateTime now) {
        LinkedListInterface<String> callouts = new LinkedList<>();
        if (window > 0) {
            for (int i = 0; i < rows.size(); i++) {
                Member m = rows.get(i);
                int expiring = expiringWithin(m, window, now);
                if (expiring > 0) callouts.addBack(m.getMemberId() + " " + guestName(m) + " has " + expiring + " pts expiring within " + window + " day(s).");
            }
        }
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
