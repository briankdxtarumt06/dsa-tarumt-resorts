package tarumtresort.report;

import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

// Author: Brian Kam Ding Xian
public class ReportChart implements Comparable<ReportChart> {

    private final String title;
    private final LinkedListInterface<Bar> bars = new LinkedList<>();

    public ReportChart(String title) {
        this.title = title;
    }

    public void addBar(String label, double value, String detail) {
        bars.addBack(new Bar(label, value, detail));
    }

    public String getTitle() {
        return title;
    }

    public LinkedListInterface<Bar> getBars() {
        return bars;
    }

    public boolean isEmpty() {
        return bars.isEmpty();
    }

    @Override
    public int compareTo(ReportChart other) {
        return title.compareTo(other.title);
    }

    public static class Bar implements Comparable<Bar> {
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

        @Override
        public int compareTo(Bar other) {
            return label.compareTo(other.label);
        }
    }
}