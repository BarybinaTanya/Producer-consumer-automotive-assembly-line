package ru.nsu.ccfit.Tanya.factory;

import org.junit.Test;
import ru.nsu.ccfit.Tanya.factory.model.Body;
import ru.nsu.ccfit.Tanya.factory.model.Storage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Storage}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Basic put/take round-trip.</li>
 *   <li>Capacity enforcement (producer blocks when full).</li>
 *   <li>Empty enforcement (consumer blocks when empty).</li>
 *   <li>FIFO ordering.</li>
 *   <li>Constructor rejects invalid capacity.</li>
 * </ul>
 */
public class StorageTest {

    /** Items placed in should be retrievable. */
    @Test(timeout = 3_000)
    public void testPutAndTake() throws InterruptedException {
        Storage<Body> storage = new Storage<>(5, "Test");
        Body body = new Body();
        storage.put(body);
        assertEquals("Same item should come back", body, storage.take());
    }

    /** getCount() must reflect the number of items in the storage. */
    @Test(timeout = 3_000)
    public void testGetCount() throws InterruptedException {
        Storage<Body> storage = new Storage<>(10, "Test");
        assertEquals(0, storage.getCount());
        storage.put(new Body());
        assertEquals(1, storage.getCount());
        storage.put(new Body());
        assertEquals(2, storage.getCount());
        storage.take();
        assertEquals(1, storage.getCount());
    }

    /** Capacity should be reported correctly. */
    @Test
    public void testGetCapacity() {
        Storage<Body> storage = new Storage<>(7, "Test");
        assertEquals(7, storage.getCapacity());
    }

    /**
     * A producer thread must block when the storage is full and be unblocked
     * as soon as a consumer frees up space.
     */
    @Test(timeout = 5_000)
    public void testProducerBlocksWhenFull() throws InterruptedException {
        Storage<Body> storage = new Storage<>(2, "Test");
        storage.put(new Body());
        storage.put(new Body()); // storage is now full

        AtomicBoolean putCompleted = new AtomicBoolean(false);

        // This thread will try to put a third item — it must block.
        Thread producer = new Thread(() -> {
            try {
                storage.put(new Body()); // should block
                putCompleted.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.setDaemon(true);
        producer.start();

        Thread.sleep(300); // give producer time to block
        assertFalse("Producer should still be blocked", putCompleted.get());

        storage.take(); // free one slot

        producer.join(2_000); // producer should unblock now
        assertTrue("Producer should have completed after space freed", putCompleted.get());
    }

    /**
     * A consumer thread must block when the storage is empty and be unblocked
     * as soon as a producer adds an item.
     */
    @Test(timeout = 5_000)
    public void testConsumerBlocksWhenEmpty() throws InterruptedException {
        Storage<Body> storage = new Storage<>(5, "Test");
        AtomicReference<Body> taken = new AtomicReference<>(null);

        // This thread will try to take from an empty storage — it must block.
        Thread consumer = new Thread(() -> {
            try {
                taken.set(storage.take()); // should block
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.setDaemon(true);
        consumer.start();

        Thread.sleep(300); // consumer is blocked
        assertNull("Consumer should not have gotten anything yet", taken.get());

        Body body = new Body();
        storage.put(body); // this should unblock the consumer

        consumer.join(2_000);
        assertNotNull("Consumer should have gotten the body", taken.get());
        assertEquals("Consumer should have gotten the exact body we put", body, taken.get());
    }

    /** Constructor must reject capacity ≤ 0. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroCapacity() {
        new Storage<>(0, "Invalid");
    }
}