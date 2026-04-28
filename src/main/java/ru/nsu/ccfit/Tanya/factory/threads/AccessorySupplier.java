package ru.nsu.ccfit.Tanya.factory.threads;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.model.Accessory;
import ru.nsu.ccfit.Tanya.factory.model.Storage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supplier thread that manufactures {@link Accessory} parts at a controllable rate
 * and deposits them into the accessory warehouse.
 *
 * <p>The factory creates multiple AccessorySupplier instances (configured via
 * {@code AccessorySuppliers} in the config file).  A <em>static</em>
 * {@code totalProducedAll} counter tracks the combined output of all instances,
 * which is what the GUI displays.</p>
 *
 * <p>The per-instance delay is controlled by {@link #setDelayMs(int)}.
 * All instances share the same slider in the GUI (same delay value is set on all).</p>
 */
public class AccessorySupplier extends Thread {

    private static final Logger logger = Logger.getLogger(AccessorySupplier.class);

    /**
     * Shared counter for ALL accessory supplier instances combined.
     * The GUI shows this aggregated number.
     */
    private static final AtomicInteger totalProducedAll = new AtomicInteger(0);

    private final int              supplierIndex;  // for distinguishing threads in logs
    private final Storage<Accessory> accessoryStorage;
    private volatile int           delayMs;

    /**
     * Creates one accessory supplier thread.
     *
     * @param supplierIndex   1-based index, used only in log messages
     * @param accessoryStorage warehouse to supply accessories to
     * @param delayMs          initial production delay in milliseconds
     */
    public AccessorySupplier(int supplierIndex, Storage<Accessory> accessoryStorage, int delayMs) {
        super("AccessorySupplier-" + supplierIndex);
        this.supplierIndex    = supplierIndex;
        this.accessoryStorage = accessoryStorage;
        this.delayMs          = delayMs;
        setDaemon(true);
    }

    /** Updates the production speed for this instance. @param delayMs new delay in ms */
    public void setDelayMs(int delayMs) { this.delayMs = delayMs; }

    /** @return current production delay in ms */
    public int getDelayMs() { return delayMs; }

    /**
     * Returns the combined total accessories produced by <em>all</em> supplier instances.
     *
     * @return total accessories produced
     */
    public static int getTotalProducedAll() { return totalProducedAll.get(); }

    /** Production loop: sleep → create part → put in warehouse → repeat. */
    @Override
    public void run() {
        logger.info("AccessorySupplier-" + supplierIndex + " started (delay: " + delayMs + " ms).");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(delayMs);

                Accessory accessory = new Accessory();
                accessoryStorage.put(accessory);
                int allCount = totalProducedAll.incrementAndGet();
                logger.debug("AccessorySupplier-" + supplierIndex
                        + ": produced " + accessory + " | total-all=" + allCount);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("AccessorySupplier-" + supplierIndex + " stopped.");
    }
}