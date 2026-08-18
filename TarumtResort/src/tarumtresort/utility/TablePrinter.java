package tarumtresort.utility;

public class TablePrinter {

    public static final int DOC_WIDTH = 132;

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

    private TablePrinter() {
    }

    public static void displayTable(String[] header, String[][] rows) {
        int numColumns = header.length;
        int[] columnWidths = new int[numColumns];

        for (int i = 0; i < numColumns; i++) {
            columnWidths[i] = Ansi.strip(header[i]).length();
        }
        for (String[] row : rows) {
            if (row.length != numColumns) {
                ConsoleUtil.printWarning("Row size mismatch. Skipping row.");
                continue;
            }
            for (int i = 0; i < numColumns; i++) {
                columnWidths[i] = Math.max(columnWidths[i], Ansi.strip(row[i]).length());
            }
        }
        for (int i = 0; i < numColumns; i++) {
            columnWidths[i] += 2; 
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
            String plain = Ansi.strip(cell);
            sb.append(' ').append(cell);
            sb.append(repeat(' ', widths[i] - 1 - plain.length()));
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

    // -------------------- plain | -delimited style (for formal reports) --------------------

    public static void displayDelimitedTable(String[] header, String[][] rows) {
        int numColumns = header.length;
        int[] columnWidths = new int[numColumns];

        for (int i = 0; i < numColumns; i++) {
            columnWidths[i] = Ansi.strip(header[i]).length();
        }
        for (String[] row : rows) {
            if (row.length != numColumns) {
                continue;
            }
            for (int i = 0; i < numColumns; i++) {
                columnWidths[i] = Math.max(columnWidths[i], Ansi.strip(row[i]).length());
            }
        }
        for (int i = 0; i < numColumns; i++) {
            columnWidths[i] += 2;
        }

        printDelimitedRow(header, columnWidths, true);
        printFullWidthLine('-');
        for (String[] row : rows) {
            printDelimitedRow(row, columnWidths, false);
        }
    }

    private static void printDelimitedRow(String[] cells, int[] widths, boolean isHeader) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            String cell = Ansi.strip(cells[i]);
            if (i > 0) {
                sb.append(" | ");
            }
            int pad;
            if (isHeader) {
                pad = widths[i] - cell.length();
                sb.append(centreAlign(cell, widths[i]));
            } else if (isNumeric(cell)) {
                pad = widths[i] - cell.length();
                sb.append(centreAlign(cell, widths[i]));
            } else {
                sb.append(padRight(cell, widths[i]));
            }
        }
        System.out.println(sb);
    }

    private static boolean isNumeric(String s) {
        return s != null && !s.isEmpty() && s.matches("-?\\d+(\\.\\d+)?[%]?");
    }

    private static String centreAlign(String text, int width) {
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

    private static String padRight(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        for (int i = text.length(); i < width; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static void printFullWidthLine(char c) {
        StringBuilder sb = new StringBuilder(DOC_WIDTH);
        for (int i = 0; i < DOC_WIDTH; i++) {
            sb.append(c);
        }
        System.out.println(sb);
    }

    public static void printCentered(String text) {
        String plain = Ansi.strip(text);
        if (plain.length() >= DOC_WIDTH) {
            System.out.println(text);
            return;
        }
        int pad = (DOC_WIDTH - plain.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) {
            sb.append(' ');
        }
        sb.append(text);
        System.out.println(sb);
    }
}
