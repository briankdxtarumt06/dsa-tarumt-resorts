package tarumtresort.report.LoyaltyReport;

// Renders the Redemption & Voucher Report using the shared loyalty report UI,
// mirroring HousekeepingReport/RoomCleaningPerformanceUI.
public class RedemptionVoucherUI {

    private static final String TITLE = "REDEMPTION & VOUCHER REPORT";

    private final ReportUI ui;

    public RedemptionVoucherUI(ReportUI ui) {
        this.ui = ui;
    }

    public void render(RedemptionVoucherReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printCriteriaSection(result.getCriteria());
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printCalloutsSection(result.getCallouts());
        ui.printDocumentFooter();
    }
}
