package ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;
import model.User;
import util.SessionMonitor;

/**
 * Abstract base of every menu: shared input helpers, menu/table printing and one central error boundary.
 * Subclasses implement show(); they only call services and never hold business logic.
 */
public abstract class MenuHandler {

    /** Width of the banner lines. */
    protected static final int WIDTH = 64;

    /** Validated console input shared by all menus of one session. */
    protected final ConsoleInput input;
    /** The user who is logged in. */
    protected final User currentUser;

    private final Deque<String> breadcrumb = new ArrayDeque<>();

    /** Creates a menu for the logged-in user; the monitor tracks inactivity for this session. */
    protected MenuHandler(Scanner scanner, User currentUser, SessionMonitor monitor) {
        this.input = new ConsoleInput(scanner, monitor);
        this.currentUser = currentUser;
        this.breadcrumb.addLast(currentUser.getMenuTitle());
    }

    /** Runs the menu loop until the user logs out. */
    public abstract void show();

    /** Reads a menu choice between min and max, repeating until it is valid. */
    protected int readChoice(int min, int max) {
        return input.readInt("Enter choice [" + min + "-" + max + "]: ", min, max);
    }

    /** Prints a numbered menu; each row is {label, description} (rows[i][0] and rows[i][1]). */
    protected void printMenu(String title, String[][] rows) {
        int labelWidth = 0;
        for (String[] row : rows) {
            labelWidth = Math.max(labelWidth, row[0].length());
        }
        StringBuilder out = new StringBuilder();
        out.append('\n').append("=".repeat(WIDTH)).append('\n');
        out.append(' ').append(String.join(" > ", breadcrumb));
        if (!title.equals(breadcrumb.peekLast())) {
            out.append(" > ").append(title);
        }
        out.append('\n').append("=".repeat(WIDTH)).append('\n');
        for (int i = 0; i < rows.length; i++) {
            out.append(String.format(" %d. %-" + labelWidth + "s  %s%n", i + 1, rows[i][0], rows[i][1]));
        }
        out.append("-".repeat(WIDTH));
        System.out.println(out);
    }

    /** Prints an aligned text table with a header row; rows are lists of cell texts. */
    protected void printTable(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int c = 0; c < headers.length; c++) {
            widths[c] = headers[c].length();
        }
        for (String[] row : rows) {
            for (int c = 0; c < headers.length; c++) {
                widths[c] = Math.max(widths[c], row[c].length());
            }
        }
        StringBuilder out = new StringBuilder();
        appendRow(out, headers, widths);
        int total = headers.length - 1;
        for (int w : widths) {
            total += w + 1;
        }
        out.append("-".repeat(total)).append('\n');
        for (String[] row : rows) {
            appendRow(out, row, widths);
        }
        System.out.print(out);
    }

    /** Enters a sub-menu so that the banner shows where the user is. */
    protected void enter(String name) {
        breadcrumb.addLast(name);
    }

    /** Leaves the current sub-menu. */
    protected void leave() {
        if (breadcrumb.size() > 1) {
            breadcrumb.removeLast();
        }
    }

    /** Asks the user to confirm a risky action. */
    protected boolean confirm(String question) {
        return input.readYesNo(question);
    }

    /** Prints a success message. */
    protected void success(String message) {
        System.out.println("[OK] " + message);
    }

    /** Prints an information message. */
    protected void info(String message) {
        System.out.println("[INFO] " + message);
    }

    /** Prints an error message. */
    protected void error(String message) {
        System.out.println("[ERROR] " + message);
    }

    /**
     * Central error boundary: any failure of one operation is shown as a clear message and the menu continues.
     * Input-closed and session-expired signals are rethrown so that Main can end the session properly.
     */
    protected void execute(MenuAction action) {
        try {
            action.run();
        } catch (ConsoleInput.InputClosedException | ConsoleInput.SessionExpiredException e) {
            throw e;
        } catch (Exception e) {
            error(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            System.out.println();
        }
    }

    private static void appendRow(StringBuilder out, String[] cells, int[] widths) {
        for (int c = 0; c < cells.length; c++) {
            out.append(String.format("%-" + widths[c] + "s", cells[c]));
            out.append(c < cells.length - 1 ? ' ' : '\n');
        }
    }
}
