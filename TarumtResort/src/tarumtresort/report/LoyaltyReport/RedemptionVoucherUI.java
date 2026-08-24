package tarumtresort.report.LoyaltyReport;

// Author: Imam Mahdi Ali Ang Attuko
public class RedemptionVoucherUI {

    private static final String TITLE = "REDEMPTION & VOUCHER REPORT";

    private final LoyaltyReportUI ui;

    public RedemptionVoucherUI(LoyaltyReportUI ui) {
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
