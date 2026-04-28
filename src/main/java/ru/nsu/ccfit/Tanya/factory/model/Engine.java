package ru.nsu.ccfit.Tanya.factory.model;

/**
 * Represents a car engine (called "Motor" in the config file).
 * Extends {@link Part} to inherit automatic unique-ID assignment.
 */
public class Engine extends Part {

    /**
     * Creates a new Engine part.
     * The unique ID is assigned automatically by the {@link Part} superclass.
     */
    public Engine() {
        super();
    }
}