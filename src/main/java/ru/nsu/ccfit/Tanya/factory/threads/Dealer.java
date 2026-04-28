package ru.nsu.ccfit.Tanya.factory.threads;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.SaleLogger;
import ru.nsu.ccfit.Tanya.factory.model.Car;
import ru.nsu.ccfit.Tanya.factory.model.Storage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A dealer thread that periodically requests one car from the finished-car warehouse
 * and logs the sale.
 *
 * <p>Each dealer has its own 1-based number used in log messages and in the
 * {@link SaleLogger} sale records.  Multiple dealer threads share the same
 * {@link Storage}&lt;{@link Car}&gt; and the same {@link SaleLogger}
 * (which is thread-safe).</p>
 *
 * <p>The request rate is controlled by {@code delayMs}: the dealer sleeps for
 * that many milliseconds between consecutive purchases.  The GUI slider adjusts
 * this value on all dealers simultaneously.</p>
 */
public class Dealer extends Thread {

    private static final Logger logger = Logger.getLogger(Dealer.class);

    /** Combined counter of all cars sold across all Dealer instances. */
    private static final AtomicInteger totalSold = new AtomicInteger(0);

    /** 1-based identifier of this dealer (used in log output). */
    private final int dealerNumber;

    /** Finished-car warehouse to buy from. */
    private final Storage<Car> carStorage;

    /** Sales logger (may be a no-op if LogSale=false). */
    private final SaleLogger saleLogger;

    /**
     * Milliseconds to wait between purchases.
     * {@code volatile} so the GUI can update it safely from the EDT.
     */
    private volatile int delayMs;

    /**
     * Creates one dealer thread.
     *
     * @param dealerNumber 1-based dealer identifier
     * @param carStorage   finished-car warehouse to purchase from
     * @param saleLogger   logger for recording sales
     * @param delayMs      initial delay between purchases in milliseconds
     */
    public Dealer(int dealerNumber, Storage<Car> carStorage, SaleLogger saleLogger, int delayMs) {
        super("Dealer-" + dealerNumber);
        this.dealerNumber = dealerNumber;
        this.carStorage   = carStorage;
        this.saleLogger   = saleLogger;
        this.delayMs      = delayMs;
        setDaemon(true);
    }

    /** Adjusts the purchase rate. @param delayMs new delay in milliseconds */
    public void setDelayMs(int delayMs) { this.delayMs = delayMs; }

    /** @return current delay in ms */
    public int getDelayMs() { return delayMs; }

    /**
     * Returns the total number of cars sold by ALL dealer instances combined.
     *
     * @return total cars sold
     */
    public static int getTotalSold() { return totalSold.get(); }

    /**
     * Purchase loop: sleep → take car from warehouse → log the sale → repeat.
     */
    @Override
    public void run() {
        logger.info("Dealer-" + dealerNumber + " started (initial delay: " + delayMs + " ms).");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(delayMs);   // simulate time between visits to the warehouse

                // This blocks if the car warehouse is empty — correct per spec.
                Car car = carStorage.take();

                int sold = totalSold.incrementAndGet();
                logger.info("Dealer-" + dealerNumber + " purchased " + car
                        + " | Total sold: " + sold);

                // Write the sale record to factory_sales.log (if enabled).
                saleLogger.logSale(dealerNumber, car);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("Dealer-" + dealerNumber + " stopped.");
    }
}