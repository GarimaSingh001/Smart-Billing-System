package util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Background daemon thread that marks a login session as expired after a period without activity.
 * The console thread calls touch() on every input and checks isExpired() before trusting the input.
 */
public class SessionMonitor implements Runnable {

    private final long timeoutNanos;
    private final long pollMillis;
    private final AtomicLong lastActivity = new AtomicLong(System.nanoTime());
    private volatile boolean expired;
    private Thread thread;

    /** Creates a monitor with an idle timeout and a polling interval, both in milliseconds. */
    public SessionMonitor(long timeoutMillis, long pollMillis) {
        if (timeoutMillis <= 0 || pollMillis <= 0) {
            throw new IllegalArgumentException("Timeout and poll interval must be positive");
        }
        this.timeoutNanos = timeoutMillis * 1_000_000L;
        this.pollMillis = pollMillis;
    }

    /** Starts the daemon thread (a daemon never blocks JVM shutdown). */
    public synchronized void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this, "session-monitor");
        thread.setDaemon(true);
        thread.start();
    }

    /** Stops the thread by interrupting it and waits briefly for it to finish (join). */
    public synchronized void stop() {
        if (thread == null) {
            return;
        }
        thread.interrupt();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Records user activity; has no effect once the session has expired. */
    public void touch() {
        if (!expired) {
            lastActivity.set(System.nanoTime());
        }
    }

    /** Returns true when the session timed out. */
    public boolean isExpired() {
        return expired;
    }

    /** Returns the state of the monitor thread (NEW, RUNNABLE, TIMED_WAITING, TERMINATED) for demos and tests. */
    public synchronized Thread.State getThreadState() {
        return thread == null ? Thread.State.NEW : thread.getState();
    }

    /** Polls until the idle time exceeds the timeout or the thread is interrupted. */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(pollMillis);
                if (System.nanoTime() - lastActivity.get() >= timeoutNanos) {
                    expired = true;
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
