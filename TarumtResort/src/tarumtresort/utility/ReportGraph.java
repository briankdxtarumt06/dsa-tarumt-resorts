package tarumtresort.utility;

import java.util.List;
import tarumtresort.report.ReportChart;

/**
 *
 * @author Brian
 *
 * Shared chart / callout printing helpers for the formal report document.
 * Renders side-by-side ASCII bar charts with dynamic Y-axis scaling and
 * two-row label wrapping. All output is plain text (ANSI stripped).
 */
public class ReportGraph {

    // bar slot: bar glyph (4 chars) + 4 trailing spaces = 8 chars pitch,
    // matching the 8-char label slots below so bars line up with labels
    private static final int BAR_PITCH = 8;
    private static final int SCALE_WIDTH = 3;

    private ReportGraph() {
    }

    // -------------------- summary & callouts (plain) --------------------

    public static void printSummary(String[] summaryLines) {
        if (summaryLines == null) {
            return;
        }
        for (String line : summaryLines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            System.out.println(" " + Ansi.strip(line));
        }
    }

    public static void printCallouts(List<String> callouts) {
        if (callouts == null || callouts.isEmpty()) {
            return;
        }
        for (String line : callouts) {
            System.out.println(" " + Ansi.strip(line));
        }
    }

    // -------------------- side-by-side charts --------------------

    public static void printCharts(List<ReportChart> charts) {
        if (charts == null || charts.isEmpty()) {
            return;
        }

        // pair charts: up to 2 per row
        for (int row = 0; row < charts.size(); row += 2) {
            ReportChart left = charts.get(row);
            ReportChart right = (row + 1 < charts.size()) ? charts.get(row + 1) : null;

            renderChartPair(left, right);
            System.out.println();
        }
    }

    private static void renderChartPair(ReportChart left, ReportChart right) {
        // compute block widths and Y-axis scale
        int leftBars = left.getBars().size();
        int rightBars = right != null ? right.getBars().size() : 0;

        int leftContentWidth = leftBars * BAR_PITCH;
        int rightContentWidth = rightBars * BAR_PITCH;

        // scale top = max value across both charts, capped at 10 for readability
        double globalMax = 0;
        for (ReportChart.Bar b : left.getBars()) {
            globalMax = Math.max(globalMax, b.getValue());
        }
        if (right != null) {
            for (ReportChart.Bar b : right.getBars()) {
                globalMax = Math.max(globalMax, b.getValue());
            }
        }
        int scaleY = computeScaleY(globalMax);
        int scaleHeight = scaleY; // number of Y rows

        // block width = max(title length, scale_width + content width)
        int leftBlock = Math.max(Ansi.strip(left.getTitle()).length(),
                SCALE_WIDTH + leftContentWidth);
        int rightBlock = (right != null)
                ? Math.max(Ansi.strip(right.getTitle()).length(),
                        SCALE_WIDTH + rightContentWidth)
                : 0;

        // title row
        StringBuilder titleRow = new StringBuilder();
        titleRow.append(rightPad(Ansi.strip(left.getTitle()), leftBlock));
        if (right != null) {
            titleRow.append(" | ");
            titleRow.append(rightPad(Ansi.strip(right.getTitle()), rightBlock));
        }
        System.out.println(titleRow);

        // alignment caret row (marks y-axis column of each block)
        StringBuilder caretRow = new StringBuilder();
        caretRow.append(repeat(' ', SCALE_WIDTH - 1));
        caretRow.append('^');
        for (int i = SCALE_WIDTH; i < leftBlock; i++) {
            caretRow.append(' ');
        }
        if (right != null) {
            caretRow.append(" | ");
            caretRow.append(repeat(' ', SCALE_WIDTH - 1));
            caretRow.append('^');
            for (int i = SCALE_WIDTH; i < rightBlock; i++) {
                caretRow.append(' ');
            }
        }
        System.out.println(caretRow);

        // compute bar heights for each chart
        int[] leftHeights = computeHeights(left.getBars(), scaleY);
        int[] rightHeights = (right != null) ? computeHeights(right.getBars(), scaleY) : new int[0];

        // scale rows (top to bottom: scaleY, scaleY-1, ..., 1)
        for (int y = scaleY; y >= 1; y--) {
            StringBuilder row = new StringBuilder();

            // left block
            row.append(rightPad(String.valueOf(y), SCALE_WIDTH - 1));
            row.append(" |");
            row.append(buildBarRow(leftHeights, y, leftBars));

            // right block
            if (right != null) {
                row.append(" | ");
                row.append(rightPad(String.valueOf(y), SCALE_WIDTH - 1));
                row.append(" |");
                row.append(buildBarRow(rightHeights, y, rightBars));
            }

            System.out.println(row);
        }

        // axis row (one dash wider than the block so it lines up with the
        // scale rows, which carry the y value + " |" separator)
        StringBuilder axisRow = new StringBuilder();
        axisRow.append(" -+");
        axisRow.append(repeat('-', leftBlock - SCALE_WIDTH + 1));
        if (right != null) {
            axisRow.append("-+ ");
            axisRow.append(repeat(' ', SCALE_WIDTH - 1));
            axisRow.append("+");
            axisRow.append(repeat('-', rightBlock - SCALE_WIDTH + 1));
            axisRow.append("-+");
        }
        System.out.println(axisRow);

        // label rows (up to 2 rows for long names)
        int maxBars = Math.max(leftBars, rightBars);
        String[][] leftLabels = wrapLabels(left.getBars());
        String[][] rightLabels = (right != null) ? wrapLabels(right.getBars()) : new String[0][];

        int labelRows = Math.max(
                leftLabels.length > 0 ? leftLabels[0].length : 0,
                rightLabels.length > 0 ? rightLabels[0].length : 0);

        for (int lr = 0; lr < labelRows; lr++) {
            StringBuilder labelRow = new StringBuilder();

            // left labels
            labelRow.append(repeat(' ', SCALE_WIDTH + 1));
            for (int i = 0; i < leftBars; i++) {
                String lbl = (lr < leftLabels[i].length) ? leftLabels[i][lr] : "";
                labelRow.append(padCenter(lbl, BAR_PITCH));
            }

            // right labels
            if (right != null) {
                labelRow.append(" | ");
                labelRow.append(repeat(' ', SCALE_WIDTH + 1));
                for (int i = 0; i < rightBars; i++) {
                    String lbl = (lr < rightLabels[i].length) ? rightLabels[i][lr] : "";
                    labelRow.append(padCenter(lbl, BAR_PITCH));
                }
            }

            System.out.println(labelRow);
        }
    }

