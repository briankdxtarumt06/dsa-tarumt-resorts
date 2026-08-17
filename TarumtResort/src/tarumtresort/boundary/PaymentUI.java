package tarumtresort.boundary;

public class PaymentUI {
    
    public void printBill(double roomCharge, double serviceCharge, double tax, double lateFee, double total) {
        printBillTable(buildBillTableData(roomCharge, serviceCharge, tax, lateFee, total));
    }

    private String[][] buildBillTableData(double roomCharge, double serviceCharge, double tax, double lateFee, double total) {
        boolean hasLateFee = lateFee > 0;
        int rowCount = hasLateFee ? 6 : 5;

        String[][] data = new String[rowCount][2];
        data[0] = new String[]{"Description", "Amount (RM)"};
        data[1] = new String[]{"Room Charge", String.format("%.2f", roomCharge)};
        data[2] = new String[]{"Service Charge", String.format("%.2f", serviceCharge)};
        data[3] = new String[]{"Tax", String.format("%.2f", tax)};

        if (hasLateFee) {
            data[4] = new String[]{"Late Checkout Fee", String.format("%.2f", lateFee)};
            data[5] = new String[]{"Total", String.format("%.2f", total)};
        } else {
            data[4] = new String[]{"Total", String.format("%.2f", total)};
        }

        return data;
    }

    private void printBillTable(String[][] data) {
        System.out.println("\n[Bill]");
        for (String[] row : data) {
            for (String col : row) {
                System.out.printf("%-20s", col);
            }
            System.out.println();
        }
    }
}
