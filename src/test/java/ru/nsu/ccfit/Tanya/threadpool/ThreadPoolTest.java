package ru.nsu.ccfit.Tanya.threadpool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ThreadPool}.
 *
 * <p>We verify:
 * <ul>
 *   <li>Tasks are executed.</li>
 *   <li>All submitted tasks eventually run.</li>
 *   <li>The queue size increases when tasks are submitted.</li>
 *   <li>Shutdown prevents further task submission.</li>
 *   <li>Constructor rejects invalid thread counts.</li>
 * </ul>
 */
public class ThreadPoolTest {

    private ThreadPool pool;

    @Before
    public void setUp() {
        // Use 2 workers for most tests.
        pool = new ThreadPool(2);
    }

    @After
    public void tearDown() {
        pool.shutdown();
    }

    /** A single task must be executed. */
    @Test(timeout = 5_000)
    public void testSingleTaskIsExecuted() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        pool.submit(counter::incrementAndGet);

        // Busy-wait with a sleep until the task completes (max 4 s).
        long deadline = System.currentTimeMillis() + 4_000;
        while (counter.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertEquals("Task should have been executed once", 1, counter.get());
    }

    /** All tasks submitted to the pool must eventually run. */
    @Test(timeout = 10_000)
    public void testAllTasksAreExecuted() throws InterruptedException {
        int taskCount = 20;
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            pool.submit(counter::incrementAndGet);
        }

        long deadline = System.currentTimeMillis() + 8_000;
        while (counter.get() < taskCount && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        assertEquals("All " + taskCount + " tasks should have run", taskCount, counter.get());
    }

    /** Submitting a task should increase the queue size (at least transiently). */
    @Test
    public void testQueueSizeAfterSubmit() throws InterruptedException {
        // Use a pool that can NEVER pick up tasks (0 workers would throw, so use a blocking task instead).
        // We create a separate pool with 1 worker but give it a blocking first task.
        ThreadPool slowPool = new ThreadPool(1);
        Object lock = new Object();

        // First task: block the single worker thread indefinitely.
        slowPool.submit(() -> {
            synchronized (lock) {
                try { lock.wait(3_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        Thread.sleep(100); // give the worker time to pick up the blocking task

        // Now submit a second task — it must sit in the queue.
        slowPool.submit(() -> { /* no-op */ });

        assertEquals("Queue should contain the pending no-op task", 1, slowPool.getQueueSize());

        // Release the blocked worker and clean up.
        synchronized (lock) { lock.notifyAll(); }
        slowPool.shutdown();
    }

    /** Submitting to a shut-down pool must throw IllegalStateException. */
    @Test(expected = IllegalStateException.class)
    public void testSubmitAfterShutdownThrows() {
        pool.shutdown();
        pool.submit(() -> { /* should not run */ });
    }

    /** isShutdown() must return true after shutdown(). */
    @Test
    public void testIsShutdownAfterShutdown() {
        assertFalse("Pool should not be shut down initially", pool.isShutdown());
        pool.shutdown();
        assertTrue("Pool should be shut down after calling shutdown()", pool.isShutdown());
    }

    /** Constructor with zero threads must throw. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroThreads() {
        new ThreadPool(0);
    }

    /** Submitting null task must throw NullPointerException. */
    @Test(expected = NullPointerException.class)
    public void testSubmitNullThrows() {
        pool.submit(null);
    }
}