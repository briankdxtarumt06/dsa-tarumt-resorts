package tarumtresort.report;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

// Author: Brian Kam Ding Xian
public class ReportResult {

    private final String[][] table;
    private final String[] summary;
    private final LinkedListInterface<ReportChart> charts;
    private final LinkedListInterface<String> callouts;

    public ReportResult(String[][] table, String[] summary) {
        this(table, summary, (LinkedListInterface<ReportChart>) null, null);
    }

    public ReportResult(String[][] table, String[] summary,
            LinkedListInterface<ReportChart> charts, LinkedListInterface<String> callouts) {
        this.table = table;
        this.summary = summary == null ? new String[0] : summary;
        this.charts = charts == null ? new LinkedList<>() : charts;
        this.callouts = callouts == null ? new LinkedList<>() : callouts;
    }

    public String[][] getTable() {
        return table;
    }

    public String[] getSummary() {
        return summary;
    }

    public LinkedListInterface<ReportChart> getCharts() {
        return charts;
    }

    public LinkedListInterface<String> getCallouts() {
        return callouts;
    }

    public boolean isEmpty() {
        return table == null || table.length <= 1;
    }
}