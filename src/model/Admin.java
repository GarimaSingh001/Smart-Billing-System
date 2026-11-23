package model;

/** Store administrator: manages products, stock, reports and user accounts. */
public class Admin extends User {

    /** Creates an administrator account from a hashed password. */
    public Admin(String userId, String username, String passwordHash) {
        super(userId, username, passwordHash, Role.ADMIN);
    }

    /** Returns the title of the administrator menu. */
    @Override
    public String getMenuTitle() {
        return "ADMIN CONSOLE";
    }

    /** Describes the administrator's permissions. */
    @Override
    public String getAccessSummary() {
        return "Products, inventory, sales reports and user accounts";
    }
}
