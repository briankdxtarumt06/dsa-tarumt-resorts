package tarumtresort.boundary;

import java.util.ArrayList;
import java.util.List;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Payment;
import tarumtresort.utility.Ansi;
import tarumtresort.utility.TablePrinter;

public class PaymentUI {

    public void printBill(double roomCharge, double serviceCharge, double tax, double lateFee, double total) {
        printBillHeader();
        printBillTable(roomCharge, serviceCharge, tax, lateFee, total);
        System.out.println("  " + Ansi.green("Thank you for choosing TARUMT Resort!"));
        System.out.println();
    }

    private void printBillHeader() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("          PAYMENT BILL");
        System.out.println("========================================");
        System.out.println();
    }

    private void printBillTable(double roomCharge, double serviceCharge, double tax, double lateFee, double total) {
        String[] header = {"Description", "Amount (RM)"};

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Room Charge", String.format("%.2f", roomCharge)});
        rows.add(new String[]{"Service Charge (10%)", String.format("%.2f", serviceCharge)});
        rows.add(new String[]{"Tax (6%)", String.format("%.2f", tax)});

        if (lateFee > 0) {
            rows.add(new String[]{"Late Checkout Fee", String.format("%.2f", lateFee)});
        }

        // highlight the total row
        rows.add(new String[]{
            Ansi.bold("TOTAL"),
            Ansi.bold("RM " + String.format("%.2f", total))
        });

        TablePrinter.displayTable(header, rows.toArray(new String[0][]));
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
