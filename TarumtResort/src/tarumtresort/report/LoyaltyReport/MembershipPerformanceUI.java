package tarumtresort.report.LoyaltyReport;

// Renders the Membership & Tier Performance Report using the shared loyalty report UI,
// mirroring HousekeepingReport/RoomCleaningPerformanceUI.
public class MembershipPerformanceUI {

    private static final String TITLE = "MEMBERSHIP & TIER PERFORMANCE REPORT";

    private final LoyaltyReportUI ui;

    public MembershipPerformanceUI(LoyaltyReportUI ui) {
        this.ui = ui;
    }

    public void render(MembershipPerformanceReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printCriteriaSection(result.getCriteria());
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printCalloutsSection(result.getCallouts());
        ui.printDocumentFooter();
    }
}
