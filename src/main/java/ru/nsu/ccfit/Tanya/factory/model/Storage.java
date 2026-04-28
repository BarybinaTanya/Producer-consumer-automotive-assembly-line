package ru.nsu.ccfit.Tanya.factory.model;

import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A generic, thread-safe, bounded warehouse (storage) for objects of type {@code T}.
 *
 * <h3>Blocking semantics</h3>
 * <ul>
 *   <li>{@link #put(Object)} blocks the calling thread if the storage is <em>full</em>.</li>
 *   <li>{@link #take()} blocks the calling thread if the storage is <em>empty</em>.</li>
 * </ul>
 *
 * <h3>Synchronization</h3>
 * All methods are {@code synchronized} on {@code this}.  Both {@code put} and
 * {@code take} call {@code notifyAll()} after modifying the queue so that:
 * <ul>
 *   <li>Waiting <em>producers</em> (in {@code put}) are woken when space frees up.</li>
 *   <li>Waiting <em>consumers</em> (in {@code take}) are woken when an item arrives.</li>
 *   <li>The {@link ru.nsu.ccfit.Tanya.factory.threads.StockController} (which waits
 *       on the car storage's monitor) is woken on every {@code take()}.</li>
 * </ul>
 *
 * <p><strong>No {@code java.util.concurrent} classes are used.</strong></p>
 *
 * @param <T> the type of items stored in this warehouse
 */
public class Storage<T> {

    private static final Logger logger = Logger.getLogger(Storage.class);

    /** Maximum number of items that can be stored at once. */
    private final int capacity;

    /** The actual items.  Access is always guarded by {@code synchronized(this)}. */
    private final Queue<T> items;

    /** Display name used in log messages (e.g., "BodyStorage"). */
    private final String name;

    /**
     * Creates a new storage with the given capacity and display name.
     *
     * @param capacity maximum number of items (must be ≥ 1)
     * @param name     label used in log messages
     */
    public Storage(int capacity, String name) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be ≥ 1, got: " + capacity);
        }
        this.capacity = capacity;
        this.items    = new LinkedList<>();
        this.name     = name;
    }

    /**
     * Adds one item to this storage, <strong>blocking</strong> if full.
     *
     * <p>Uses a {@code while} loop (never {@code if}) to re-check the condition
     * after every wakeup, guarding against spurious wakeups.</p>
     *
     * @param item the item to store; must not be {@code null}
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized void put(T item) throws InterruptedException {
        // IMPORTANT: while — not if — to handle spurious wakeups correctly.
        while (items.size() >= capacity) {
            logger.debug("[" + name + "] Full (" + items.size() + "/" + capacity + "). Producer waiting…");
            wait();
        }

        items.offer(item);
        logger.debug("[" + name + "] Added item. Count: " + items.size() + "/" + capacity);

        // Wake all threads waiting on this storage's monitor:
        // - Consumers waiting in take() because the storage was empty.
        // - StockController waiting because car count was above threshold.
        notifyAll();
    }

    /**
     * Removes and returns one item from this storage, <strong>blocking</strong> if empty.
     *
     * <p>Uses a {@code while} loop (never {@code if}) to re-check the condition
     * after every wakeup, guarding against spurious wakeups.</p>
     *
     * @return the retrieved item
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized T take() throws InterruptedException {
        // IMPORTANT: while — not if — to handle spurious wakeups correctly.
        while (items.isEmpty()) {
            logger.debug("[" + name + "] Empty. Consumer waiting…");
            wait();
        }

        T item = items.poll();
        logger.debug("[" + name + "] Took item. Count: " + items.size() + "/" + capacity);

        // Wake all threads waiting on this monitor:
        // - Producers waiting in put() because the storage was full.
        // - StockController waiting for the car count to drop.
        notifyAll();

        return item;
    }

    /**
     * Returns the current number of items in the storage without blocking.
     *
     * @return current count (0 … capacity)
     */
    public synchronized int getCount() {
        return items.size();
    }

    /**
     * Returns the maximum number of items this storage can hold.
     *
     * @return capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the human-readable name of this storage.
     *
     * @return storage name
     */
    public String getName() {
        return name;
    }
}