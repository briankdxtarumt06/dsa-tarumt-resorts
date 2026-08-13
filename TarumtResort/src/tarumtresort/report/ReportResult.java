package tarumtresort.report;

/**
 *
 * @author Brian
 *
 * Shared output holder for every report: a 2D table (row 0 = header) plus
 * summary lines (totals / averages) printed under the table.
 */
public class ReportResult {

    private final String[][] table;
    private final String[] summary;

    public ReportResult(String[][] table, String[] summary) {
        this.table = table;
        this.summary = summary == null ? new String[0] : summary;
    }

    public String[][] getTable() {
        return table;
    }

    public String[] getSummary() {
        return summary;
    }

    public boolean isEmpty() {
        return table == null || table.length <= 1;
    }
}