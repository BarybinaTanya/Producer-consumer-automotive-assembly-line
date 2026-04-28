package ru.nsu.ccfit.Tanya.factory;

import org.apache.log4j.Logger;
import ru.nsu.ccfit.Tanya.factory.config.FactoryConfig;
import ru.nsu.ccfit.Tanya.factory.gui.FactoryGUI;
import ru.nsu.ccfit.Tanya.factory.model.*;
import ru.nsu.ccfit.Tanya.factory.threads.*;
import ru.nsu.ccfit.Tanya.threadpool.ThreadPool;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the Factory Work Emulator.
 *
 * <p>Responsibilities:</p>
 * <ol>
 *   <li>Load the configuration from {@code factory.properties}.</li>
 *   <li>Create all warehouses, the ThreadPool, and the SaleLogger.</li>
 *   <li>Start the StockController, supplier threads, and dealer threads.</li>
 *   <li>Open the Swing GUI on the Event Dispatch Thread.</li>
 *   <li>Register a JVM shutdown hook to interrupt all threads and flush the log.</li>
 * </ol>
 */
public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1. Load configuration
        // ---------------------------------------------------------------
        FactoryConfig config;
        try {
            config = new FactoryConfig();
        } catch (IOException e) {
            System.err.println("FATAL: Cannot load factory.properties — " + e.getMessage());
            System.exit(1);
            return; // unreachable; satisfies the compiler's definite-assignment check
        }

        // ---------------------------------------------------------------
        // 2. Create warehouses
        // ---------------------------------------------------------------
        Storage<Body>      bodyStorage      = new Storage<>(config.getStorageBodySize(),      "BodyStorage");
        Storage<Engine>    engineStorage    = new Storage<>(config.getStorageMotorSize(),     "EngineStorage");
        Storage<Accessory> accessoryStorage = new Storage<>(config.getStorageAccessorySize(), "AccessoryStorage");
        Storage<Car>       carStorage       = new Storage<>(config.getStorageAutoSize(),       "CarStorage");

        // ---------------------------------------------------------------
        // 3. Create ThreadPool and SaleLogger
        // ---------------------------------------------------------------
        ThreadPool threadPool;
        try {
            threadPool = new ThreadPool(config.getWorkers());
        } catch (IllegalArgumentException e) {
            System.err.println("FATAL: Invalid Workers value in config — " + e.getMessage());
            System.exit(1);
            return;
        }

        SaleLogger saleLogger;
        try {
            saleLogger = new SaleLogger(config.isLogSale());
        } catch (IOException e) {
            System.err.println("FATAL: Cannot create sale log file — " + e.getMessage());
            threadPool.shutdown();
            System.exit(1);
            return;
        }

        // ---------------------------------------------------------------
        // 4. Start the StockController (monitors the car warehouse)
        // ---------------------------------------------------------------
        StockController stockController = new StockController(
                carStorage, bodyStorage, engineStorage, accessoryStorage, threadPool);
        stockController.start();
        logger.info("StockController thread started.");

        // ---------------------------------------------------------------
        // 5. Start part suppliers (one body, one engine, N accessory)
        // ---------------------------------------------------------------
        BodySupplier bodySupplier = new BodySupplier(bodyStorage, 500);
        bodySupplier.start();

        EngineSupplier engineSupplier = new EngineSupplier(engineStorage, 500);
        engineSupplier.start();

        List<AccessorySupplier> accessorySuppliers = new ArrayList<>();
        for (int i = 1; i <= config.getAccessorySuppliers(); i++) {
            AccessorySupplier supplier = new AccessorySupplier(i, accessoryStorage, 500);
            supplier.start();
            accessorySuppliers.add(supplier);
        }
        logger.info("Started " + (2 + accessorySuppliers.size()) + " supplier thread(s).");

        // ---------------------------------------------------------------
        // 6. Start dealer threads
        // ---------------------------------------------------------------
        List<Dealer> dealers = new ArrayList<>();
        for (int i = 1; i <= config.getDealers(); i++) {
            Dealer dealer = new Dealer(i, carStorage, saleLogger, 2000);
            dealer.start();
            dealers.add(dealer);
        }
        logger.info("Started " + dealers.size() + " dealer thread(s).");

        // ---------------------------------------------------------------
        // 7. Register a JVM shutdown hook for clean teardown
        //    (called when System.exit() is invoked or the JVM is killed)
        // ---------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered — stopping all threads.");

            // Stop the pool first so no new assembly tasks are accepted.
            threadPool.shutdown();

            // Interrupt all daemon threads so they exit their loops.
            stockController.interrupt();
            bodySupplier.interrupt();
            engineSupplier.interrupt();
            accessorySuppliers.forEach(Thread::interrupt);
            dealers.forEach(Thread::interrupt);

            // Flush and close the sales log file.
            saleLogger.close();

            logger.info("All threads interrupted. Goodbye.");
        }, "ShutdownHook"));

        // ---------------------------------------------------------------
        // 8. Launch the Swing GUI on the Event Dispatch Thread
        // ---------------------------------------------------------------
        final FactoryConfig  finalConfig             = config;
        final ThreadPool     finalThreadPool         = threadPool;
        final BodySupplier   finalBodySupplier       = bodySupplier;
        final EngineSupplier finalEngineSupplier     = engineSupplier;
        final List<Dealer>   finalDealers            = dealers;
        final List<AccessorySupplier> finalAccessory = accessorySuppliers;

        SwingUtilities.invokeLater(() -> {
            FactoryGUI gui = new FactoryGUI(
                    finalConfig,
                    bodyStorage,
                    engineStorage,
                    accessoryStorage,
                    carStorage,
                    finalBodySupplier,
                    finalEngineSupplier,
                    finalAccessory,
                    finalDealers,
                    finalThreadPool);

            // DO_NOTHING_ON_CLOSE so we control the shutdown ourselves.
            gui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            gui.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    logger.info("Window close button pressed — calling System.exit(0).");
                    // System.exit(0) triggers the shutdown hook registered above.
                    System.exit(0);
                }
            });

            gui.setVisible(true);
        });
    }
}