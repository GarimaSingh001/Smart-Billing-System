package service;

import exception.AuthenticationException;
import exception.InvalidInputException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import model.Admin;
import model.Cashier;
import model.Role;
import model.User;
import util.PasswordUtil;

/**
 * Login, lockout and user-account management.
 * Methods are synchronized so that failed-attempt counting stays correct even if several threads call login.
 */
public class AuthService {

    /** Number of wrong passwords after which an account is locked for the rest of the run. */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private static final String USERNAME_PATTERN = "[A-Za-z0-9_]{3,20}";

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Set<String> lockedUsers = new HashSet<>();
    private final AuditLog auditLog;

    /** Creates the service from the users loaded by UserRepository; duplicates by username are ignored. */
    public AuthService(List<User> initialUsers, AuditLog auditLog) {
        this.auditLog = auditLog;
        for (User user : initialUsers) {
            users.putIfAbsent(key(user.getUsername()), user);
        }
    }

    /** Returns the two demo accounts: admin/admin123 and cashier/cash123 (stored hashed). */
    public static List<User> demoUsers() {
        List<User> demo = new ArrayList<>();
        demo.add(new Admin("U001", "admin", PasswordUtil.hash("admin123")));
        demo.add(new Cashier("U002", "cashier", PasswordUtil.hash("cash123")));
        return demo;
    }

    /** Checks the credentials and returns the user, or throws AuthenticationException with a clear message. */
    public synchronized User login(String username, String password) throws AuthenticationException {
        String key = key(username);
        if (key.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Username and password are required.");
        }
        User user = users.get(key);
        if (user == null) {
            auditLog.record(key, "Login failed: unknown username");
            throw new AuthenticationException("Invalid username or password.");
        }
        if (lockedUsers.contains(key)) {
            auditLog.record(key, "Login refused: account locked");
            throw new AuthenticationException("Account '" + user.getUsername() + "' is locked after "
                    + MAX_FAILED_ATTEMPTS + " failed attempts. Ask an administrator to unlock it.");
        }
        if (!user.checkPassword(password)) {
            int failures = failedAttempts.merge(key, 1, Integer::sum);
            auditLog.record(key, "Login failed (" + failures + "/" + MAX_FAILED_ATTEMPTS + ")");
            if (failures >= MAX_FAILED_ATTEMPTS) {
                lockedUsers.add(key);
                auditLog.record(key, "Account locked");
                throw new AuthenticationException("Too many failed attempts. Account '"
                        + user.getUsername() + "' is now locked.");
            }
            throw new AuthenticationException("Invalid username or password. "
                    + (MAX_FAILED_ATTEMPTS - failures) + " attempt(s) left.");
        }
        failedAttempts.remove(key);
        auditLog.record(key, "Login successful (" + user.getRole() + ")");
        return user;
    }

    /** Adds an already built user; throws InvalidInputException when the username exists. */
    public synchronized void addUser(User user) throws InvalidInputException {
        if (user == null) {
            throw new InvalidInputException("User must not be null.");
        }
        if (users.containsKey(key(user.getUsername()))) {
            throw new InvalidInputException("Username '" + user.getUsername() + "' already exists.");
        }
        users.put(key(user.getUsername()), user);
        auditLog.record("system", "User added: " + user.getUsername());
    }

    /** Validates the input, hashes the password and creates a new Admin or Cashier with a generated id. */
    public synchronized User createUser(String username, String plainPassword, Role role)
            throws InvalidInputException {
        String name = validateUsername(username);
        validatePassword(plainPassword);
        if (role == null) {
            throw new InvalidInputException("Role is required.");
        }
        User user = User.create(nextUserId(), name, PasswordUtil.hash(plainPassword), role);
        addUser(user);
        return user;
    }

