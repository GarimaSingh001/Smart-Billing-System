package repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared CSV file logic: creates missing files, skips malformed lines and
 * collects warnings (the ui layer prints them, this class never prints).
 */
public abstract class CsvFileRepository<T> implements Repository<T> {
    private final Path file;
    private final List<String> warnings = new ArrayList<>();

    /** Creates a repository for the given file path. */
    protected CsvFileRepository(String path) {
        this.file = Path.of(path);
    }

    /** Converts one CSV line to an object; throw a RuntimeException if malformed. */
    protected abstract T parse(String line);

    /** Converts one object to a CSV line. */
    protected abstract String format(T item);

    /** Returns warnings (skipped lines) from the last load. */
    public List<String> getWarnings() {
        return List.copyOf(warnings);
    }

    /** Returns the file this repository uses. */
    public Path getFile() {
        return file;
    }

    /** Loads all valid lines; creates the file and returns an empty list if missing. */
    @Override
    public List<T> loadAll() throws IOException {
        warnings.clear();
        List<T> result = new ArrayList<>();
        if (Files.notExists(file)) {
            createParentFolder();
            Files.createFile(file);
            return result;
        }
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                try {
                    T item = parse(line);
                    if (item == null) {
                        throw new IllegalArgumentException("empty result");
                    }
                    result.add(item);
                } catch (RuntimeException e) {
                    warnings.add("Skipped line " + lineNo + " in " + file.getFileName()
                            + " (" + e.getMessage() + ")");
                }
            }
        }
        return result;
    }

    /** Writes all items to a temp file, then swaps it in so a crash cannot corrupt data. */
    @Override
    public void saveAll(List<T> items) throws IOException {
        createParentFolder();
        Path temp = Path.of(file.toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp)) {
            for (T item : items) {
                writer.write(format(item));
                writer.newLine();
            }
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Appends one line to the end of the file. */
    protected void appendLine(String line) throws IOException {
        createParentFolder();
        try (BufferedWriter writer = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        }
    }

    private void createParentFolder() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}