    // -------------------- helpers --------------------

    private static int computeScaleY(double maxValue) {
        if (maxValue <= 0) {
            return 1;
        }
        if (maxValue <= 10) {
            // minimum 2 rows so small values (e.g. 0.5) stay visible and are
            // not misread as a full-height bar on a 1-row scale
            return Math.max(2, (int) Math.ceil(maxValue));
        }
        return 10;
    }

    private static int[] computeHeights(List<ReportChart.Bar> bars, int scaleY) {
        int n = bars.size();
        int[] heights = new int[n];
        double maxVal = 0;
        for (ReportChart.Bar b : bars) {
            maxVal = Math.max(maxVal, b.getValue());
        }
        if (maxVal == 0) {
            return heights;
        }
        for (int i = 0; i < n; i++) {
            double v = bars.get(i).getValue();
            if (maxVal <= scaleY) {
                // 1:1 scale: round up so no non-zero value disappears
                heights[i] = v <= 0 ? 0 : (int) Math.ceil(v);
            } else {
                heights[i] = (int) Math.round(v / maxVal * scaleY);
            }
            heights[i] = Math.min(heights[i], scaleY);
        }
        return heights;
    }

    private static String buildBarRow(int[] heights, int y, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (heights[i] >= y) {
                sb.append(" ██");
            } else {
                sb.append("    ");
            }
            sb.append("    "); // pitch = 8 (bar 4 + gap 4)
        }
        return sb.toString();
    }

    // wrap labels to max 2 rows of up to 8 chars each; long enum names
    // (STANDARD_SINGLE, MAINTENANCE) are split on '_' where possible so
    // their identity survives the two-row budget
    private static String[][] wrapLabels(List<ReportChart.Bar> bars) {
        String[][] result = new String[bars.size()][];
        for (int i = 0; i < bars.size(); i++) {
            result[i] = wrapLabel(bars.get(i).getLabel());
        }
        return result;
    }

    private static String[] wrapLabel(String label) {
        if (label == null || label.isEmpty()) {
            return new String[] { "" };
        }
        String plain = Ansi.strip(label);
        if (plain.length() <= BAR_PITCH) {
            return new String[] { plain };
        }
        // prefer a split on '_' or ' ' inside the first row
        int breakAt = -1;
        for (int i = BAR_PITCH; i > 0; i--) {
            char c = plain.charAt(i);
            if (c == '_' || c == ' ') {
                breakAt = i;
                break;
            }
        }
        if (breakAt > 0 && plain.length() - breakAt - 1 <= BAR_PITCH) {
            return new String[] { plain.substring(0, breakAt), plain.substring(breakAt + 1) };
        }
        // hard split into 2 rows of up to BAR_PITCH chars
        String row1 = plain.substring(0, BAR_PITCH);
        String remainder = plain.substring(BAR_PITCH);
        String row2 = remainder.length() <= BAR_PITCH ? remainder : remainder.substring(0, BAR_PITCH);
        return new String[] { row1, row2 };
    }

    private static String rightPad(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        for (int i = text.length(); i < width; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String padCenter(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
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

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
