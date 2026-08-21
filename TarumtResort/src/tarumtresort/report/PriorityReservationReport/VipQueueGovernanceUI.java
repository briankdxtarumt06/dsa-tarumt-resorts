package tarumtresort.report.PriorityReservationReport;

// Author: Lee Boon Yew
public class VipQueueGovernanceUI {

    private static final String TITLE = "VIP QUEUE AND OVERRIDE GOVERNANCE REPORT";

    private final PriorityReservationReportUI ui;

    public VipQueueGovernanceUI(PriorityReservationReportUI ui) {
        this.ui = ui;
    }

    public void render(VipQueueGovernanceReport.Result result, String[] filterLines) {
        if (result == null || result.isEmpty()) {
            ui.printNoData(TITLE);
            return;
        }
        ui.printDocumentHeader(TITLE);
        ui.printFilterSection(filterLines);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts(),
                new String[] { "Staff", "Staff" });
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}
