package tarumtresort.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import tarumtresort.adt.LinkedListInterface;
import tarumtresort.entity.Payment;
import tarumtresort.entity.RedemptionRecord;
import tarumtresort.utility.Ansi;
import tarumtresort.utility.ConsoleUtil;
import tarumtresort.utility.TablePrinter;

public class PaymentUI {

    private final Scanner scanner;

    public PaymentUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void printBill(double roomCharge, int discountPercent, double discount,
            String promoLabel, double promoDiscount,
            String[] voucherLabels, double[] voucherValues,
            double serviceCharge, double tax, double lateFee, double total) {
        printBillHeader();
        printBillTable(roomCharge, discountPercent, discount, promoLabel, promoDiscount,
                voucherLabels, voucherValues, serviceCharge, tax, lateFee, total);
        System.out.println("  " + Ansi.green("Thank you for choosing TARUMT Resort!"));
        System.out.println();
    }

    /** Backwards-compatible overload without a promotion. */
    public void printBill(double roomCharge, int discountPercent, double discount,
            String[] voucherLabels, double[] voucherValues,
            double serviceCharge, double tax, double lateFee, double total) {
        printBill(roomCharge, discountPercent, discount, null, 0.0,
                voucherLabels, voucherValues, serviceCharge, tax, lateFee, total);
    }

    private void printBillHeader() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("          PAYMENT BILL");
        System.out.println("========================================");
        System.out.println();
    }

    private void printBillTable(double roomCharge, int discountPercent, double discount,
            String promoLabel, double promoDiscount,
            String[] voucherLabels, double[] voucherValues,
            double serviceCharge, double tax, double lateFee, double total) {
        String[] header = {"Description", "Amount (RM)"};

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Room Charge", String.format("%.2f", roomCharge)});

        if (voucherLabels != null) {
            for (int i = 0; i < voucherLabels.length; i++) {
                rows.add(new String[]{voucherLabels[i], "-" + String.format("%.2f", voucherValues[i])});
            }
        }

        if (discount > 0) {
            rows.add(new String[]{"Member Discount (" + discountPercent + "%)",
                "-" + String.format("%.2f", discount)});
        }

        if (promoDiscount > 0 && promoLabel != null) {
            rows.add(new String[]{"Promotion: " + promoLabel,
                "-" + String.format("%.2f", promoDiscount)});
        }

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

    /**
     * Lists the applicable vouchers and returns the chosen redemption id,
     * or null when the staff picks 0 (no more vouchers).
     */
    public String selectVoucher(LinkedListInterface<RedemptionRecord> vouchers) {
        System.out.println();
        String[] header = {"#", "Voucher", "Room Type", "Value"};
        String[][] rows = new String[vouchers.size()][4];
        for (int i = 0; i < vouchers.size(); i++) {
            RedemptionRecord v = vouchers.get(i);
            boolean percent = v.getDiscountPercent() != null;
            String roomName = v.getRoomType() == null ? "Any" : v.getRoomType().name();
            rows[i] = new String[]{
                String.valueOf(i + 1),
                percent
                    ? v.getDiscountPercent() + "% OFF " + roomName + " Voucher"
                    : (v.getRoomType() == null ? "Generic Voucher" : "1-Night " + roomName + " Voucher"),
                roomName,
                percent
                    ? v.getDiscountPercent() + "%"
                    : String.format("%.2f", v.getVoucherValue())
            };
        }
        TablePrinter.displayTable(header, rows);
        System.out.println(" 0. No more vouchers");
        System.out.println("========================================");

        while (true) {
            System.out.print("Apply a voucher (0 to skip): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                int idx = Integer.parseInt(line);
                if (idx == 0) {
                    System.out.println();
                    return null;
                }
                if (idx >= 1 && idx <= vouchers.size()) {
                    System.out.println();
                    return vouchers.get(idx - 1).getRedemptionId();
                }
            } catch (NumberFormatException e) {
                // retry
            }
            ConsoleUtil.printError("Please enter a number between 0 and " + vouchers.size() + ".");
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
