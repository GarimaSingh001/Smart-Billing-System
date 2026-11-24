package test;

import static test.TestSupport.check;
import static test.TestSupport.expectThrows;
import static test.TestSupport.section;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Admin;
import model.Cashier;
import model.Role;
import model.User;
import util.PasswordUtil;

/** Tests inheritance, polymorphism, CSV round trip and ordering of users. Run: java -cp out test.UserTest */
public class UserTest {

    /** Runs all checks. */
    public static void run() {
        section("UserTest");
        User admin = new Admin("U001", "admin", PasswordUtil.hash("admin123"));
        User cashier = new Cashier("U002", "cashier", PasswordUtil.hash("cash123"));

        check("Admin is a User with role ADMIN", admin instanceof User && admin.getRole() == Role.ADMIN);
        check("Cashier has role CASHIER", cashier.getRole() == Role.CASHIER);
        check("menu titles differ by role (polymorphism)", !admin.getMenuTitle().equals(cashier.getMenuTitle()));
        check("correct password accepted", admin.checkPassword("admin123"));
        check("wrong password rejected", !admin.checkPassword("wrong"));
        check("toString never shows the hash", !admin.toString().contains(PasswordUtil.hash("admin123")));

        String csv = admin.toCsv();
        User copy = User.fromCsv(csv);
        check("CSV round trip keeps username", copy.getUsername().equals("admin"));
        check("CSV round trip creates the right subclass", copy instanceof Admin);
        check("CSV round trip keeps the password valid", copy.checkPassword("admin123"));
        check("CSV never contains the plain password", !csv.contains("admin123"));
        check("fromCsv builds a Cashier for role CASHIER", User.fromCsv(cashier.toCsv()) instanceof Cashier);

        expectThrows("blank CSV line rejected", IllegalArgumentException.class, new TestSupport.Block() {
            @Override
            public void run() throws Exception {
                User.fromCsv("  ");
            }
        });
        expectThrows("wrong column count rejected", IllegalArgumentException.class, new TestSupport.Block() {
            @Override
            public void run() throws Exception {
                User.fromCsv("U9,bob");
            }
        });
        expectThrows("unknown role rejected", IllegalArgumentException.class,
                new TestSupport.Block() {
            @Override
            public void run() throws Exception {
                User.fromCsv("U9,bob," + PasswordUtil.hash("secret1") + ",MANAGER");
            }
        });
        expectThrows("plain-text password in CSV rejected", IllegalArgumentException.class,
                new TestSupport.Block() {
            @Override
            public void run() throws Exception {
                User.fromCsv("U9,bob,secret1,CASHIER");
            }
        });

        check("users with the same name (any case) are equal", admin.equals(new Admin("U9", "ADMIN", PasswordUtil.hash("x12345"))));
        List<User> users = new ArrayList<>(List.of(cashier, admin, new Cashier("U003", "anil", PasswordUtil.hash("anil123"))));
        Collections.sort(users);
        check("sort order: admins first, then cashiers by name",
                users.get(0).getUsername().equals("admin") && users.get(1).getUsername().equals("anil")
                        && users.get(2).getUsername().equals("cashier"));
    }

    /** Standalone entry point. */
    public static void main(String[] args) {
        run();
        TestSupport.finish();
    }
}
