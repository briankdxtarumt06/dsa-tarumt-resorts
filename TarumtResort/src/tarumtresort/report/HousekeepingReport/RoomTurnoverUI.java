package tarumtresort.report.HousekeepingReport;

public class RoomTurnoverUI {

    private static final String TITLE = "ROOM TURNOVER REPORT";

    private final HousekeepingReportUI ui;

    public RoomTurnoverUI(HousekeepingReportUI ui) {
        this.ui = ui;
    }

    public void render(RoomTurnoverReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}