package model;

/** Roles a SuperMart user can have; decides which menu is shown after login. */
public enum Role {
    ADMIN("Administrator"),
    CASHIER("Cashier");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    /** Returns a human-readable name for this role. */
    public String getDisplayName() {
        return displayName;
    }

    /** Parses a role from text, ignoring case and surrounding spaces. */
    public static Role fromString(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Role is missing");
        }
        try {
            return valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + text, e);
        }
    }
}
