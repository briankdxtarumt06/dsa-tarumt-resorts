package tarumtresort.report.InquiryReport;

// Author: Fong Wen Ling
public class RoomTypeInquiryDistributionUI {

    private static final String TITLE = "ROOM TYPE INQUIRY DISTRIBUTION REPORT";

    private final InquiryReportUI ui;

    public RoomTypeInquiryDistributionUI(InquiryReportUI ui) {
        this.ui = ui;
    }

    public void render(RoomTypeInquiryDistributionReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}