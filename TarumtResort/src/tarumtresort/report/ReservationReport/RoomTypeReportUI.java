package tarumtresort.report.ReservationReport;

// Author: Chai Chee Tong

public class RoomTypeReportUI {

    private static final String TITLE = "ROOM TYPE DEMAND REPORT";

    private final ReservationReportUI ui;

    public RoomTypeReportUI(ReservationReportUI ui) {
        this.ui = ui;
    }

    public void render(RoomTypeReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}
