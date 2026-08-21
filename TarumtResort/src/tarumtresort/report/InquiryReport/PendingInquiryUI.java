package tarumtresort.report.InquiryReport;

/**
 *
 * @author Wen Ling
 */
public class PendingInquiryUI {

    private static final String TITLE = "PENDING INQUIRY OVERVIEW REPORT";

    private final InquiryReportUI ui;

    public PendingInquiryUI(InquiryReportUI ui) {
        this.ui = ui;
    }

    public void render(PendingInquiryReport.Result result) {
        ui.printDocumentHeader(TITLE);
        ui.printTableSection(result.getTable());
        ui.printChartSection(TITLE, result.getCharts());
        ui.printSummarySection(result.getSummary());
        ui.printDocumentFooter();
    }
}