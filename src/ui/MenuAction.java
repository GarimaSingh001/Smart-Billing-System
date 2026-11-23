package ui;

/** One menu operation that may fail with any checked exception; run through MenuHandler.execute. */
@FunctionalInterface
public interface MenuAction {

    /** Performs the operation. */
    void run() throws Exception;
}
