package tarumtresort.report.PriorityReservationReport;

// Author: Lee Boon Yew
public class PriorityLevelEffectivenessUI {

    private static final String TITLE = "PRIORITY LEVEL EFFECTIVENESS REPORT";

    private final PriorityReservationReportUI ui;

    public PriorityLevelEffectivenessUI(PriorityReservationReportUI ui) {
        this.ui = ui;
    }

    public void render(PriorityLevelEffectivenessReport.Result result, String[] filterLines) {
        if (result == null || result.isEmpty()) {
            ui.printNoData(TITLE);
            return;
        }
        ui.printDocumentHeader(TITLE);
        ui.printFilterSection(filterLines);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts(),
                new String[] { "Tier", "Tier" });
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}
