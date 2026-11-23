package repository;

import model.Customer;

/** Stores customers in data/customers.csv using Customer.toCsv and Customer.fromCsv. */
public class CustomerRepository extends CsvFileRepository<Customer> {
    /** Uses the default file data/customers.csv. */
    public CustomerRepository() {
        this("data/customers.csv");
    }

    /** Uses a custom file path (handy for tests). */
    public CustomerRepository(String path) {
        super(path);
    }

    /** Parses one line into a Customer. */
    @Override
    protected Customer parse(String line) {
        return Customer.fromCsv(line);
    }

    /** Formats a Customer as one line. */
    @Override
    protected String format(Customer item) {
        return item.toCsv();
    }
}
