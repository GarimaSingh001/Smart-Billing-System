package service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/** Thread-safe, bounded in-memory log of security-relevant events (logins, lockouts, user changes). */
public class AuditLog {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final int capacity;
    private final Deque<String> entries = new ArrayDeque<>();

    /** Creates a log that keeps the latest 100 events. */
    public AuditLog() {
        this(100);
    }

    /** Creates a log that keeps at most the given number of events. */
    public AuditLog(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        this.capacity = capacity;
    }

    /** Records one event; the oldest event is dropped when the log is full. */
    public synchronized void record(String actor, String action) {
        if (entries.size() == capacity) {
            entries.removeFirst();
        }
        entries.addLast(String.format("%s | %-10s | %s", LocalDateTime.now().format(FORMAT), actor, action));
    }

    /** Returns up to the given number of newest events, newest first. */
    public synchronized List<String> getRecent(int count) {
        List<String> result = new ArrayList<>();
        Iterator<String> newestFirst = entries.descendingIterator();
        while (newestFirst.hasNext() && result.size() < count) {
            result.add(newestFirst.next());
        }
        return result;
    }

    /** Returns how many events are currently stored. */
    public synchronized int size() {
        return entries.size();
    }
}
