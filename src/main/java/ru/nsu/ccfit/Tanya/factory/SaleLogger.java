package ru.nsu.ccfit.Tanya.factory;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.model.Car;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Writes one line to {@code factory_sales.log} every time a dealer purchases a car.
 *
 * <p>The format matches the specification exactly:</p>
 * <pre>
 * &lt;Time&gt;: Dealer &lt;Number&gt;: Auto &lt;ID&gt; (Body: &lt;ID&gt;, Motor: &lt;ID&gt;, Accessory: &lt;ID&gt;)
 * </pre>
 *
 * <p>The write method is {@code synchronized} so multiple dealer threads can call
 * it concurrently without garbling the output.</p>
 *
 * <p>Call {@link #close()} when the application shuts down to flush and close
 * the underlying file.</p>
 */
public class SaleLogger {

    private static final Logger logger = Logger.getLogger(SaleLogger.class);

    /** Date formatter.  Not thread-safe — access is guarded by {@code synchronized}. */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    /** Underlying writer.  {@code null} when logging is disabled. */
    private final PrintWriter writer;

    /** Whether sale logging is enabled (read from config). */
    private final boolean enabled;

    /**
     * Opens (or skips) the sales log file.
     *
     * @param enabled {@code true} to create / overwrite the log file; {@code false} to do nothing
     * @throws IOException if the file cannot be created or opened
     */
    public SaleLogger(boolean enabled) throws IOException {
        this.enabled = enabled;
        if (enabled) {
            // BufferedWriter improves performance; false = overwrite on each run.
            writer = new PrintWriter(new BufferedWriter(new FileWriter("factory_sales.log", false)));
            logger.info("SaleLogger: sales will be written to factory_sales.log");
        } else {
            writer = null;
            logger.info("SaleLogger: sale logging is disabled (LogSale=false).");
        }
    }

    /**
     * Logs one car sale.
     *
     * <p>Output format:
     * {@code HH:mm:ss.SSS: Dealer N: Auto ID (Body: ID, Motor: ID, Accessory: ID)}</p>
     *
     * @param dealerNumber the identifier of the dealer (1-based)
     * @param car          the car that was sold
     */
    public synchronized void logSale(int dealerNumber, Car car) {
        if (!enabled || writer == null) {
            return;
        }

        String time = dateFormat.format(new Date());

        // Exact format from the specification:
        // <Time>: Dealer <Number>: Auto <ID> (Body: <ID>, Motor: <ID>, Accessory: <ID>)
        String line = time
                + ": Dealer " + dealerNumber
                + ": Auto "   + car.getId()
                + " (Body: "  + car.getBody().getId()
                + ", Motor: " + car.getEngine().getId()
                + ", Accessory: " + car.getAccessory().getId() + ")";

        writer.println(line);
        // Flush immediately so lines appear even if the JVM crashes.
        writer.flush();
    }

    /**
     * Flushes and closes the underlying log file.
     * Safe to call multiple times (subsequent calls are no-ops).
     */
    public synchronized void close() {
        if (writer != null) {
            writer.flush();
            writer.close();
            logger.info("SaleLogger closed.");
        }
    }
}