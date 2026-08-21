package tarumtresort.report.LoyaltyReport;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Scanner;
import tarumtresort.entity.enums.Tier;
import tarumtresort.report.ReportResult;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.ReportGraph;
import tarumtresort.utility.TablePrinter;

// Author: Imam Mahdi Ali Ang Attuko
public class ReportUI {

    private static final String UNIVERSITY =
            "TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY";
    private static final String DEFAULT_SUBSYSTEM = "HOUSEKEEPING MODULE SUBSYSTEM";
    private final String subsystem;
    private final String confidential;

    public ReportUI(Scanner scanner) {
        this(scanner, DEFAULT_SUBSYSTEM);
    }

    public ReportUI(Scanner scanner, String subsystem) {
        this.scanner = scanner;
        this.subsystem = subsystem == null ? DEFAULT_SUBSYSTEM : subsystem;
        this.confidential = UNIVERSITY + " HIGHLY CONFIDENTIAL DOCUMENT";
    }

    private static final String[] MONTHS = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Scanner scanner;

    // -------------------- REPORT SUB-MENU --------------------

    public int getReportMenuChoice() {
        System.out.println("\n========================================");
        System.out.println("  REPORTS");
        System.out.println("========================================");
        System.out.println("  1. Room Turnover & Readiness Report");
        System.out.println("  2. Staff Productivity & Reassignment Report");
        System.out.println("  0. Back");
        System.out.println("========================================");
        return getIntInput("Enter choice", 0, 2);
    }

    // -------------------- DATE-RANGE INPUT --------------------

    public LocalDateTime[] inputOptionalDateTimeRange(String fieldLabel) {
        System.out.println("\n========================================");
        System.out.println("  " + fieldLabel.toUpperCase() + " â€” DATE RANGE");
        System.out.println("========================================");
        System.out.println("  1. This Month");
        System.out.println("  2. Last Month");
        System.out.println("  3. Specific Month (pick month & year)");
        System.out.println("  4. This Week (Mon â€” Sun)");
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
            case 7: return new LocalDateTime[]{ null, null };
            default: return rangeCustom(fieldLabel);
        }
    }

    public LocalDateTime inputOptionalDateTime(String prompt) {
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

    // -------------------- SECONDARY FILTERS --------------------

    /** Asks for a tier filter; returns null when "All tiers" is chosen. */
    public Tier inputTierFilter() {
        System.out.println("\n========================================");
        System.out.println("  TIER FILTER");
        System.out.println("========================================");
        Tier[] tiers = Tier.values();
        for (int i = 0; i < tiers.length; i++) {
            System.out.println("  " + (i + 1) + ". " + tiers[i].name());
        }
        System.out.println("  " + (tiers.length + 1) + ". All tiers");
        System.out.println("========================================");
        int choice = getIntInput("Enter tier", 1, tiers.length + 1);
        if (choice == tiers.length + 1) {
            return null;
        }
        return tiers[choice - 1];
    }

    public String inputStatusFilter() {
        System.out.println("\n========================================");
        System.out.println("  REDEMPTION STATUS FILTER");
        System.out.println("========================================");
        System.out.println("  1. PENDING");
        System.out.println("  2. APPROVED");
        System.out.println("  3. REJECTED");
        System.out.println("  4. All statuses");
        System.out.println("========================================");
        int choice = getIntInput("Enter status", 1, 4);
        switch (choice) {
            case 1: return "PENDING";
            case 2: return "APPROVED";
            case 3: return "REJECTED";
            default: return null;
        }
    }

    public boolean inputYesNo(String prompt) {
        System.out.print(prompt + " (y/n): ");
        String line = scanner.nextLine().trim();
        return !line.isEmpty() && Character.toLowerCase(line.charAt(0)) == 'y';
    }

    private LocalDateTime[] rangeThisMonth() {
        YearMonth ym = YearMonth.now();
        return new LocalDateTime[]{
            ym.atDay(1).atStartOfDay(),
            ym.atEndOfMonth().atTime(23, 59, 59, 999_999_999)
        };
    }

    private LocalDateTime[] rangeLastMonth() {
        YearMonth ym = YearMonth.now().minusMonths(1);
        return new LocalDateTime[]{
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
        return new LocalDateTime[]{
            ym.atDay(1).atStartOfDay(),
            ym.atEndOfMonth().atTime(23, 59, 59, 999_999_999)
        };
    }

    private LocalDateTime[] rangeThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new LocalDateTime[]{
            monday.atStartOfDay(),
            sunday.atTime(23, 59, 59, 999_999_999)
        };
    }

    private LocalDateTime[] rangeLast7Days() {
        return new LocalDateTime[]{
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now()
        };
    }

    private LocalDateTime[] rangeToday() {
        return new LocalDateTime[]{
            LocalDate.now().atStartOfDay(),
            LocalDateTime.now()
        };
    }

    private LocalDateTime[] rangeCustom(String fieldLabel) {
        LocalDateTime from = inputOptionalDateTime("Enter " + fieldLabel + " FROM (yyyy-MM-dd HH:mm)");
        LocalDateTime to = inputOptionalDateTime("Enter " + fieldLabel + " TO (yyyy-MM-dd HH:mm)");
        return new LocalDateTime[]{ from, to };
    }

    // -------------------- FORMAL REPORT PRINTING --------------------

    public void printReport(ReportResult result, String reportTitle) {
        if (result.isEmpty()) {
            ConsoleUtil.printError("No records match the given filters.");
            return;
        }

        String[][] table = result.getTable();
        String[] header = table[0];
        String[][] rows = new String[table.length - 1][];
        System.arraycopy(table, 1, rows, 0, rows.length);

        // top border
        TablePrinter.printFullWidthLine('=');

        // title block
        TablePrinter.printCentered(UNIVERSITY);
        TablePrinter.printCentered(subsystem);
        System.out.println();
        TablePrinter.printCentered("SUMMARY OF " + reportTitle);
        TablePrinter.printFullWidthLine('-');

        // generated-at
        System.out.println("Generated at: " + LocalDateTime.now().format(TIMESTAMP_FMT));
        TablePrinter.printFullWidthLine('*');
        System.out.println();

        // applied filter criteria (multi-criteria demonstration)
        if (result.getCriteria() != null && !result.getCriteria().isEmpty()) {
            TablePrinter.printCentered("Criteria: " + result.getCriteria());
            TablePrinter.printFullWidthLine('-');
        }

        // confidential
        System.out.println(confidential);
        TablePrinter.printFullWidthLine('-');

        // data table
        TablePrinter.displayDelimitedTable(header, rows);
        TablePrinter.printFullWidthLine('-');

        // summary totals
        ReportGraph.printSummary(result.getSummary());
        System.out.println();

        // graphical representation section
        TablePrinter.printCentered("GRAPHICAL REPRESENTATION OF " + reportTitle);
        System.out.println();
        ReportGraph.printCharts(result.getCharts());
        TablePrinter.printFullWidthLine('-');

        // callouts
        ReportGraph.printCallouts(result.getCallouts());
        TablePrinter.printFullWidthLine('*');

        // footer
        TablePrinter.printCentered("END OF THE REPORT");
        TablePrinter.printFullWidthLine('=');
    }

    public void pressEnterToContinue() {
        ConsoleUtil.pressEnterToContinue(scanner);
    }

    // -------------------- INPUT HELPER --------------------

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
                // fall through to range error below
            }
            ConsoleUtil.printError("Please enter a number between " + min + " and " + max + "!");
        }
    }
}
