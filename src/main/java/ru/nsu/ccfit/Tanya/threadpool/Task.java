package ru.nsu.ccfit.Tanya.threadpool;

/**
 * Represents one abstract unit of work that can be submitted to a {@link ThreadPool}.
 *
 * <p>The ThreadPool is completely decoupled from the factory — it knows nothing about
 * cars, parts, or assembly. It just knows how to run {@code Task} objects.</p>
 *
 * <p>Implementations should handle {@link InterruptedException} internally by restoring
 * the interrupt flag: {@code Thread.currentThread().interrupt();}.</p>
 */
public interface Task {

    /**
     * Executes this task.
     * Called by a {@link WorkerThread} that picked it from the pool's queue.
     */
    void execute();
}