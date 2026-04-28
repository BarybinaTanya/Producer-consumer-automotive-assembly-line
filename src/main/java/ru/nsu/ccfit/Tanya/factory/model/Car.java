package ru.nsu.ccfit.Tanya.factory.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a fully assembled car.
 *
 * <p>A car consists of exactly one {@link Body}, one {@link Engine}, and one
 * {@link Accessory}.  Like parts, each car has a unique ID assigned at
 * construction time via an {@link AtomicInteger}.</p>
 */
public class Car {

    /** Global counter used to assign unique IDs to every assembled car. */
    private static final AtomicInteger carIdCounter = new AtomicInteger(0);

    /** Unique identifier for this car. */
    private final int id;

    /** The body used in this car. */
    private final Body body;

    /** The engine used in this car. */
    private final Engine engine;

    /** The accessory used in this car. */
    private final Accessory accessory;

    /**
     * Creates a new assembled Car from the given parts.
     *
     * @param body      the car body — must not be {@code null}
     * @param engine    the car engine — must not be {@code null}
     * @param accessory the car accessory — must not be {@code null}
     */
    public Car(Body body, Engine engine, Accessory accessory) {
        this.id        = carIdCounter.incrementAndGet();
        this.body      = body;
        this.engine    = engine;
        this.accessory = accessory;
    }

    /** @return the unique ID of this car */
    public int getId()            { return id; }

    /** @return the body part used in this car */
    public Body getBody()         { return body; }

    /** @return the engine part used in this car */
    public Engine getEngine()     { return engine; }

    /** @return the accessory part used in this car */
    public Accessory getAccessory() { return accessory; }

    @Override
    public String toString() {
        return "Car#" + id +
                "(Body:" + body.getId() +
                ",Engine:" + engine.getId() +
                ",Accessory:" + accessory.getId() + ")";
    }
}