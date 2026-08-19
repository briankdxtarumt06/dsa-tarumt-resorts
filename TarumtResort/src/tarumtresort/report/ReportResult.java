package tarumtresort.report;

import java.util.List;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

/**
 *
 * @author Brian
 *
 * Shared output holder for every report: a 2D table (row 0 = header), summary
 * lines (totals / averages) printed under the table, plus optional ASCII
 * charts and callout sections. The original table+summary constructor stays
 * valid; charts/callouts default to empty.
 */
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

    // adapter: the inquiry module still supplies charts as java.util.List
    public ReportResult(String[][] table, String[] summary,
            List<ReportChart> charts, List<String> callouts) {
        this(table, summary, toAdtList(charts), toAdtList(callouts));
    }

    private static <T extends Comparable<T>> LinkedListInterface<T> toAdtList(List<T> source) {
        LinkedListInterface<T> result = new LinkedList<>();
        if (source != null) {
            for (T element : source) {
                result.addBack(element);
            }
        }
        return result;
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