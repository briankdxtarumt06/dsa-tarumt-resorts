package tarumtresort.report.PriorityReservationReport;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Scanner;
import tarumtresort.adt.ListInterface;
import tarumtresort.entity.enums.PriorityLevel;
import tarumtresort.entity.enums.ReservationStatus;
import tarumtresort.entity.enums.RoomType;
import tarumtresort.report.ReportChart;
import tarumtresort.utility.Ansi;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

// Author: Lee Boon Yew
public class PriorityReservationReportUI {

    private static final int DOC_WIDTH = TablePrinter.DOC_WIDTH;

    private static final String UNIVERSITY =
            "TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY";
    private static final String SUBSYSTEM = "PRIORITY RESERVATION MANAGEMENT";
    private static final String CONFIDENTIAL =
            UNIVERSITY + " HIGHLY CONFIDENTIAL DOCUMENT";

    private static final String[] MONTHS = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int[] NICE_STEPS = {
        1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000
    };

    private final Scanner scanner;

    public PriorityReservationReportUI(Scanner scanner) {
        this.scanner = scanner;
    }

    // -------------------- filter prompts --------------------

    public LocalDateTime[] inputOptionalDateTimeRange(String fieldLabel) {
        System.out.println("\n========================================");
        System.out.println("  " + fieldLabel.toUpperCase() + " - DATE RANGE");
        System.out.println("========================================");
        System.out.println("  1. This Month");
        System.out.println("  2. Last Month");
        System.out.println("  3. Specific Month (pick month & year)");
        System.out.println("  4. This Week (Mon - Sun)");
        System.out.println("  5. Last 7 Days");
        System.out.println("  6. Today");
        System.out.println("  7. All Time (no limit)");
        System.out.println("  8. Custom Range (type manually)");
        System.out.println("========================================");
        int choice = getIntInput("Enter option", 1, 8);

        switch (choice) {
            case 1: return rangeThisMonth();
            case 2: return rangeLastMonth();
            case 3: return rangeSpecificMonth();
            case 4: return rangeThisWeek();
            case 5: return rangeLast7Days();
            case 6: return rangeToday();
            case 7: return new LocalDateTime[] { null, null };
            default: return rangeCustom(fieldLabel);
        }
    }

