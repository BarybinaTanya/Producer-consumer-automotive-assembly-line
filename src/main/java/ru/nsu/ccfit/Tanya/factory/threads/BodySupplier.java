package ru.nsu.ccfit.Tanya.factory.threads;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.model.Body;
import ru.nsu.ccfit.Tanya.factory.model.Storage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supplier thread that manufactures {@link Body} parts at a controllable rate
 * and deposits them into the body warehouse.
 *
 * <p>The production delay is {@code volatile} so the GUI slider can change it
 * safely from the Swing Event Dispatch Thread without requiring synchronization.</p>
 */
public class BodySupplier extends Thread {

    private static final Logger logger = Logger.getLogger(BodySupplier.class);

    /** The warehouse to fill with freshly produced body parts. */
    private final Storage<Body> bodyStorage;

    /** How many bodies this supplier has produced in total. */
    private final AtomicInteger totalProduced = new AtomicInteger(0);

    /**
     * Sleep time between producing successive parts (milliseconds).
     * {@code volatile} so the GUI can update it without synchronization.
     */
    private volatile int delayMs;

    /**
     * Creates the body supplier.
     *
     * @param bodyStorage warehouse to supply bodies to
     * @param delayMs     initial production delay in milliseconds
     */
    public BodySupplier(Storage<Body> bodyStorage, int delayMs) {
        super("BodySupplier");
        this.bodyStorage = bodyStorage;
        this.delayMs     = delayMs;
        setDaemon(true);
    }

    /**
     * Adjusts the production speed.
     *
     * @param delayMs new delay between parts in milliseconds (≥ 50 recommended)
     */
    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }

    /** @return current delay in milliseconds */
    public int getDelayMs() { return delayMs; }

    /** @return total bodies produced since this thread started */
    public int getTotalProduced() { return totalProduced.get(); }

    /**
     * Production loop: sleep → create part → put in warehouse → repeat.
     */
    @Override
    public void run() {
        logger.info("BodySupplier started (initial delay: " + delayMs + " ms).");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(delayMs); // simulate manufacturing time

                Body body = new Body();
                bodyStorage.put(body);  // blocks if warehouse is full
                int count = totalProduced.incrementAndGet();
                logger.debug("BodySupplier: produced " + body + " | total=" + count);

            } catch (InterruptedException e) {
                // Restore the interrupt flag and exit the loop cleanly.
                Thread.currentThread().interrupt();
            }
        }

        logger.info("BodySupplier stopped. Total produced: " + totalProduced.get());
    }
}