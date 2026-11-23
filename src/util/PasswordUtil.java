package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Password hashing (SHA-256) and password-policy helpers. Passwords are never stored in plain text. */
public final class PasswordUtil {

    /** Minimum number of characters for a new password. */
    public static final int MIN_LENGTH = 6;

    private PasswordUtil() {
        // static utility class: no instances
    }

    /** Returns the lowercase hex SHA-256 digest (64 characters) of the given password. */
    public static String hash(String plain) {
        if (plain == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform must provide SHA-256, so this is a programming/environment error.
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    /** Returns true when the plain password hashes to the expected value (constant-time comparison). */
    public static boolean verify(String plain, String expectedHash) {
        if (plain == null || expectedHash == null) {
            return false;
        }
        byte[] actual = hash(plain).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    /** Returns true when the password meets the minimum policy (long enough, not blank). */
    public static boolean isAcceptable(String plain) {
        return plain != null && !plain.isBlank() && plain.length() >= MIN_LENGTH;
    }

    /** Returns a one-line description of the password policy for error messages. */
    public static String describePolicy() {
        return "Password must be at least " + MIN_LENGTH + " characters long.";
    }
}
