package test;

import static test.TestSupport.check;
import static test.TestSupport.expectThrows;
import static test.TestSupport.section;

import exception.AuthenticationException;
import exception.InvalidInputException;
import model.Role;
import model.User;
import service.AuditLog;
import service.AuthService;

/** Tests login, lockout, user management and the audit log. Run: java -cp out test.AuthServiceTest */
public class AuthServiceTest {

    private static AuthService freshService() {
        return new AuthService(AuthService.demoUsers(), new AuditLog());
    }

    /** Runs all checks. */
    public static void run() throws Exception {
        section("AuthServiceTest - login");
        AuthService auth = freshService();
        check("admin logs in with admin/admin123", auth.login("admin", "admin123").getRole() == Role.ADMIN);
        check("cashier logs in with cashier/cash123", auth.login("cashier", "cash123").getRole() == Role.CASHIER);
        check("username is case-insensitive", auth.login("ADMIN", "admin123").getUsername().equals("admin"));
        check("username is trimmed", auth.login("  admin  ", "admin123") != null);
        expectThrows("wrong password rejected", AuthenticationException.class, () -> auth.login("admin", "nope"));
        expectThrows("unknown user rejected", AuthenticationException.class, () -> auth.login("ghost", "whatever"));
        expectThrows("blank username rejected", AuthenticationException.class, () -> auth.login("", "x"));
        expectThrows("blank password rejected", AuthenticationException.class, () -> auth.login("admin", ""));
        try {
            auth.login("admin", "bad");
        } catch (AuthenticationException e) {
            check("wrong-password message shows attempts left", e.getMessage().contains("attempt(s) left"));
            check("message does not reveal the real password", !e.getMessage().contains("admin123"));
        }

        section("AuthServiceTest - lockout after 3 failures");
        AuthService lockTest = freshService();
        for (int i = 0; i < 2; i++) {
            try {
                lockTest.login("cashier", "wrong" + i);
            } catch (AuthenticationException ignoredForTest) {
                check("failure " + (i + 1) + " is reported", true);
            }
        }
        check("not locked after 2 failures", !lockTest.isLocked("cashier"));
        try {
            lockTest.login("cashier", "wrong2");
        } catch (AuthenticationException e) {
            check("3rd failure says account is locked", e.getMessage().contains("locked"));
        }
        check("account is locked after 3 failures", lockTest.isLocked("cashier"));
        try {
            lockTest.login("cashier", "cash123");
            check("locked account rejects even the correct password", false);
        } catch (AuthenticationException e) {
            check("locked account rejects even the correct password", e.getMessage().contains("locked"));
        }
        check("other accounts are not affected", lockTest.login("admin", "admin123") != null);
        User admin = lockTest.login("admin", "admin123");
        lockTest.unlock("cashier", admin);
        check("admin can unlock the account", !lockTest.isLocked("cashier"));
        check("unlocked user can log in again", lockTest.login("cashier", "cash123") != null);

        AuthService reset = freshService();
        try {
            reset.login("admin", "w1");
        } catch (AuthenticationException ignoredForTest) {
            check("first failure counted", true);
        }
        reset.login("admin", "admin123");
        for (int i = 0; i < 2; i++) {
            try {
                reset.login("admin", "w" + i);
            } catch (AuthenticationException ignoredForTest) {
                check("counter restarts after a successful login (" + (i + 1) + ")", !reset.isLocked("admin"));
            }
        }

        section("AuthServiceTest - user management");
        AuthService users = freshService();
        User boss = users.login("admin", "admin123");
        User created = users.createUser("anil_k", "secret1", Role.CASHIER);
        check("new user gets the next id", created.getUserId().equals("U003"));
        check("new user can log in", users.login("anil_k", "secret1").getRole() == Role.CASHIER);
        check("user list has 3 entries", users.getAllUsers().size() == 3);
        expectThrows("duplicate username rejected", InvalidInputException.class, () -> users.createUser("ANIL_K", "secret1", Role.CASHIER));
        expectThrows("short password rejected", InvalidInputException.class, () -> users.createUser("bob", "123", Role.CASHIER));
        expectThrows("bad username rejected", InvalidInputException.class, () -> users.createUser("a b", "secret1", Role.CASHIER));
        expectThrows("null role rejected", InvalidInputException.class, () -> users.createUser("bob", "secret1", null));
        expectThrows("cannot delete yourself", InvalidInputException.class, () -> users.removeUser("admin", boss));
        expectThrows("cannot delete unknown user", InvalidInputException.class, () -> users.removeUser("ghost", boss));
        users.removeUser("anil_k", boss);
        check("deleted user is gone", users.findByUsername("anil_k").isEmpty());
        expectThrows("deleted user cannot log in", AuthenticationException.class, () -> users.login("anil_k", "secret1"));

        users.createUser("second_admin", "secret1", Role.ADMIN);
        users.removeUser("admin", users.login("second_admin", "secret1"));
        expectThrows("last administrator cannot be deleted", InvalidInputException.class,
                () -> users.removeUser("second_admin", users.login("cashier", "cash123")));

        AuthService pwd = freshService();
        User cashier = pwd.login("cashier", "cash123");
        pwd.changePassword(cashier, "cash123", "newpass9");
        check("old password stops working", !cashier.checkPassword("cash123"));
        check("new password works", pwd.login("cashier", "newpass9") != null);
        expectThrows("change with wrong current password rejected", AuthenticationException.class,
                () -> pwd.changePassword(cashier, "wrong", "another1"));
        expectThrows("change to weak password rejected", InvalidInputException.class,
                () -> pwd.changePassword(cashier, "newpass9", "abc"));
        pwd.resetPassword("cashier", "reset999", pwd.login("admin", "admin123"));
        check("admin reset works", pwd.login("cashier", "reset999") != null);

        section("AuthServiceTest - audit log");
        AuditLog log = new AuditLog(3);
        AuthService audited = new AuthService(AuthService.demoUsers(), log);
        audited.login("admin", "admin123");
        check("successful login is logged", log.size() == 1 && log.getRecent(1).get(0).contains("Login successful"));
        for (int i = 0; i < 5; i++) {
            log.record("test", "event " + i);
        }
        check("log keeps only its capacity", log.size() == 3);
        check("newest event comes first", log.getRecent(1).get(0).contains("event 4"));
        expectThrows("capacity 0 rejected", IllegalArgumentException.class, () -> new AuditLog(0));
    }

    /** Standalone entry point. */
    public static void main(String[] args) throws Exception {
        run();
        TestSupport.finish();
    }
}
