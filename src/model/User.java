package model;

import java.util.Locale;
import java.util.Objects;
import util.PasswordUtil;

/** Abstract base class for everyone who can log in; subclasses decide the menu they see. */
public abstract class User implements Comparable<User> {

    private static final String HASH_PATTERN = "[0-9a-f]{64}";

    private final String userId;
    private final String username;
    private String passwordHash;
    private final Role role;

    /** Creates a user from an already hashed password (never pass a plain password here). */
    protected User(String userId, String username, String passwordHash, Role role) {
        this.userId = requireText(userId, "User id");
        this.username = requireText(username, "Username");
        this.passwordHash = requireHash(passwordHash);
        this.role = Objects.requireNonNull(role, "Role must not be null");
    }

    /** Returns the unique user id, for example U001. */
    public String getUserId() {
        return userId;
    }

    /** Returns the login name. */
    public String getUsername() {
        return username;
    }

    /** Returns the role of this user. */
    public Role getRole() {
        return role;
    }

    /** Returns true when the plain password matches the stored SHA-256 hash. */
    public boolean checkPassword(String plain) {
        return PasswordUtil.verify(plain, passwordHash);
    }

    /** Replaces the stored hash after a password change or reset. */
    public void setPasswordHash(String newHash) {
        this.passwordHash = requireHash(newHash);
    }

    /** Returns the title shown at the top of this user's menu. */
    public abstract String getMenuTitle();

    /** Returns a one-line description of what this user is allowed to do. */
    public abstract String getAccessSummary();

    /** Serialises this user as userId,username,passwordHash,ROLE. */
    public String toCsv() {
        return String.join(",", userId, username, passwordHash, role.name());
    }

    /** Builds an Admin or Cashier from a CSV line; throws IllegalArgumentException for a bad line. */
    public static User fromCsv(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Empty user line");
        }
        String[] parts = line.split(",", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Expected 4 columns but found " + parts.length);
        }
        return create(parts[0].trim(), parts[1].trim(), parts[2].trim(), Role.fromString(parts[3]));
    }

    /** Factory that returns the correct subclass for the given role (polymorphism). */
    public static User create(String userId, String username, String passwordHash, Role role) {
        Objects.requireNonNull(role, "Role must not be null");
        switch (role) {
            case ADMIN: return new Admin(userId, username, passwordHash);
            case CASHIER: return new Cashier(userId, username, passwordHash);
            default: throw new IllegalArgumentException("Unsupported role");
        }
    }

    /** Orders users by role first and then by username, ignoring case. */
    @Override
    public int compareTo(User other) {
        int byRole = role.compareTo(other.role);
        return byRole != 0 ? byRole : username.compareToIgnoreCase(other.username);
    }

    /** Two users are equal when their usernames match, ignoring case. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User other = (User) o;
        return username.equalsIgnoreCase(other.username);
    }

    /** Hash code consistent with equals. */
    @Override
    public int hashCode() {
        return username.toLowerCase(Locale.ROOT).hashCode();
    }

    /** Returns a safe description that never contains the password hash. */
    @Override
    public String toString() {
        return username + " (" + role.getDisplayName() + ")";
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank() || value.contains(",")) {
            throw new IllegalArgumentException(label + " must be non-empty and must not contain commas");
        }
        return value.trim();
    }

    private static String requireHash(String hash) {
        if (hash == null || !hash.matches(HASH_PATTERN)) {
            throw new IllegalArgumentException("Password hash must be a 64-character lowercase SHA-256 hex string");
        }
        return hash;
    }
}
