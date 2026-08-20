package tarumtresort.report.HousekeepingReport;

// Author: Brian Kam Ding Xian
public class RoomCleaningPerformanceUI {

    private static final String TITLE = "ROOM CLEANING PERFORMANCE REPORT";

    private final HousekeepingReportUI ui;

    public RoomCleaningPerformanceUI(HousekeepingReportUI ui) {
        this.ui = ui;
    }

    public void render(RoomCleaningPerformanceReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}