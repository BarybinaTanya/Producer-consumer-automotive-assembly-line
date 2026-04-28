package ru.nsu.ccfit.Tanya.factory.threads;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.model.*;
import ru.nsu.ccfit.Tanya.threadpool.ThreadPool;

/**
 * The StockController is a dedicated monitoring thread for the finished-car warehouse.
 *
 * <h3>Responsibility</h3>
 * <p>It wakes up every time a car is <em>removed</em> from the warehouse
 * (because {@link Storage#take()} calls {@code notifyAll()} on the storage's monitor).
 * It then checks whether the stock level dropped below half of the warehouse capacity.
 * If so, it submits enough {@link AssemblyTask}s to the {@link ThreadPool} to fill the
 * warehouse back up to capacity.</p>
 *
 * <h3>Why this design?</h3>
 * <ul>
 *   <li>The controller does not poll on a timer — it reacts to events.</li>
 *   <li>It accounts for already-queued tasks to avoid over-scheduling.</li>
 *   <li>All waiting is done via {@code synchronized} + {@code wait()} — no busy-loops.</li>
 * </ul>
 */
public class StockController extends Thread {

    private static final Logger logger = Logger.getLogger(StockController.class);

    private final Storage<Car>       carStorage;
    private final Storage<Body>      bodyStorage;
    private final Storage<Engine>    engineStorage;
    private final Storage<Accessory> accessoryStorage;
    private final ThreadPool         threadPool;

    /**
     * Creates the stock controller.
     *
     * @param carStorage       finished-car warehouse to monitor
     * @param bodyStorage      body warehouse (passed to new AssemblyTasks)
     * @param engineStorage    engine warehouse (passed to new AssemblyTasks)
     * @param accessoryStorage accessory warehouse (passed to new AssemblyTasks)
     * @param threadPool       pool to submit assembly tasks to
     */
    public StockController(Storage<Car>       carStorage,
                           Storage<Body>      bodyStorage,
                           Storage<Engine>    engineStorage,
                           Storage<Accessory> accessoryStorage,
                           ThreadPool         threadPool) {
        super("StockController");
        this.carStorage       = carStorage;
        this.bodyStorage      = bodyStorage;
        this.engineStorage    = engineStorage;
        this.accessoryStorage = accessoryStorage;
        this.threadPool       = threadPool;
        setDaemon(true);
    }

    /**
     * Main monitoring loop.
     *
     * <ol>
     *   <li>Acquires the lock on {@code carStorage}.</li>
     *   <li>Waits (using {@code while + wait()}) until the car count drops below
     *       half of the warehouse capacity.</li>
     *   <li>Releases the lock and submits assembly tasks to refill the warehouse,
     *       taking into account tasks already queued in the pool.</li>
     * </ol>
     */
    @Override
    public void run() {
        logger.info("StockController started. Will monitor: " + carStorage.getName());

        // The trigger threshold: submit tasks when stock drops below this.
        final int threshold = carStorage.getCapacity() / 2;

        while (!Thread.currentThread().isInterrupted()) {

            // --- Phase 1: wait until the stock is below the threshold ---
            synchronized (carStorage) {
                // while loop guards against spurious wakeups.
                while (!Thread.currentThread().isInterrupted()
                        && carStorage.getCount() >= threshold) {
                    try {
                        // wait() atomically releases the lock on carStorage
                        // and suspends this thread until notifyAll() is called
                        // by Storage.take() or Storage.put().
                        carStorage.wait();
                    } catch (InterruptedException e) {
                        // Restore flag and let the outer while condition exit cleanly.
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            // We no longer hold the lock on carStorage here.

            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            // --- Phase 2: decide how many tasks to submit ---
            // We read the current count (lock is re-acquired briefly inside getCount()).
            int currentCars   = carStorage.getCount();
            int pendingTasks  = threadPool.getQueueSize();
            int capacity      = carStorage.getCapacity();

            // We want (currentCars + pendingTasks) to reach capacity.
            // This prevents submitting too many tasks when the queue is already full of work.
            int needed = capacity - currentCars - pendingTasks;

            if (needed > 0) {
                logger.info("StockController: cars=" + currentCars
                        + ", pending=" + pendingTasks
                        + ", capacity=" + capacity
                        + " → submitting " + needed + " assembly task(s).");

                for (int i = 0; i < needed; i++) {
                    if (threadPool.isShutdown()) {
                        break;
                    }
                    threadPool.submit(new AssemblyTask(
                            bodyStorage, engineStorage, accessoryStorage, carStorage));
                }
            }
        }

        logger.info("StockController stopped.");
    }
}