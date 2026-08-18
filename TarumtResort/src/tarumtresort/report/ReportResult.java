package tarumtresort.report;

import java.util.ArrayList;
import java.util.List;

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
    private final List<ReportChart> charts;
    private final List<String> callouts;

    public ReportResult(String[][] table, String[] summary) {
        this(table, summary, null, null);
    }

    public ReportResult(String[][] table, String[] summary,
            List<ReportChart> charts, List<String> callouts) {
        this.table = table;
        this.summary = summary == null ? new String[0] : summary;
        this.charts = charts == null ? new ArrayList<>() : charts;
        this.callouts = callouts == null ? new ArrayList<>() : callouts;
    }

    public String[][] getTable() {
        return table;
    }

    public String[] getSummary() {
        return summary;
    }

    public List<ReportChart> getCharts() {
        return charts;
    }

    public List<String> getCallouts() {
        return callouts;
    }

    public boolean isEmpty() {
        return table == null || table.length <= 1;
    }
}