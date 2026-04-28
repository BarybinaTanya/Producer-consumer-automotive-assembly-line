package ru.nsu.ccfit.Tanya.threadpool;

import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A fixed-size thread pool implemented from scratch using only {@code synchronized},
 * {@code wait()}, and {@code notifyAll()}.
 *
 * <p><strong>No {@code java.util.concurrent} classes are used.</strong></p>
 *
 * <ol>
 *   <li>On creation, {@code threadCount} {@link WorkerThread}s are started.
 *       They immediately block in {@link #takeTask()} because the queue is empty.</li>
 *   <li>A caller submits a {@link Task} as a {@link #submit(Task)}.
 *       The task is added to the queue and {@code notifyAll()} wakes a worker.</li>
 *   <li>A worker picks the task, executes it, then loops back to wait.</li>
 *   <li>On {@link #shutdown()}, the shutdown flag is set and all workers are woken
 *       so they can notice the flag and exit cleanly.</li>
 * </ol>
 */
public class ThreadPool {

    private static final Logger logger = Logger.getLogger(ThreadPool.class);

    /** Queue of pending tasks. Access is guarded by {@code this}. */
    private final Queue<Task> taskQueue;

    /** The worker threads managed by this pool. */
    private final WorkerThread[] workers;

    /**
     * Shutdown flag. Volatile so that reads outside synchronized blocks are safe.
     * Once set to {@code true} it is never reset.
     */
    private volatile boolean shutdown;

    /**
     * Creates and starts a pool with the given number of worker threads.
     *
     * @param threadCount number of worker threads (must be ≥ 1)
     * @throws IllegalArgumentException if {@code threadCount < 1}
     */
    public ThreadPool(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount must be at least 1, got: " + threadCount);
        }

        this.taskQueue = new LinkedList<>();
        this.workers   = new WorkerThread[threadCount];
        this.shutdown  = false;

        // Create and start each worker thread.
        for (int i = 0; i < threadCount; i++) {
            workers[i] = new WorkerThread(this, "PoolWorker-" + i);
            workers[i].start();
        }

        logger.info("ThreadPool started with " + threadCount + " worker thread(s).");
    }

    /**
     * Submits a task for asynchronous execution.
     *
     * <p>The task is appended to the internal queue and one waiting worker is notified.</p>
     *
     * @param task the task to run; must not be {@code null}
     * @throws IllegalStateException if the pool has already been shut down
     * @throws NullPointerException  if {@code task} is {@code null}
     */
    public synchronized void submit(Task task) {
        if (task == null) {
            throw new NullPointerException("task must not be null");
        }
        if (shutdown) {
            throw new IllegalStateException("Cannot submit tasks to a shut-down ThreadPool.");
        }

        taskQueue.offer(task);
        logger.debug("Task submitted. Pending queue size: " + taskQueue.size());

        // Wake up one waiting worker.  We use notifyAll() so all workers re-evaluate
        // their condition, which is safer against spurious-wakeup scenarios.
        notifyAll();
    }

    /**
     * Blocks the calling thread until a task is available, then returns and removes it.
     *
     * <p>Returns {@code null} when the pool is shut down <em>and</em> the queue is empty,
     * signalling the calling {@link WorkerThread} to exit its loop.</p>
     *
     * <p><strong>IMPORTANT:</strong> The {@code while} loop (not {@code if}) is mandatory
     * to handle spurious wakeups correctly.</p>
     *
     * @return the next task, or {@code null} if shutting down with an empty queue
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    synchronized Task takeTask() throws InterruptedException {
        // while — NOT if — to guard against spurious wakeups.
        while (taskQueue.isEmpty() && !shutdown) {
            wait(); // releases the lock and suspends the thread
        }

        // If we woke up because of shutdown and the queue is now empty, signal the worker to stop.
        if (taskQueue.isEmpty()) {
            return null;
        }

        return taskQueue.poll();
    }

    /**
     * Returns the current number of tasks waiting in the queue (not yet picked up by a worker).
     *
     * @return pending task count (≥ 0)
     */
    public synchronized int getQueueSize() {
        return taskQueue.size();
    }

    /**
     * Returns {@code true} if {@link #shutdown()} has been called.
     *
     * @return shutdown state
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Shuts down the pool.
     *
     * <ol>
     *   <li>Sets the shutdown flag so no new tasks can be submitted.</li>
     *   <li>Wakes all waiting workers so they notice the flag and exit.</li>
     *   <li>Interrupts each worker and waits up to 2 s for it to finish.</li>
     * </ol>
     */
    public void shutdown() {
        // Step 1: set the flag and wake everyone while holding the lock.
        synchronized (this) {
            if (shutdown) {
                return; // already shut down — idempotent
            }
            shutdown = true;
            notifyAll();
        }

        // Step 2: interrupt workers and join them outside the synchronized block
        //         to avoid holding the lock during potentially long joins.
        for (WorkerThread worker : workers) {
            worker.interrupt();
            try {
                worker.join(2_000); // wait at most 2 seconds per worker
            } catch (InterruptedException e) {
                // Restore the flag so the caller can handle it.
                Thread.currentThread().interrupt();
            }
        }

        logger.info("ThreadPool shut down. " + taskQueue.size() + " task(s) left unexecuted.");
    }
}