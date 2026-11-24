package util;

/**
 * Background daemon thread that marks a login session as expired after a period without activity.
 * The console thread calls touch() on every input and checks isExpired() before trusting the input.
 */
public class SessionMonitor implements Runnable {

    private final long timeoutNanos;
    private final long pollMillis;
    private long lastActivity = System.nanoTime();
    private boolean expired;
    private boolean stopped;
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
    public void stop() {
        Thread toStop;
        synchronized (this) {
            stopped = true;
            toStop = thread;
        }
        if (toStop == null) { return; }
        toStop.interrupt();
        try {
            // Do not hold this object's lock while joining the worker.
            toStop.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Records user activity; has no effect once the session has expired. */
    public synchronized void touch() {
        if (!expired && !stopped) {
            lastActivity = System.nanoTime();
        }
    }

    /** Returns true when the session timed out. */
    public synchronized boolean isExpired() {
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
                synchronized (this) {
                    if (stopped) { return; }
                    if (System.nanoTime() - lastActivity >= timeoutNanos) {
                        expired = true;
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
