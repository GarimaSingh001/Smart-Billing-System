package payment;

import java.math.BigDecimal;

/** Common contract for prototype payment methods; no stock is changed here. */
public interface Payment {
    /** Attempts payment; returns false for a null or negative amount. */
    boolean pay(BigDecimal amount);

    /** Returns the payment mode saved on the invoice. */
    String getMode();
}
