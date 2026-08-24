package tarumtresort.report;

import tarumtresort.adt.DoublyLinkedList;
import tarumtresort.adt.ListInterface;

// Author: Brian Kam Ding Xian
public class ReportResult {

    private final String[][] table;
    private final String[] summary;
    private final ListInterface<ReportChart> charts;
    private final ListInterface<String> callouts;
    private final String criteria;

    public ReportResult(String[][] table, String[] summary) {
        this(table, summary, (ListInterface<ReportChart>) null, null, null);
    }

    public ReportResult(String[][] table, String[] summary,
            ListInterface<ReportChart> charts, ListInterface<String> callouts) {
        this(table, summary, charts, callouts, null);
    }

    public ReportResult(String[][] table, String[] summary,
            ListInterface<ReportChart> charts, ListInterface<String> callouts,
            String criteria) {
        this.table = table;
        this.summary = summary == null ? new String[0] : summary;
        this.charts = charts == null ? new DoublyLinkedList<>() : charts;
        this.callouts = callouts == null ? new DoublyLinkedList<>() : callouts;
        this.criteria = criteria;
    }

    public String[][] getTable() {
        return table;
    }

    public String[] getSummary() {
        return summary;
    }

    public ListInterface<ReportChart> getCharts() {
        return charts;
    }

    public ListInterface<String> getCallouts() {
        return callouts;
    }

    public String getCriteria() {
        return criteria;
    }

    public boolean isEmpty() {
        return table == null || table.length <= 1;
    }
}