package repository;

import model.Product;

/** Stores products in data/products.csv using Product.toCsv and Product.fromCsv. */
public class ProductRepository extends CsvFileRepository<Product> {
    /** Uses the default file data/products.csv. */
    public ProductRepository() {
        this("data/products.csv");
    }

    /** Uses a custom file path (handy for tests). */
    public ProductRepository(String path) {
        super(path);
    }

    /** Parses one line into a Product. */
    @Override
    protected Product parse(String line) {
        return Product.fromCsv(line);
    }

    /** Formats a Product as one line. */
    @Override
    protected String format(Product item) {
        return item.toCsv();
    }
}
