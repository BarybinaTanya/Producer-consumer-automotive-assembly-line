package ru.nsu.ccfit.Tanya.factory.threads;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.model.*;
import ru.nsu.ccfit.Tanya.threadpool.Task;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Task} that assembles one {@link Car} from parts taken from the warehouses.
 *
 * <p>This is the "assembly order" submitted to the {@link ru.nsu.ccfit.Tanya.threadpool.ThreadPool}
 * by the {@link StockController}.  A {@link ru.nsu.ccfit.Tanya.threadpool.WorkerThread}
 * picks it up and calls {@link #execute()}.</p>
 *
 * <h3>Steps</h3>
 * <ol>
 *   <li>Take one {@link Body} from the body warehouse (blocks if empty).</li>
 *   <li>Take one {@link Engine} from the engine warehouse (blocks if empty).</li>
 *   <li>Take one {@link Accessory} from the accessory warehouse (blocks if empty).</li>
 *   <li>Create a new {@link Car} object from those three parts.</li>
 *   <li>Put the car in the finished-car warehouse (blocks if full).</li>
 * </ol>
 *
 * <p>If the thread is interrupted at any point, the interrupt flag is restored
 * and the method returns without completing assembly.</p>
 */
public class AssemblyTask implements Task {

    private static final Logger logger = Logger.getLogger(AssemblyTask.class);

    /**
     * Shared counter tracking how many cars have been successfully assembled
     * across <em>all</em> AssemblyTask instances.
     * {@link AtomicInteger} makes increments thread-safe without {@code synchronized}.
     */
    private static final AtomicInteger totalCarsProduced = new AtomicInteger(0);

    private final Storage<Body>      bodyStorage;
    private final Storage<Engine>    engineStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Car>       carStorage;

    /**
     * Creates an assembly task with references to all four warehouses.
     *
     * @param bodyStorage      warehouse to take a body from
     * @param engineStorage    warehouse to take an engine from
     * @param accessoryStorage warehouse to take an accessory from
     * @param carStorage       warehouse to deliver the assembled car to
     */
    public AssemblyTask(Storage<Body>      bodyStorage,
                        Storage<Engine>    engineStorage,
                        Storage<Accessory> accessoryStorage,
                        Storage<Car>       carStorage) {
        this.bodyStorage      = bodyStorage;
        this.engineStorage    = engineStorage;
        this.accessoryStorage = accessoryStorage;
        this.carStorage       = carStorage;
    }

    /**
     * Executes the assembly sequence.
     *
     * <p>Calls to {@link Storage#take()} and {@link Storage#put(Object)} block
     * automatically when warehouses are empty or full — no busy-waiting here.</p>
     */
    @Override
    public void execute() {
        try {
            // --- Step 1-3: take one of each part (each call may block) ---
            Body      body      = bodyStorage.take();
            Engine    engine    = engineStorage.take();
            Accessory accessory = accessoryStorage.take();

            // --- Step 4: assemble the car ---
            Car car = new Car(body, engine, accessory);
            int total = totalCarsProduced.incrementAndGet();
            logger.info("Assembled " + car + " | Total produced so far: " + total);

            // --- Step 5: deliver to finished-car warehouse (may block if full) ---
            carStorage.put(car);

        } catch (InterruptedException e) {
            // Restore the interrupt flag so the WorkerThread's loop condition can detect it.
            Thread.currentThread().interrupt();
            logger.warn(Thread.currentThread().getName()
                    + ": AssemblyTask interrupted — car assembly aborted.");
        }
    }

    /**
     * Returns the total number of cars successfully assembled since the JVM started.
     *
     * @return total assembled car count
     */
    public static int getTotalCarsProduced() {
        return totalCarsProduced.get();
    }
}