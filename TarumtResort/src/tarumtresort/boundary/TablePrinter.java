package tarumtresort.boundary;

/**
 *
 * @author Brian
 *
 * Shared table / detail printing helpers used by every UI class so that all
 * module outputs look the same. Pure formatting logic is centralized here.
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
                sb.append(String.format("%-" + (widths[col] + 3) + "s", cell));
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
}