    /** Deletes a user; blocks deleting yourself or the last administrator. */
    public synchronized void removeUser(String username, User actingUser) throws InvalidInputException {
        User target = requireExisting(username);
        if (target.equals(actingUser)) {
            throw new InvalidInputException("You cannot delete your own account.");
        }
        if (target.getRole() == Role.ADMIN && countAdmins() <= 1) {
            throw new InvalidInputException("At least one administrator must remain.");
        }
        Iterator<User> iterator = users.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(target)) {
                iterator.remove();
                break;
            }
        }
        failedAttempts.remove(key(username));
        lockedUsers.remove(key(username));
        auditLog.record(actorName(actingUser), "User deleted: " + target.getUsername());
    }

    /** Sets a new password for a user (admin action) and clears any lock. */
    public synchronized void resetPassword(String username, String newPlainPassword, User actingUser)
            throws InvalidInputException {
        User target = requireExisting(username);
        validatePassword(newPlainPassword);
        target.setPasswordHash(PasswordUtil.hash(newPlainPassword));
        clearLock(key(username));
        auditLog.record(actorName(actingUser), "Password reset for " + target.getUsername());
    }

    /** Lets a logged-in user change their own password after confirming the old one. */
    public synchronized void changePassword(User user, String oldPlain, String newPlain)
            throws AuthenticationException, InvalidInputException {
        if (!user.checkPassword(oldPlain)) {
            auditLog.record(user.getUsername(), "Password change refused: wrong current password");
            throw new AuthenticationException("Current password is incorrect.");
        }
        validatePassword(newPlain);
        user.setPasswordHash(PasswordUtil.hash(newPlain));
        auditLog.record(user.getUsername(), "Password changed");
    }

    /** Unlocks an account that was blocked after too many failed logins. */
    public synchronized void unlock(String username, User actingUser) throws InvalidInputException {
        User target = requireExisting(username);
        clearLock(key(username));
        auditLog.record(actorName(actingUser), "Account unlocked: " + target.getUsername());
    }

    /** Returns true when the account is currently locked. */
    public synchronized boolean isLocked(String username) {
        return lockedUsers.contains(key(username));
    }

    /** Finds a user by username, ignoring case. */
    public synchronized Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(key(username)));
    }

    /** Returns a read-only list of all users sorted by role and username. */
    public synchronized List<User> getAllUsers() {
        List<User> sorted = new ArrayList<>(users.values());
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    /** Returns the number of registered users. */
    public synchronized int getUserCount() {
        return users.size();
    }

    /** Returns the audit log used by this service. */
    public AuditLog getAuditLog() {
        return auditLog;
    }

    private void clearLock(String key) {
        lockedUsers.remove(key);
        failedAttempts.remove(key);
    }

    private User requireExisting(String username) throws InvalidInputException {
        User user = users.get(key(username));
        if (user == null) {
            throw new InvalidInputException("No user named '" + (username == null ? "" : username.trim()) + "'.");
        }
        return user;
    }

    private int countAdmins() {
        int admins = 0;
        for (User user : users.values()) {
            if (user.getRole() == Role.ADMIN) {
                admins++;
            }
        }
        return admins;
    }

    private String nextUserId() {
        int highest = 0;
        for (User user : users.values()) {
            String id = user.getUserId();
            if (id.length() > 1 && id.charAt(0) == 'U' && id.substring(1).matches("\\d+")) {
                highest = Math.max(highest, Integer.parseInt(id.substring(1)));
            }
        }
        return String.format("U%03d", highest + 1);
    }

    private static String validateUsername(String username) throws InvalidInputException {
        String name = username == null ? "" : username.trim();
        if (!name.matches(USERNAME_PATTERN)) {
            throw new InvalidInputException("Username must be 3-20 characters: letters, digits or underscore.");
        }
        return name;
    }

    private static void validatePassword(String plain) throws InvalidInputException {
        if (!PasswordUtil.isAcceptable(plain)) {
            throw new InvalidInputException(PasswordUtil.describePolicy());
        }
    }

    private static String actorName(User actingUser) {
        return actingUser == null ? "system" : actingUser.getUsername();
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
