package ru.nsu.ccfit.Tanya.factory.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for every car part (body, engine, accessory).
 *
 * <p>Each part automatically receives a globally unique integer ID the moment
 * it is constructed.  We use an {@link AtomicInteger} so the ID assignment is
 * thread-safe without needing a {@code synchronized} block.</p>
 */
public abstract class Part {

    /**
     * Shared counter across ALL part subclasses — gives truly unique IDs
     * even across different part types.
     */
    private static final AtomicInteger globalIdCounter = new AtomicInteger(0);

    /** The unique identifier of this specific part instance. */
    private final int id;

    /**
     * Assigns a unique ID to this part.
     * Called automatically by every subclass constructor via {@code super()}.
     */
    protected Part() {
        this.id = globalIdCounter.incrementAndGet();
    }

    /**
     * Returns the unique ID of this part.
     *
     * @return part ID (always ≥ 1)
     */
    public int getId() {
        return id;
    }

    /**
     * Returns a human-readable description like {@code "Body#42"}.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + id;
    }
}