package ru.nsu.ccfit.Tanya.factory.threads;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.model.Engine;
import ru.nsu.ccfit.Tanya.factory.model.Storage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supplier thread that manufactures {@link Engine} parts at a controllable rate
 * and deposits them into the engine warehouse.
 *
 * <p>Identical in structure to {@link BodySupplier} — refer to that class for
 * detailed design comments.</p>
 */
public class EngineSupplier extends Thread {

    private static final Logger logger = Logger.getLogger(EngineSupplier.class);

    private final Storage<Engine> engineStorage;
    private final AtomicInteger   totalProduced = new AtomicInteger(0);
    private volatile int          delayMs;

    /**
     * Creates the engine supplier.
     *
     * @param engineStorage warehouse to supply engines to
     * @param delayMs       initial production delay in milliseconds
     */
    public EngineSupplier(Storage<Engine> engineStorage, int delayMs) {
        super("EngineSupplier");
        this.engineStorage = engineStorage;
        this.delayMs       = delayMs;
        setDaemon(true);
    }

    /** Updates the production speed. @param delayMs new delay in ms */
    public void setDelayMs(int delayMs) { this.delayMs = delayMs; }

    /** @return current production delay in ms */
    public int getDelayMs() { return delayMs; }

    /** @return total engines produced since this thread started */
    public int getTotalProduced() { return totalProduced.get(); }

    /** Production loop: sleep → create part → put in warehouse → repeat. */
    @Override
    public void run() {
        logger.info("EngineSupplier started (initial delay: " + delayMs + " ms).");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(delayMs);

                Engine engine = new Engine();
                engineStorage.put(engine);
                int count = totalProduced.incrementAndGet();
                logger.debug("EngineSupplier: produced " + engine + " | total=" + count);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("EngineSupplier stopped. Total produced: " + totalProduced.get());
    }
}