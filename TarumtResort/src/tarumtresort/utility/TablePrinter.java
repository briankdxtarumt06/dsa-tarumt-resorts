package tarumtresort.utility;

/**
 * Console table renderer (converted from the C++ OutputManager::displayTable).
 * Draws a box-drawing table:
 * <pre>
 * ╔═════════╦══════════╗
 * ║  Head 1 ║  Head 2  ║
 * ╠═════════╬══════════╣
 * ║  cell   ║  cell    ║
 * ╚═════════╩══════════╝
 * </pre>
 */
public class TablePrinter {

    private static final char HORIZONTAL = '═';
    private static final char VERTICAL = '║';
    private static final char CORNER_TL = '╔';
    private static final char CORNER_TR = '╗';
    private static final char CORNER_BL = '╚';
    private static final char CORNER_BR = '╝';
    private static final char T_DOWN = '╦';
    private static final char T_UP = '╩';
    private static final char T_RIGHT = '╠';
    private static final char T_LEFT = '╣';
    private static final char CROSS = '╬';

    private TablePrinter() { // static-only utility
    }

    /**
     * Prints a box-drawing table with a header row and data rows.
     * Columns are sized to the longest value (plus padding) and left-aligned.
     *
     * @param header the column headers
     * @param rows   the data rows; each row must have the same length as header
     */
    public static void displayTable(String[] header, String[][] rows) {
        int numColumns = header.length;
        int[] columnWidths = new int[numColumns];

        for (int i = 0; i < numColumns; i++) {
            columnWidths[i] = header[i].length();
        }
        for (String[] row : rows) {
            if (row.length != numColumns) {
                System.err.println("Warning: Row size mismatch. Skipping row.");
                continue;
            }
            for (int i = 0; i < numColumns; i++) {
                columnWidths[i] = Math.max(columnWidths[i], row[i].length());
            }
        }
        for (int i = 0; i < numColumns; i++) {
            columnWidths[i] += 2; // padding on each side
        }

        drawHorizontalLine(columnWidths, CORNER_TL, T_DOWN, CORNER_TR);
        printRow(header, columnWidths);

        if (rows.length == 0) {
            drawHorizontalLine(columnWidths, CORNER_BL, T_UP, CORNER_BR);
            System.out.println();
            return;
        }

        drawHorizontalLine(columnWidths, T_RIGHT, CROSS, T_LEFT);
        for (int i = 0; i < rows.length; i++) {
            printRow(rows[i], columnWidths);
            if (i < rows.length - 1) {
                drawHorizontalLine(columnWidths, T_RIGHT, CROSS, T_LEFT);
            }
        }
        drawHorizontalLine(columnWidths, CORNER_BL, T_UP, CORNER_BR);
        System.out.println();
    }

    private static void drawHorizontalLine(int[] widths, char left, char mid, char right) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat(HORIZONTAL, widths[i]));
            if (i < widths.length - 1) {
                sb.append(mid);
            }
        }
        sb.append(right);
        System.out.println(sb);
    }

    private static void printRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(VERTICAL);
        for (int i = 0; i < cells.length; i++) {
            String cell = cells[i] == null ? "" : cells[i];
            sb.append(' ').append(cell);
            sb.append(repeat(' ', widths[i] - 1 - cell.length()));
            sb.append(VERTICAL);
        }
        System.out.println(sb);
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