    /** Reservation status filter. Returns null for "all statuses". */
    public ReservationStatus selectStatusFilter() {
        ReservationStatus[] values = ReservationStatus.values();
        System.out.println("\n========================================");
        System.out.println("  FILTER BY RESERVATION STATUS");
        System.out.println("========================================");
        System.out.println("  0. All Statuses (no filter)");
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i].name());
        }
        System.out.println("========================================");
        int choice = getIntInput("Enter option", 0, values.length);
        return choice == 0 ? null : values[choice - 1];
    }

    /** Room type filter. Returns null for "all room types". */
    public RoomType selectRoomTypeFilter() {
        RoomType[] values = RoomType.values();
        System.out.println("\n========================================");
        System.out.println("  FILTER BY ROOM TYPE REQUESTED");
        System.out.println("========================================");
        System.out.println("  0. All Room Types (no filter)");
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i].name());
        }
        System.out.println("========================================");
        int choice = getIntInput("Enter option", 0, values.length);
        return choice == 0 ? null : values[choice - 1];
    }

    public PriorityLevel selectMinimumPriorityLevel() {
        PriorityLevel[] values = PriorityLevel.values();
        System.out.println("\n========================================");
        System.out.println("  MINIMUM PRIORITY LEVEL");
        System.out.println("========================================");
        System.out.println("  0. All Levels (no threshold)");
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i].name()
                    + " and above (rank " + values[i].getRank() + "+)");
        }
        System.out.println("========================================");
        int choice = getIntInput("Enter option", 0, values.length);
        return choice == 0 ? null : values[choice - 1];
    }

    /** Override scope: 0 = all records, 1 = overridden only, 2 = non-overridden only. */
    public int selectOverrideScope() {
        System.out.println("\n========================================");
        System.out.println("  OVERRIDE SCOPE");
        System.out.println("========================================");
        System.out.println("  0. All Records");
        System.out.println("  1. Staff-Overridden Records Only");
        System.out.println("  2. Loyalty-Tier Records Only (not overridden)");
        System.out.println("========================================");
        return getIntInput("Enter option", 0, 2);
    }

    private LocalDateTime inputOptionalDateTime(String prompt) {
        System.out.print(prompt + ": ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException e) {
            ConsoleUtil.printError("Invalid format! Please use yyyy-MM-dd HH:mm (blank = no limit).");
            return inputOptionalDateTime(prompt);
        }
    }

    private LocalDateTime[] rangeThisMonth() {
        YearMonth ym = YearMonth.now();
        return new LocalDateTime[] {
            ym.atDay(1).atStartOfDay(),
            ym.atEndOfMonth().atTime(23, 59, 59, 999_999_999)
        };
    }

    private LocalDateTime[] rangeLastMonth() {
        YearMonth ym = YearMonth.now().minusMonths(1);
        return new LocalDateTime[] {
            ym.atDay(1).atStartOfDay(),
            ym.atEndOfMonth().atTime(23, 59, 59, 999_999_999)
        };
    }

    private LocalDateTime[] rangeSpecificMonth() {
        System.out.println("\nSelect Month:");
        for (int i = 0; i < MONTHS.length; i++) {
            System.out.println("  " + (i + 1) + ". " + MONTHS[i]);
        }
        int month = getIntInput("Enter month", 1, 12);

        int currentYear = YearMonth.now().getYear();
        System.out.print("Enter year (" + currentYear + " = default): ");
        String yearInput = scanner.nextLine().trim();
        int year;
        if (yearInput.isEmpty()) {
            year = currentYear;
        } else {
            try {
                year = Integer.parseInt(yearInput);
                if (year < 2000 || year > 2100) {
                    ConsoleUtil.printWarning("Year out of range, using " + currentYear + ".");
                    year = currentYear;
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.printWarning("Invalid year, using " + currentYear + ".");
                year = currentYear;
            }
        }

        YearMonth ym = YearMonth.of(year, month);
        return new LocalDateTime[] {
            ym.atDay(1).atStartOfDay(),
            ym.atEndOfMonth().atTime(23, 59, 59, 999_999_999)
        };
    }

    private LocalDateTime[] rangeThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new LocalDateTime[] {
            monday.atStartOfDay(),
            sunday.atTime(23, 59, 59, 999_999_999)
        };
    }

    private LocalDateTime[] rangeLast7Days() {
        return new LocalDateTime[] {
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now()
        };
    }

    private LocalDateTime[] rangeToday() {
        return new LocalDateTime[] {
            LocalDate.now().atStartOfDay(),
            LocalDateTime.now()
        };
    }

    private LocalDateTime[] rangeCustom(String fieldLabel) {
        LocalDateTime from = inputOptionalDateTime("Enter " + fieldLabel + " FROM (yyyy-MM-dd HH:mm)");
        LocalDateTime to = inputOptionalDateTime("Enter " + fieldLabel + " TO (yyyy-MM-dd HH:mm)");
        return new LocalDateTime[] { from, to };
    }

    private int getIntInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    System.out.println();
                    return value;
                }
            } catch (NumberFormatException e) {
            }
            ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
        }
    }

    public void pressEnterToContinue() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }

    // -------------------- formal document sections --------------------

    public void printDocumentHeader(String reportTitle) {
        TablePrinter.printFullWidthLine('=');
        TablePrinter.printCentered(UNIVERSITY);
        TablePrinter.printCentered(SUBSYSTEM);
        System.out.println();
        TablePrinter.printCentered("SUMMARY OF " + reportTitle);
        TablePrinter.printFullWidthLine('=');
        System.out.println();
        System.out.println("Generated at: " + LocalDateTime.now().format(TIMESTAMP_FMT));
        System.out.println();
        TablePrinter.printFullWidthLine('-');
        System.out.println();
        TablePrinter.printCentered(CONFIDENTIAL);
        System.out.println();
    }

    public void printFilterSection(String[] lines) {
        if (lines == null || lines.length == 0) {
            return;
        }
        TablePrinter.printFullWidthLine('-');
        System.out.println(" FILTERS APPLIED");
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            System.out.println("   " + Ansi.strip(line));
        }
        System.out.println();
    }

    public void printTableSection(String[][] table) {
        if (table == null || table.length == 0) {
            return;
        }
        String[] header = table[0];
        String[][] rows = new String[table.length - 1][];
        System.arraycopy(table, 1, rows, 0, rows.length);
        TablePrinter.printFullWidthLine('-');
        TablePrinter.displayDelimitedTable(header, rows);
        TablePrinter.printFullWidthLine('-');
        System.out.println();
        System.out.println();
        TablePrinter.printFullWidthLine('=');
    }

    public void printChartSection(String reportTitle, ListInterface<ReportChart> charts,
            String[] axisLabels) {
        System.out.println();
        TablePrinter.printCentered("GRAPHICAL REPRESENTATION OF " + reportTitle);
        System.out.println();

        if (charts != null) {
            for (int i = 0; i < charts.size(); i += 2) {
                ReportChart left = charts.get(i);
                ReportChart right = (i + 1 < charts.size()) ? charts.get(i + 1) : null;
                printChartPair(left, axisLabel(axisLabels, i), right, axisLabel(axisLabels, i + 1));
                System.out.println();
            }
        }
        TablePrinter.printFullWidthLine('=');
        System.out.println();
    }

    private String axisLabel(String[] labels, int index) {
        return (labels == null || index >= labels.length || labels[index] == null) ? "" : labels[index];
    }

    public void printSummarySection(String[] lines) {
        TablePrinter.printCentered("KEY PERFORMANCE METRICS");
        System.out.println();
        if (lines != null) {
            for (String line : lines) {
                if (line == null || line.isEmpty()) {
                    continue;
                }
                printFullWidth(" " + Ansi.strip(line));
            }
        }
    }

    public void printDocumentFooter() {
        System.out.println();
        TablePrinter.printFullWidthLine('=');
        TablePrinter.printCentered("END OF THE REPORT");
        TablePrinter.printFullWidthLine('=');
    }

    public void printNoData(String reportTitle) {
        TablePrinter.printFullWidthLine('=');
        TablePrinter.printCentered(UNIVERSITY);
        TablePrinter.printCentered(SUBSYSTEM);
        System.out.println();
        TablePrinter.printCentered(reportTitle);
        TablePrinter.printFullWidthLine('=');
        System.out.println();
        ConsoleUtil.printError("No priority reservation records match the selected filters.");
        System.out.println(" Try widening the date range, or choose \"All\" for status and room type.");
        System.out.println();
        TablePrinter.printFullWidthLine('=');
    }

    // -------------------- side-by-side ASCII bar charts --------------------

    private static final String CHART_GAP = " | ";
    private static final int BLOCK_WIDTH = (DOC_WIDTH - CHART_GAP.length()) / 2;

    private void printChartPair(ReportChart left, String leftAxis,
            ReportChart right, String rightAxis) {
        if (left == null || left.isEmpty()) {
            return;
        }
        ChartBlock leftBlock = buildChartBlock(left, leftAxis);
        ChartBlock rightBlock = (right == null || right.isEmpty())
                ? null : buildChartBlock(right, rightAxis);

        int tallest = rightBlock == null ? leftBlock.scaleRows
                : Math.max(leftBlock.scaleRows, rightBlock.scaleRows);
        String[] leftLines = leftBlock.alignedTo(tallest);
        String[] rightLines = rightBlock == null ? null : rightBlock.alignedTo(tallest);

        int lines = rightLines == null ? leftLines.length
                : Math.max(leftLines.length, rightLines.length);

        for (int i = 0; i < lines; i++) {
            String leftLine = i < leftLines.length ? leftLines[i] : "";
            if (rightLines == null) {
                System.out.println(padRight(leftLine, BLOCK_WIDTH));
                continue;
            }
            String rightLine = i < rightLines.length ? rightLines[i] : "";
            System.out.println(padRight(padRight(leftLine, BLOCK_WIDTH)
                    + CHART_GAP + rightLine, DOC_WIDTH));
        }
    }

    /** A rendered chart plus the number of scale rows, used to align baselines. */
    private final class ChartBlock {
        private final String[] lines;
        private final int scaleRows;

        ChartBlock(String[] lines, int scaleRows) {
            this.lines = lines;
            this.scaleRows = scaleRows;
        }

        /** Inserts blank rows between the title and the caret to match a taller chart. */
        String[] alignedTo(int targetRows) {
            int shortfall = targetRows - scaleRows;
            if (shortfall <= 0) {
                return lines;
            }
            String[] padded = new String[lines.length + shortfall];
            padded[0] = lines[0];
            for (int i = 0; i < shortfall; i++) {
                padded[1 + i] = repeat(' ', BLOCK_WIDTH);
            }
            System.arraycopy(lines, 1, padded, 1 + shortfall, lines.length - 1);
            return padded;
        }
    }

    /** One chart rendered into fixed-width lines: title, scale, bars, axis, labels. */
    private ChartBlock buildChartBlock(ReportChart chart, String axisCaption) {
        ListInterface<ReportChart.Bar> bars = chart.getBars();
        int barCount = bars.size();

        double peak = 0;
        for (int i = 0; i < barCount; i++) {
            peak = Math.max(peak, bars.get(i).getValue());
        }
        int step = niceStep(peak);
        int top = niceTop(peak, step);
        int rows = top / step;
        int[] heights = barHeights(bars, top, rows);

        int scaleWidth = String.valueOf(top).length() + 1;
        int pitch = Math.max(4,
                Math.min(12, (BLOCK_WIDTH - scaleWidth - 2) / Math.max(1, barCount)));

        String[][] labels = labelLines(bars, barCount);
        int labelRows = labels.length == 0 ? 0 : labels[0].length;

        // title + caret + scale rows + axis + label rows
        String[] block = new String[1 + 1 + rows + 1 + labelRows];
        int line = 0;

        block[line++] = truncate(chart.getTitle(), BLOCK_WIDTH);
        block[line++] = repeat(' ', scaleWidth - 1) + "^";

        for (int rowY = rows; rowY >= 1; rowY--) {
            block[line++] = scaleLabel(rowY * step, scaleWidth)
                    + barRow(heights, rowY, barCount, pitch);
        }

        // keep the dash run short enough that the arrow and its caption survive
        String caption = (axisCaption == null || axisCaption.isEmpty()) ? "" : " " + axisCaption;
        int dashBudget = BLOCK_WIDTH - scaleWidth - 1 - caption.length();
        int dashes = Math.max(1, Math.min(barCount * pitch, dashBudget));
        block[line++] = repeat(' ', scaleWidth - 1) + "+" + repeat('-', dashes) + ">" + caption;

        for (int row = 0; row < labelRows; row++) {
            block[line++] = labelRow(labels, row, barCount, pitch);
        }

        for (int i = 0; i < block.length; i++) {
            block[i] = truncate(padRight(block[i], BLOCK_WIDTH), BLOCK_WIDTH);
        }
        return new ChartBlock(block, rows);
    }

    private String scaleLabel(int y, int scaleWidth) {
        return padLeft(String.valueOf(y), scaleWidth - 1) + "|";
    }

    private String barRow(int[] heights, int y, int barCount, int pitch) {
        int glyph = 4;
        int pad = Math.max(0, (pitch - glyph) / 2);
        int gap = Math.max(0, pitch - glyph - pad);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < barCount; i++) {
            if (heights[i] >= y) {
                sb.append(repeat(' ', pad)).append(" ██ ").append(repeat(' ', gap));
            } else {
                sb.append(repeat(' ', pitch));
            }
        }
        return sb.toString();
    }

    private String axisRow(int scaleWidth, int barCount, int pitch) {
        return repeat(' ', scaleWidth - 1) + "+" + repeat('-', barCount * pitch);
    }

    private String labelRow(String[][] labels, int row, int barCount, int pitch) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < barCount; i++) {
            String part = row < labels[i].length ? labels[i][row] : "";
            sb.append(padCenter(truncate(part, pitch), pitch));
        }
        return sb.toString();
    }

    private String truncate(String text, int width) {
        if (text == null) {
            return "";
        }
        return text.length() <= width ? text : text.substring(0, width);
    }

    private String[][] labelLines(ListInterface<ReportChart.Bar> bars, int barCount) {
        String[][] result = new String[barCount][];
        int maxRows = 0;
        for (int i = 0; i < barCount; i++) {
            result[i] = splitLabel(bars.get(i).getLabel());
            maxRows = Math.max(maxRows, result[i].length);
        }
        for (int i = 0; i < barCount; i++) {
            String[] parts = result[i];
            String[] padded = new String[maxRows];
            for (int r = 0; r < maxRows; r++) {
                padded[r] = r < parts.length ? parts[r] : "";
            }
            result[i] = padded;
        }
        return result;
    }

    private String[] splitLabel(String label) {
        String plain = Ansi.strip(label);
        if (plain == null || plain.isEmpty()) {
            return new String[] { "" };
        }
        if (plain.indexOf('\n') > 0) {
            String[] parts = plain.split("\n", -1);
            return parts.length >= 2 ? new String[] { parts[0], parts[1] } : new String[] { parts[0] };
        }
        if (plain.length() <= 12) {
            return new String[] { plain };
        }
        int breakAt = -1;
        for (int i = 12; i > 0; i--) {
            char c = plain.charAt(i);
            if (c == '_' || c == ' ') {
                breakAt = i;
                break;
            }
        }
        if (breakAt > 0) {
            return new String[] { plain.substring(0, breakAt), plain.substring(breakAt + 1) };
        }
        int half = plain.length() / 2;
        return new String[] { plain.substring(0, half), plain.substring(half) };
    }

    private int niceStep(double peak) {
        for (int step : NICE_STEPS) {
            if (peak <= step * 10) {
                return step;
            }
        }
        return NICE_STEPS[NICE_STEPS.length - 1];
    }

    private int niceTop(double peak, int step) {
        if (peak <= 0) {
            return step;
        }
        return (int) Math.ceil(peak / step) * step;
    }

    private int[] barHeights(ListInterface<ReportChart.Bar> bars, int top, int rows) {
        int n = bars.size();
        int[] heights = new int[n];
        double maxVal = 0;
        for (int i = 0; i < n; i++) {
            maxVal = Math.max(maxVal, bars.get(i).getValue());
        }
        if (maxVal == 0) {
            return heights;
        }
        for (int i = 0; i < n; i++) {
            double v = bars.get(i).getValue();
            if (maxVal <= rows) {
                heights[i] = v <= 0 ? 0 : (int) Math.ceil(v);
            } else {
                heights[i] = (int) Math.round(v / maxVal * rows);
            }
            heights[i] = Math.min(heights[i], rows);
        }
        return heights;
    }


    // -------------------- width helpers --------------------

    private void printFullWidth(String text) {
        System.out.println(padRight(text, DOC_WIDTH));
    }

    private String padLeft(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        return repeat(' ', width - text.length()) + text;
    }

    private String padRight(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        return text + repeat(' ', width - text.length());
    }

    private String padCenter(String text, int width) {
        String plain = Ansi.strip(text);
        if (plain.length() >= width) {
            return text;
        }
        int pad = width - plain.length();
        int left = pad / 2;
        return repeat(' ', left) + text + repeat(' ', pad - left);
    }

    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
