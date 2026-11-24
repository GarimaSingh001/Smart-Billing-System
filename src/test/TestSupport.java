package test;

/** Tiny assertion helper so tests can be plain main methods (no JUnit needed). */
public final class TestSupport {

    /** A block of code that may throw. */
    public interface Block {
        /** Runs the code under test. */
        void run() throws Exception;
    }

    private static int passed;
    private static int failed;

    private TestSupport() {
    }

    /** Records one check and prints PASS or FAIL. */
    public static synchronized void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    /** Passes when the block throws an exception of the expected type. */
    public static void expectThrows(String name, Class<? extends Throwable> expected, Block block) {
        try {
            block.run();
            check(name + " (expected " + expected.getSimpleName() + ")", false);
        } catch (Throwable t) {
            check(name, expected.isInstance(t));
        }
    }

    /** Prints a section heading. */
    public static void section(String title) {
        System.out.println("\n" + title);
    }

    /** Prints the summary and returns the number of failed checks. */
    public static synchronized int summary() {
        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        return failed;
    }

    /** Prints the summary and ends the JVM with exit code 1 when anything failed. */
    public static void finish() {
        if (summary() > 0) {
            System.exit(1);
        }
    }
}
