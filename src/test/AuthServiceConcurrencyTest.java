package test;

import static test.TestSupport.check;
import static test.TestSupport.section;

import exception.AuthenticationException;
import service.AuditLog;
import service.AuthService;

/**
 * Many threads hammer one account with wrong passwords at the same time.
 * Because login is synchronized, exactly three failures are counted and exactly one thread triggers the lock.
 * Run: java -cp out test.AuthServiceConcurrencyTest
 */
public class AuthServiceConcurrencyTest {

    private static final int THREADS = 40;

    /** Runs all checks. */
    public static void run() throws InterruptedException {
        section("AuthServiceConcurrencyTest (" + THREADS + " threads)");
        AuthService auth = new AuthService(AuthService.demoUsers(), new AuditLog(500));
        Counter attemptsLeftMessages = new Counter();
        Counter nowLockedMessages = new Counter();
        Counter alreadyLockedMessages = new Counter();

        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                try {
                    auth.login("cashier", "wrong-password");
                } catch (AuthenticationException e) {
                    String message = e.getMessage();
                    if (message.contains("attempt(s) left")) {
                        attemptsLeftMessages.increment();
                    } else if (message.contains("is now locked")) {
                        nowLockedMessages.increment();
                    } else if (message.contains("is locked after")) {
                        alreadyLockedMessages.increment();
                    }
                }
                }
            }, "attacker-" + i);
        }
        for (Thread worker : workers) {
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        check("exactly 2 'attempts left' replies", attemptsLeftMessages.get() == 2);
        check("exactly 1 thread caused the lock", nowLockedMessages.get() == 1);
        check("all other threads were refused as locked", alreadyLockedMessages.get() == THREADS - 3);
        check("account ends up locked", auth.isLocked("cashier"));
    }

    /** Standalone entry point. */
    public static void main(String[] args) throws InterruptedException {
        run();
        TestSupport.finish();
    }

    /** A synchronized counter demonstrates the shared-data rule from lectures 30-31. */
    private static class Counter {
        private int value;

        public synchronized void increment() { value++; }

        public synchronized int get() { return value; }
    }
}
