package model;

/** Billing counter staff: creates bills, searches products and views their own recent bills. */
public class Cashier extends User {

    /** Creates a cashier account from a hashed password. */
    public Cashier(String userId, String username, String passwordHash) {
        super(userId, username, passwordHash, Role.CASHIER);
    }

    /** Returns the title of the cashier menu. */
    @Override
    public String getMenuTitle() {
        return "CASHIER COUNTER";
    }

    /** Describes the cashier's permissions. */
    @Override
    public String getAccessSummary() {
        return "Billing, product search and own recent bills";
    }
}
