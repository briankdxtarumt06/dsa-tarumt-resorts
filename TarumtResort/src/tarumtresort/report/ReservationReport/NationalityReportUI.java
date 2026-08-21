package tarumtresort.report.ReservationReport;

// Author: Chai Chee Tong

public class NationalityReportUI {

    private static final String TITLE = "NATIONALITY DEMAND REPORT";

    private final ReservationReportUI ui;

    public NationalityReportUI(ReservationReportUI ui) {
        this.ui = ui;
    }

    public void render(NationalityReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}
