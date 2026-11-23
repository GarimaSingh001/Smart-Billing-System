package test;

import static test.TestSupport.check;
import static test.TestSupport.expectThrows;
import static test.TestSupport.section;

import java.math.BigDecimal;
import java.util.Scanner;
import ui.ConsoleInput;
import util.SessionMonitor;

/** Tests that bad input never crashes and is asked again. Run: java -cp out test.ConsoleInputTest */
public class ConsoleInputTest {

    private static ConsoleInput over(String text) {
        return new ConsoleInput(new Scanner(text), null);
    }

    /** Runs all checks. */
    public static void run() throws InterruptedException {
        section("ConsoleInputTest");
        check("readInt skips letters and out-of-range values", over("abc\n99\n-1\n4\n").readInt("n: ", 1, 5) == 4);
        check("readPositiveInt rejects 0", over("0\n7\n").readPositiveInt("n: ") == 7);
        check("readNonBlank skips empty lines", over("\n   \nmilk\n").readNonBlank("s: ").equals("milk"));
        check("readMoney rounds HALF_UP to 2 decimals", over("12.345\n").readMoney("m: ").equals(new BigDecimal("12.35")));
        check("readMoney rejects text and negatives", over("abc\n-5\n9.5\n").readMoney("m: ").equals(new BigDecimal("9.50")));
        check("optional money: blank means keep", over("\n").readOptionalMoney("m: ").isEmpty());
        check("optional int: value is returned", over("12\n").readOptionalInt("n: ", 0, 20).orElse(-1) == 12);
        check("yes/no accepts 'Yes'", over("maybe\nYes\n").readYesNo("ok?"));
        check("yes/no accepts 'n'", !over("n\n").readYesNo("ok?"));
        expectThrows("end of input is reported, not an endless loop", ConsoleInput.InputClosedException.class,
                () -> over("abc\n").readInt("n: ", 1, 5));

        SessionMonitor monitor = new SessionMonitor(100, 20);
        monitor.start();
        ConsoleInput timed = new ConsoleInput(new Scanner("a\nb\n"), monitor);
        check("input works before the timeout", timed.readLine("x: ").equals("a"));
        Thread.sleep(400);
        expectThrows("input after the idle timeout ends the session", ConsoleInput.SessionExpiredException.class,
                () -> timed.readLine("x: "));
        monitor.stop();
    }

    /** Standalone entry point. */
    public static void main(String[] args) throws InterruptedException {
        run();
        TestSupport.finish();
    }
}
