package test;

/** Runs every Member A test in one go. Run: java -cp out test.RunAllTests */
public class RunAllTests {

    /** Runs all test classes and exits with code 1 when anything failed. */
    public static void main(String[] args) throws Exception {
        PasswordUtilTest.run();
        UserTest.run();
        AuthServiceTest.run();
        AuthServiceConcurrencyTest.run();
        SessionMonitorTest.run();
        ConsoleInputTest.run();
        TestSupport.finish();
    }
}
