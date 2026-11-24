package ui;

import java.io.Console;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;
import util.SessionMonitor;

/** Validated console input on top of one shared Scanner; every method loops until the input is valid. */
public class ConsoleInput {

    /** Thrown when the input stream ends (Ctrl+D / Ctrl+Z or piped input finished). */
    public static class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /** Creates the exception with a standard message. */
        public InputClosedException() {
            super("Input stream closed");
        }
    }

    /** Thrown when the user was idle for longer than the session timeout. */
    public static class SessionExpiredException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /** Creates the exception with a standard message. */
        public SessionExpiredException() {
            super("Session expired because of inactivity");
        }
    }

    private final Scanner scanner;
    private final SessionMonitor monitor;

    /** Creates an input helper; the monitor may be null when no session timeout applies (login screen). */
    public ConsoleInput(Scanner scanner, SessionMonitor monitor) {
        this.scanner = scanner;
        this.monitor = monitor;
    }

    /** Prints the prompt and returns the trimmed line (possibly empty). */
    public String readLine(String prompt) {
        return readRaw(prompt).trim();
    }

    /** Prints the prompt and returns a non-blank line. */
    public String readNonBlank(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("[ERROR] This field cannot be empty.");
        }
    }

    /** Reads a whole number between min and max (inclusive); letters and out-of-range values are rejected. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("[ERROR] Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] '" + line + "' is not a valid whole number.");
            }
        }
    }

    /** Reads a whole number of at least 1. */
    public int readPositiveInt(String prompt) {
        return readInt(prompt, 1, Integer.MAX_VALUE);
    }

    /** Reads a non-negative money amount rounded to 2 decimals (HALF_UP). */
    public BigDecimal readMoney(String prompt) {
        while (true) {
            String line = readLine(prompt);
            BigDecimal parsed = parseMoney(line);
            if (parsed != null) {
                return parsed;
            }
            System.out.println("[ERROR] '" + line + "' is not a valid amount (example: 49.50).");
        }
    }

    /** Reads an optional money amount; a blank line means "keep the current value" and returns null. */
    public BigDecimal readOptionalMoney(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return null;
            }
            BigDecimal parsed = parseMoney(line);
            if (parsed != null) {
                return parsed;
            }
            System.out.println("[ERROR] '" + line + "' is not a valid amount (example: 49.50).");
        }
    }

    /** Reads an optional whole number; a blank line means "keep the current value" and returns null. */
    public Integer readOptionalInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return null;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return Integer.valueOf(value);
                }
                System.out.println("[ERROR] Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] '" + line + "' is not a valid whole number.");
            }
        }
    }

    /** Asks a yes/no question and returns true for y/yes. */
    public boolean readYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (y/n): ").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("[ERROR] Please answer y or n.");
        }
    }

    /** Reads a password without echo when a real console exists; otherwise falls back to the Scanner. */
    public String readPassword(String prompt) {
        Console console = System.console();
        if (console == null) {
            return readRaw(prompt);
        }
        char[] chars = console.readPassword(prompt);
        if (chars == null) {
            throw new InputClosedException();
        }
        String password = new String(chars);
        Arrays.fill(chars, ' ');
        registerActivity();
        return password;
    }

    private String readRaw(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        String line;
        try {
            line = scanner.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            throw new InputClosedException();
        }
        registerActivity();
        return line;
    }

    private void registerActivity() {
        if (monitor != null) {
            if (monitor.isExpired()) {
                throw new SessionExpiredException();
            }
            monitor.touch();
        }
    }

    private static BigDecimal parseMoney(String text) {
        try {
            BigDecimal value = new BigDecimal(text);
            if (value.signum() < 0) {
                return null;
            }
            return value.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
