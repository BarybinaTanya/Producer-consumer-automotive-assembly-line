package ru.nsu.ccfit.Tanya.factory.config;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads factory configuration from {@code factory.properties} on the classpath.
 *
 * <p>We use {@code ClassLoader.getResourceAsStream()} so the file is found
 * whether the application is run from the IDE, with Gradle {@code run}, or
 * from a JAR file.</p>
 *
 * <p>Every getter has a sensible default value in case a property is missing.</p>
 */
public class FactoryConfig {

    private static final Logger logger = Logger.getLogger(FactoryConfig.class);

    /** Name of the properties file on the classpath. */
    private static final String CONFIG_FILE = "factory.properties";

    /** The loaded key-value pairs. */
    private final Properties props;

    /**
     * Loads the configuration file from the classpath.
     *
     * @throws IOException if the file cannot be found or read
     */
    public FactoryConfig() throws IOException {
        props = new Properties();

        // ClassLoader.getResourceAsStream works in JARs and IDEs alike.
        InputStream stream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (stream == null) {
            throw new IOException("Cannot find '" + CONFIG_FILE + "' on the classpath. "
                    + "Make sure it is in src/main/resources/.");
        }

        try {
            props.load(stream);
        } finally {
            stream.close();
        }

        logger.info("Factory configuration loaded from '" + CONFIG_FILE + "'.");
        logger.info("  StorageBodySize="     + getStorageBodySize());
        logger.info("  StorageMotorSize="    + getStorageMotorSize());
        logger.info("  StorageAccessorySize="+ getStorageAccessorySize());
        logger.info("  StorageAutoSize="     + getStorageAutoSize());
        logger.info("  AccessorySuppliers="  + getAccessorySuppliers());
        logger.info("  Workers="             + getWorkers());
        logger.info("  Dealers="             + getDealers());
        logger.info("  LogSale="             + isLogSale());
    }

    // -----------------------------------------------------------------------
    // Typed accessors with default fallback values
    // -----------------------------------------------------------------------

    /** @return capacity of the body-parts warehouse */
    public int getStorageBodySize() {
        return getInt("StorageBodySize", 100);
    }

    /** @return capacity of the engine (motor) warehouse */
    public int getStorageMotorSize() {
        return getInt("StorageMotorSize", 100);
    }

    /** @return capacity of the accessory warehouse */
    public int getStorageAccessorySize() {
        return getInt("StorageAccessorySize", 100);
    }

    /** @return capacity of the finished-car warehouse */
    public int getStorageAutoSize() {
        return getInt("StorageAutoSize", 100);
    }

    /** @return number of accessory supplier threads to start */
    public int getAccessorySuppliers() {
        return getInt("AccessorySuppliers", 5);
    }

    /** @return number of assembler threads in the ThreadPool */
    public int getWorkers() {
        return getInt("Workers", 10);
    }

    /** @return number of dealer threads to start */
    public int getDealers() {
        return getInt("Dealers", 20);
    }

    /** @return {@code true} if every car sale should be written to the log file */
    public boolean isLogSale() {
        return Boolean.parseBoolean(props.getProperty("LogSale", "true"));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Parses an integer property with a default fallback.
     *
     * @param key          property key
     * @param defaultValue value returned when the key is missing or unparseable
     * @return parsed integer value
     */
    private int getInt(String key, int defaultValue) {
        String raw = props.getProperty(key);
        if (raw == null) {
            logger.warn("Property '" + key + "' not found; using default " + defaultValue);
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            logger.warn("Property '" + key + "' has invalid value '" + raw + "'; using default " + defaultValue);
            return defaultValue;
        }
    }
}