package tarumtresort.report;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Brian
 *
 * ASCII bar chart holder: a title plus one or more labelled bars.
 * Each bar carries a numeric value (and an optional detail string that is
 * shown next to the value, e.g. "3 tasks" or "16.1%").
 */
public class ReportChart {

    private final String title;
    private final List<Bar> bars = new ArrayList<>();

    public ReportChart(String title) {
        this.title = title;
    }

    public void addBar(String label, double value, String detail) {
        bars.add(new Bar(label, value, detail));
    }

    public String getTitle() {
        return title;
    }

    public List<Bar> getBars() {
        return bars;
    }

    public boolean isEmpty() {
        return bars.isEmpty();
    }

    public static class Bar {
        private final String label;
        private final double value;
        private final String detail;

        Bar(String label, double value, String detail) {
            this.label = label;
            this.value = value;
            this.detail = detail;
        }

        public String getLabel() {
            return label;
        }

        public double getValue() {
            return value;
        }

        public String getDetail() {
            return detail;
        }
    }
}