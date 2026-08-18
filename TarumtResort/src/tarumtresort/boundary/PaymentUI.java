package tarumtresort.boundary;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Payment;
import tarumtresort.utility.TablePrinter;

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

    public void printPaymentRecords(LinkedListInterface<Payment> payments) {
        if (payments == null || payments.size() == 0) {
            System.out.println("\nNo payment records found.");
            return;
        }

        String[] header = {"Payment ID", "Conf. No.(s)", "Total (RM)", "Refunded (RM)", "Refund At", "Status"};
        String[][] rows = new String[payments.size()][6];
        for (int i = 0; i < payments.size(); i++) {
            Payment p = payments.get(i);

            StringBuilder confs = new StringBuilder(p.getReservationID() == null ? "-" : p.getReservationID());
            LinkedListInterface<String> numbers = p.getConfirmationNumbers();
            if (numbers != null) {
                for (int j = 0; j < numbers.size(); j++) {
                    if (numbers.get(j) != null && !numbers.get(j).equals(p.getReservationID())) {
                        confs.append(", ").append(numbers.get(j));
                    }
                }
            }

            rows[i] = new String[]{
                p.getPaymentID(),
                confs.toString(),
                String.format("%.2f", p.getTotalAmount()),
                String.format("%.2f", p.getRefundedAmount()),
                p.getRefundDateTime() != null ? p.getRefundDateTime().toString() : "-",
                p.getPaymentStatus().toString()
            };
        }

        System.out.println("\nPayment & Refund Records");
        TablePrinter.displayTable(header, rows);
    }
}
