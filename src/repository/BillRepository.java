package repository;

import java.io.IOException;
import java.util.List;
import model.Bill;

/** Stores finished bills in data/bills.csv; each bill is appended immediately. */
public class BillRepository extends CsvFileRepository<Bill> {
    /** Uses the default file data/bills.csv. */
    public BillRepository() {
        this("data/bills.csv");
    }

    /** Uses a custom file path (handy for tests). */
    public BillRepository(String path) {
        super(path);
    }

    /** Parses a bill line (needs Bill.fromCsv from Member C). */
    @Override
    protected Bill parse(String line) {
        return Bill.fromCsv(line);
    }

    /** Formats a bill line. */
    @Override
    protected String format(Bill item) {
        return item.toCsv();
    }

    /** Saves one finished bill straight away by appending it to the file. */
    public void appendBill(Bill bill) throws IOException {
        appendLine(bill.toCsv());
    }

    /** Returns the highest invoice number saved so far (0 if none), e.g. SM-0007 gives 7. */
    public int getLastInvoiceNumber() throws IOException {
        int max = 0;
        List<Bill> bills = loadAll();
        for (Bill bill : bills) {
            String no = bill.getInvoiceNo();
            int dash = no.lastIndexOf('-');
            try {
                max = Math.max(max, Integer.parseInt(no.substring(dash + 1)));
            } catch (NumberFormatException e) {
                // an odd invoice number is ignored; the other bills still count
            }
        }
        return max;
    }
}
