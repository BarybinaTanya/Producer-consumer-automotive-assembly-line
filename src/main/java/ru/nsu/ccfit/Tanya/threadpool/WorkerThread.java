package ru.nsu.ccfit.Tanya.threadpool;

import org.apache.log4j.Logger;

/**
 * A single worker thread that lives inside a {@link ThreadPool}.
 *
 * <p>Its only job is to loop forever: ask the pool for the next {@link Task},
 * run it, then ask again.  It stops when the pool is shut down and the queue
 * is empty, or when it is interrupted.</p>
 *
 * <p>Daemon flag is set to {@code true} so that the JVM can exit normally
 * even if some workers are still blocked inside {@link ThreadPool#takeTask()}.</p>
 */
public class WorkerThread extends Thread {

    private static final Logger logger = Logger.getLogger(WorkerThread.class);

    /** The pool this worker belongs to — used to fetch tasks. */
    private final ThreadPool pool;

    /**
     * Creates a new worker thread associated with the given pool.
     *
     * @param pool the pool to draw tasks from
     * @param name a human-readable name used in logs and thread dumps
     */
    public WorkerThread(ThreadPool pool, String name) {
        super(name);
        this.pool = pool;
        // Daemon threads do not prevent the JVM from shutting down.
        setDaemon(true);
    }

    /**
     * Main execution loop.
     *
     * <ol>
     *   <li>Asks the pool for the next task (blocks if the queue is empty).</li>
     *   <li>Executes the task.</li>
     *   <li>Repeats until interrupted or pool is shut down with an empty queue.</li>
     * </ol>
     */
    @Override
    public void run() {
        logger.debug(getName() + " started.");

        // Keep looping until interrupted or the pool signals us to stop.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Blocks here when queue is empty; returns null on shutdown.
                Task task = pool.takeTask();

                if (task == null) {
                    // Pool is shutting down and queue is empty — time to exit.
                    logger.debug(getName() + ": received shutdown signal (null task).");
                    break;
                }

                logger.debug(getName() + ": executing task " + task.getClass().getSimpleName());
                task.execute();

            } catch (InterruptedException e) {
                // Restore the interrupt flag so the while-condition check can see it.
                Thread.currentThread().interrupt();
                logger.debug(getName() + " interrupted while waiting for a task.");
                break;
            }
        }

        logger.debug(getName() + " stopped.");
    }
}