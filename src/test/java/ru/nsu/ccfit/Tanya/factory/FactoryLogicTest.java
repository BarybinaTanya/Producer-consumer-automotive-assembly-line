package ru.nsu.ccfit.Tanya.factory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.nsu.ccfit.Tanya.factory.model.*;
import ru.nsu.ccfit.Tanya.factory.threads.AssemblyTask;
import ru.nsu.ccfit.Tanya.factory.threads.StockController;
import ru.nsu.ccfit.Tanya.threadpool.ThreadPool;

import static org.junit.Assert.*;

/**
 * Integration-style tests for the core factory assembly logic.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@link AssemblyTask} — a directly executed task assembles a car correctly.</li>
 *   <li>{@link StockController} — it submits tasks to fill the warehouse when stock is low.</li>
 * </ul>
 */
public class FactoryLogicTest {

    private Storage<Body>      bodyStorage;
    private Storage<Engine>    engineStorage;
    private Storage<Accessory> accessoryStorage;
    private Storage<Car>       carStorage;
    private ThreadPool         threadPool;

    @Before
    public void setUp() {
        bodyStorage      = new Storage<>(10, "TestBody");
        engineStorage    = new Storage<>(10, "TestEngine");
        accessoryStorage = new Storage<>(10, "TestAccessory");
        carStorage       = new Storage<>(10, "TestCar");
        threadPool       = new ThreadPool(2);
    }

    @After
    public void tearDown() {
        threadPool.shutdown();
    }

    /**
     * Directly executing one AssemblyTask (bypassing the ThreadPool) should
     * consume one part from each warehouse and add one car to the car storage.
     */
    @Test(timeout = 5_000)
    public void testAssemblyTaskProducesOneCar() throws InterruptedException {
        // Pre-fill part warehouses with exactly one of each.
        bodyStorage.put(new Body());
        engineStorage.put(new Engine());
        accessoryStorage.put(new Accessory());

        AssemblyTask task = new AssemblyTask(bodyStorage, engineStorage, accessoryStorage, carStorage);
        task.execute(); // direct call, no thread pool

        assertEquals("One car should have been assembled", 1, carStorage.getCount());
        assertEquals("Body warehouse should be empty", 0, bodyStorage.getCount());
        assertEquals("Engine warehouse should be empty", 0, engineStorage.getCount());
        assertEquals("Accessory warehouse should be empty", 0, accessoryStorage.getCount());
    }

    /**
     * The assembled car must hold the correct parts (IDs must match the parts
     * that were in the warehouses).
     */
    @Test(timeout = 5_000)
    public void testAssembledCarHasCorrectParts() throws InterruptedException {
        Body      body      = new Body();
        Engine    engine    = new Engine();
        Accessory accessory = new Accessory();

        bodyStorage.put(body);
        engineStorage.put(engine);
        accessoryStorage.put(accessory);

        new AssemblyTask(bodyStorage, engineStorage, accessoryStorage, carStorage).execute();

        Car car = carStorage.take();
        assertNotNull("A car should have been produced", car);
        assertEquals("Car's body ID must match", body.getId(), car.getBody().getId());
        assertEquals("Car's engine ID must match", engine.getId(), car.getEngine().getId());
        assertEquals("Car's accessory ID must match", accessory.getId(), car.getAccessory().getId());
    }

    /**
     * After the StockController starts with an empty car storage (0 < capacity/2),
     * it should submit assembly tasks.  Given enough parts, cars should appear in
     * the warehouse within a few seconds.
     */
    @Test(timeout = 10_000)
    public void testStockControllerTriggersAssembly() throws InterruptedException {
        // Small capacities to keep the test fast.
        Storage<Body>      bs = new Storage<>(4, "TestBody");
        Storage<Engine>    es = new Storage<>(4, "TestEngine");
        Storage<Accessory> as = new Storage<>(4, "TestAccessory");
        Storage<Car>       cs = new Storage<>(4, "TestCar");

        // Fill all part warehouses so assembly tasks can complete immediately.
        for (int i = 0; i < 4; i++) {
            bs.put(new Body());
            es.put(new Engine());
            as.put(new Accessory());
        }

        ThreadPool tp = new ThreadPool(2);
        StockController controller = new StockController(cs, bs, es, as, tp);
        controller.start();

        // Wait for the controller to react and workers to assemble cars.
        long deadline = System.currentTimeMillis() + 8_000;
        while (cs.getCount() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        controller.interrupt();
        tp.shutdown();

        assertTrue("StockController should have triggered at least one car assembly",
                cs.getCount() > 0);
    }

    /**
     * An AssemblyTask submitted via the ThreadPool (not called directly) must also
     * produce a car in the warehouse.
     */
    @Test(timeout = 8_000)
    public void testAssemblyTaskViaThreadPool() throws InterruptedException {
        bodyStorage.put(new Body());
        engineStorage.put(new Engine());
        accessoryStorage.put(new Accessory());

        threadPool.submit(new AssemblyTask(bodyStorage, engineStorage, accessoryStorage, carStorage));

        // Wait for the worker to pick up and execute the task.
        long deadline = System.currentTimeMillis() + 6_000;
        while (carStorage.getCount() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        assertEquals("ThreadPool should have executed the assembly task",
                1, carStorage.getCount());
    }
}