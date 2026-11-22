package repository;

import java.io.IOException;
import java.util.List;

/** Generic storage contract: load every item, or save every item. */
public interface Repository<T> {
    /** Loads all items from storage. */
    List<T> loadAll() throws IOException;

    /** Replaces the stored items with the given list. */
    void saveAll(List<T> items) throws IOException;
}