package tarumtresort.report.HousekeepingReport;

public class StaffProductivityUI {

    private static final String TITLE = "STAFF PRODUCTIVITY REPORT";

    private final HousekeepingReportUI ui;

    public StaffProductivityUI(HousekeepingReportUI ui) {
        this.ui = ui;
    }

    public void render(StaffProductivityReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}