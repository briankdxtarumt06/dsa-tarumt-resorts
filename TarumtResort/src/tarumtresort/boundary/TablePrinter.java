package tarumtresort.boundary;

import java.util.List;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;

/**
 *
 * @author Brian
 *
 * Shared table / detail printing helpers used by every UI class so that all
 * module outputs look the same. Pure formatting logic is centralized here.
 * Charts and callouts are colored with ANSI codes (see {@link Ansi}).
 */
public class TablePrinter {

    private TablePrinter() {
    }

    // prints a 2D table where row 0 is the header; column widths auto-fit
    public static void printTable(String[][] data) {
        if (data == null || data.length == 0) {
            return;
        }

        int[] widths = new int[data[0].length];
        for (String[] row : data) {
            for (int col = 0; col < row.length; col++) {
                if (row[col] != null && row[col].length() > widths[col]) {
                    widths[col] = row[col].length();
                }
            }
        }

        for (int r = 0; r < data.length; r++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < data[r].length; col++) {
                String cell = data[r][col] == null ? "" : data[r][col];
                // bold the header row
                if (r == 0) {
                    cell = Ansi.bold(cell);
                }
                // pad by visible width (ANSI escape codes are invisible but
                // would otherwise break the alignment of the header row)
                int pad = (widths[col] + 3) - visibleLength(cell);
                if (pad > 0) {
                    sb.append(cell);
                    for (int i = 0; i < pad; i++) {
                        sb.append(' ');
                    }
                } else {
                    sb.append(cell);
                }
            }
            System.out.println(sb.toString());

            // underline after header row
            if (r == 0) {
                int totalWidth = 0;
                for (int w : widths) totalWidth += w + 3;
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < totalWidth; i++) line.append('-');
                System.out.println(line.toString());
            }
        }
    }

    // visible width of a cell: length without ANSI escape codes
    public static int visibleLength(String text) {
        if (text == null) {
            return 0;
        }
        return text.replaceAll("\u001B\\[[0-9;]*m", "").length();
    }

    // prints a key-value detail block (e.g. single record details)
    public static void printDetails(String[][] details) {
        if (details == null || details.length == 0) {
            return;
        }

        int keyWidth = 0;
        for (String[] pair : details) {
            if (pair[0] != null && pair[0].length() > keyWidth) {
                keyWidth = pair[0].length();
            }
        }

        System.out.println("\n--- Details ---");
        for (String[] pair : details) {
            System.out.println(String.format("%-" + (keyWidth + 3) + "s: %s",
                    pair[0] == null ? "" : pair[0],
                    pair.length > 1 && pair[1] != null ? pair[1] : "-"));
        }
        System.out.println("----------------------------");
    }

    // prints a summary line under a table (totals / averages)
    public static void printSummary(String summary) {
        if (summary == null || summary.isEmpty()) {
            return;
        }
        System.out.println(summary);
    }

    // prints multiple summary lines under a table
    public static void printSummary(String[] summaryLines) {
        if (summaryLines == null) {
            return;
        }
        for (String line : summaryLines) {
            printSummary(line);
        }
    }

    public static void printSeparator() {
        System.out.println("----------------------------");
    }

    // -------------------- charts & callouts --------------------

    // vertical column chart: every bar is 3 characters wide and grows upward.
    // The tallest bar is red, the shortest is green and the rest are cyan, so
    // high vs low values can be compared at a glance. Values and labels are
    // printed underneath each bar, centred in the bar slot.
    public static void printChart(ReportChart chart) {
        if (chart == null || chart.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println(Ansi.bold(Ansi.cyan("  " + chart.getTitle())));

        List<ReportChart.Bar> bars = chart.getBars();
        int n = bars.size();
        int maxHeight = 10;
        final int BAR_WIDTH = 3; // each bar occupies 3 characters
        final int GAP = 2;       // spacing between bars

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (ReportChart.Bar bar : bars) {
            max = Math.max(max, bar.getValue());
            min = Math.min(min, bar.getValue());
        }
        boolean allEqual = max == min;

        int[] heights = new int[n];
        for (int i = 0; i < n; i++) {
            heights[i] = max == 0 ? 0 : (int) Math.round(bars.get(i).getValue() / max * maxHeight);
        }

        // bars, drawn from the top row downwards
        for (int row = maxHeight; row >= 1; row--) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (heights[i] >= row) {
                    String color = allEqual ? Ansi.CYAN
                            : bars.get(i).getValue() == max ? Ansi.RED
                            : bars.get(i).getValue() == min ? Ansi.GREEN : Ansi.CYAN;
                    line.append(Ansi.color(color, "███"));
                } else {
                    line.append("   ");
                }
                if (i < n - 1) {
                    line.append("  ");
                }
            }
            System.out.println(line.toString());
        }

        // value row (bold), centred under each bar
        StringBuilder valueLine = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int slot = (i < n - 1) ? BAR_WIDTH + GAP : BAR_WIDTH;
            valueLine.append(centreIn(formatNumber(bars.get(i).getValue()), slot));
        }
        System.out.println(Ansi.bold(valueLine.toString()));

        // detail row (e.g. "(9 tasks)"), only when at least one bar has one
        boolean anyDetail = false;
        for (ReportChart.Bar bar : bars) {
            if (bar.getDetail() != null && !bar.getDetail().isBlank()) {
                anyDetail = true;
                break;
            }
        }
        if (anyDetail) {
            StringBuilder detailLine = new StringBuilder();
            for (int i = 0; i < n; i++) {
                int slot = (i < n - 1) ? BAR_WIDTH + GAP : BAR_WIDTH;
                String detail = bars.get(i).getDetail() == null ? "" : bars.get(i).getDetail();
                detailLine.append(centreIn(detail, slot));
            }
            System.out.println(detailLine.toString());
        }

        // label row, centred under each bar (long labels left-align / overflow)
        StringBuilder labelLine = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int slot = (i < n - 1) ? BAR_WIDTH + GAP : BAR_WIDTH;
            labelLine.append(centreIn(bars.get(i).getLabel() == null ? "" : bars.get(i).getLabel(), slot));
        }
        System.out.println(labelLine.toString());
    }

    // centres text within a slot; text longer than the slot is left-aligned
    private static String centreIn(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int pad = width - text.length();
        int left = pad / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < left; i++) {
            sb.append(' ');
        }
        sb.append(text);
        for (int i = 0; i < pad - left; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // prints a titled callout block; lines arrive pre-colored by the report
    public static void printCallouts(List<String> callouts) {
        if (callouts == null || callouts.isEmpty()) {
            return;
        }
        System.out.println();
        for (String line : callouts) {
            System.out.println("  " + line);
        }
    }

    // "42.3" -> "42.3", "42.0" -> "42"
    private static String formatNumber(double value) {
        String text = String.format("%.1f", value);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }
}