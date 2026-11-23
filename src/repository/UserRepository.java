package repository;

import model.User;

/** Stores users in data/users.csv using User.toCsv and User.fromCsv. */
public class UserRepository extends CsvFileRepository<User> {
    /** Uses the default file data/users.csv. */
    public UserRepository() {
        this("data/users.csv");
    }

    /** Uses a custom file path (handy for tests). */
    public UserRepository(String path) {
        super(path);
    }

    /** Parses one line into a User. */
    @Override
    protected User parse(String line) {
        return User.fromCsv(line);
    }

    /** Formats a User as one line. */
    @Override
    protected String format(User item) {
        return item.toCsv();
    }
}
