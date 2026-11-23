package ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

/** Safe console input: every method loops until the input is valid and never crashes. */
public final class InputHelper {
    private static final Scanner SCANNER = new Scanner(System.in);

    private InputHelper() {
    }

    /** Returns the one shared Scanner so all menus read from the same place. */
    public static Scanner getScanner() {
        return SCANNER;
    }

    /** Asks for a whole number between min and max (inclusive). */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("'" + text + "' is not a whole number. Try again.");
            }
        }
    }

    /** Asks for a whole number of at least 1. */
    public static int readPositiveInt(String prompt) {
        return readInt(prompt, 1, Integer.MAX_VALUE);
    }

    /** Asks for text that is not empty. */
    public static String readNonBlank(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isBlank()) {
                return text;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    /** Asks for an amount of money (zero or more), rounded to 2 decimals. */
    public static BigDecimal readMoney(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                BigDecimal value = new BigDecimal(text);
                if (value.signum() >= 0) {
                    return value.setScale(2, RoundingMode.HALF_UP);
                }
                System.out.println("Amount cannot be negative.");
            } catch (NumberFormatException e) {
                System.out.println("'" + text + "' is not a valid amount, for example 49.50.");
            }
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SCANNER.hasNextLine()) {
            throw new IllegalStateException("Input stream closed.");
        }
        return SCANNER.nextLine().trim();
    }
}
