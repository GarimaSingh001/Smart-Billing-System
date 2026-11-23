package test;

import static test.TestSupport.check;
import static test.TestSupport.expectThrows;
import static test.TestSupport.section;

import util.SessionMonitor;

/** Tests the idle-timeout daemon thread and its life cycle. Run: java -cp out test.SessionMonitorTest */
public class SessionMonitorTest {

    /** Runs all checks. */
    public static void run() throws InterruptedException {
        section("SessionMonitorTest");
        SessionMonitor monitor = new SessionMonitor(400, 50);
        check("thread state is NEW before start", monitor.getThreadState() == Thread.State.NEW);
        monitor.start();
        Thread.sleep(100);
        Thread.State running = monitor.getThreadState();
        check("thread is alive and sleeping/polling after start",
                running == Thread.State.TIMED_WAITING || running == Thread.State.RUNNABLE);
        check("not expired right after start", !monitor.isExpired());

        for (int i = 0; i < 6; i++) {
            Thread.sleep(100);
            monitor.touch();
        }
        check("activity keeps the session alive (600 ms > 400 ms timeout)", !monitor.isExpired());

        Thread.sleep(800);
        check("session expires after inactivity", monitor.isExpired());
        monitor.touch();
        check("touch cannot revive an expired session", monitor.isExpired());
        monitor.stop();
        check("monitor thread is TERMINATED after expiry and stop", monitor.getThreadState() == Thread.State.TERMINATED);

        SessionMonitor stopped = new SessionMonitor(60_000, 50);
        stopped.start();
        stopped.stop();
        check("stop() interrupts and joins the thread", stopped.getThreadState() == Thread.State.TERMINATED);
        check("a stopped session did not expire", !stopped.isExpired());

        expectThrows("zero timeout rejected", IllegalArgumentException.class, () -> new SessionMonitor(0, 10));
    }

    /** Standalone entry point. */
    public static void main(String[] args) throws InterruptedException {
        run();
        TestSupport.finish();
    }
}
