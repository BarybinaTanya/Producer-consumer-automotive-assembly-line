package ru.nsu.ccfit.Tanya.factory.gui;

import ru.nsu.ccfit.Tanya.factory.config.FactoryConfig;
import ru.nsu.ccfit.Tanya.factory.model.*;
import ru.nsu.ccfit.Tanya.factory.threads.*;
import ru.nsu.ccfit.Tanya.threadpool.ThreadPool;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Main Swing window for the Factory Work Emulator.
 *
 * <h3>What it shows</h3>
 * <ul>
 *   <li>Current count / capacity of every warehouse (refreshed every 500 ms).</li>
 *   <li>Total parts produced by each supplier group.</li>
 *   <li>Total cars assembled and total cars sold.</li>
 *   <li>Pending tasks in the ThreadPool queue.</li>
 * </ul>
 *
 * <h3>What it controls</h3>
 * <ul>
 *   <li>Production speed of the body supplier (slider).</li>
 *   <li>Production speed of the engine supplier (slider).</li>
 *   <li>Production speed of all accessory suppliers (one slider for all).</li>
 *   <li>Purchase speed of all dealers (one slider for all).</li>
 * </ul>
 *
 * <p><strong>This class depends on Swing — intentionally separated from the model
 * and thread classes which are pure Java with no GUI dependency.</strong></p>
 */
public class FactoryGUI extends JFrame {

    // -----------------------------------------------------------------------
    // References to factory components (for reading live data)
    // -----------------------------------------------------------------------
    private final Storage<Body>            bodyStorage;
    private final Storage<Engine>          engineStorage;
    private final Storage<Accessory>       accessoryStorage;
    private final Storage<Car>             carStorage;
    private final BodySupplier             bodySupplier;
    private final EngineSupplier           engineSupplier;
    private final List<AccessorySupplier>  accessorySuppliers;
    private final List<Dealer>             dealers;
    private final ThreadPool               threadPool;

    // -----------------------------------------------------------------------
    // Live-data labels (updated by the Swing Timer)
    // -----------------------------------------------------------------------
    private final JLabel labelBodyCount        = new JLabel();
    private final JLabel labelEngineCount      = new JLabel();
    private final JLabel labelAccessoryCount   = new JLabel();
    private final JLabel labelCarCount         = new JLabel();
    private final JLabel labelBodyProduced     = new JLabel();
    private final JLabel labelEngineProduced   = new JLabel();
    private final JLabel labelAccessoryProduced= new JLabel();
    private final JLabel labelTotalAssembled   = new JLabel();
    private final JLabel labelTotalSold        = new JLabel();
    private final JLabel labelQueueSize        = new JLabel();

    // -----------------------------------------------------------------------
    // Speed-control sliders
    // -----------------------------------------------------------------------

    /** Slider for body-supplier delay.  Left = fast (100 ms), right = slow (5000 ms). */
    private final JSlider sliderBodySpeed      = makeDelaySlider(500);

    /** Slider for engine-supplier delay. */
    private final JSlider sliderEngineSpeed    = makeDelaySlider(500);

    /** Slider for accessory-supplier delay (same value applied to ALL accessory suppliers). */
    private final JSlider sliderAccessorySpeed = makeDelaySlider(500);

    /** Slider for dealer delay (same value applied to ALL dealers). */
    private final JSlider sliderDealerSpeed    = makeDelaySlider(1000);

    /**
     * Constructs and lays out the factory GUI window.
     *
     * @param config             factory configuration (used only for capacity labels)
     * @param bodyStorage        body warehouse
     * @param engineStorage      engine warehouse
     * @param accessoryStorage   accessory warehouse
     * @param carStorage         finished-car warehouse
     * @param bodySupplier       body supplier thread
     * @param engineSupplier     engine supplier thread
     * @param accessorySuppliers list of all accessory supplier threads
     * @param dealers            list of all dealer threads
     * @param threadPool         the assembly thread pool
     */
    public FactoryGUI(FactoryConfig          config,
                      Storage<Body>          bodyStorage,
                      Storage<Engine>        engineStorage,
                      Storage<Accessory>     accessoryStorage,
                      Storage<Car>           carStorage,
                      BodySupplier           bodySupplier,
                      EngineSupplier         engineSupplier,
                      List<AccessorySupplier> accessorySuppliers,
                      List<Dealer>           dealers,
                      ThreadPool             threadPool) {

        super("Factory Work Emulator"); // parent class object link

        this.bodyStorage        = bodyStorage;
        this.engineStorage      = engineStorage;
        this.accessoryStorage   = accessoryStorage;
        this.carStorage         = carStorage;
        this.bodySupplier       = bodySupplier;
        this.engineSupplier     = engineSupplier;
        this.accessorySuppliers = accessorySuppliers;
        this.dealers            = dealers;
        this.threadPool         = threadPool;

        // Build the layout
        setLayout(new BorderLayout(10, 10));
        add(buildStatusPanel(), BorderLayout.CENTER);
        add(buildControlPanel(), BorderLayout.SOUTH);

        // Connect slider listeners AFTER adding to window so the initial
        // slider values don't fire spurious change events during construction.
        attachSliderListeners();

        // Refresh labels every 500 ms using a Swing Timer (runs on the EDT).
        new Timer(500, e -> refreshLabels()).start();

        // Initial refresh so labels are not blank on first paint.
        refreshLabels();

        pack();
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null); // center on screen
    }

    // -----------------------------------------------------------------------
    // UI construction helpers
    // -----------------------------------------------------------------------

    /** Builds the top / center panel showing all warehouse and production statistics. */
    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 4));
        panel.setBorder(new TitledBorder("Factory Status"));

        // --- Warehouses ---
        panel.add(bold("Body warehouse:"));
        panel.add(labelBodyCount);

        panel.add(bold("Engine warehouse:"));
        panel.add(labelEngineCount);

        panel.add(bold("Accessory warehouse:"));
        panel.add(labelAccessoryCount);

        panel.add(bold("Car warehouse:"));
        panel.add(labelCarCount);

        panel.add(new JSeparator()); panel.add(new JSeparator());

        // --- Supplier production totals ---
        panel.add(bold("Bodies produced:"));
        panel.add(labelBodyProduced);

        panel.add(bold("Engines produced:"));
        panel.add(labelEngineProduced);

        panel.add(bold("Accessories produced (all suppliers):"));
        panel.add(labelAccessoryProduced);

        panel.add(new JSeparator()); panel.add(new JSeparator());

        // --- Assembly stats ---
        panel.add(bold("Cars assembled (total):"));
        panel.add(labelTotalAssembled);

        panel.add(bold("Cars sold (total):"));
        panel.add(labelTotalSold);

        panel.add(bold("Pending assembly tasks (queue):"));
        panel.add(labelQueueSize);

        return panel;
    }

    /** Builds the bottom panel with speed sliders. */
    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 4));
        panel.setBorder(new TitledBorder("Speed Controls  [left = fast (100 ms) … right = slow (5000 ms)]"));

        panel.add(bold("Body supplier delay (ms):"));
        panel.add(sliderBodySpeed);

        panel.add(bold("Engine supplier delay (ms):"));
        panel.add(sliderEngineSpeed);

        panel.add(bold("Accessory supplier delay (ms):"));
        panel.add(sliderAccessorySpeed);

        panel.add(bold("Dealer purchase delay (ms):"));
        panel.add(sliderDealerSpeed);

        return panel;
    }

    /**
     * Attaches {@code ChangeListener}s to each slider so that moving the slider
     * immediately updates the corresponding thread's delay.
     */
    private void attachSliderListeners() {
        sliderBodySpeed.addChangeListener(e ->
                bodySupplier.setDelayMs(sliderBodySpeed.getValue()));

        sliderEngineSpeed.addChangeListener(e ->
                engineSupplier.setDelayMs(sliderEngineSpeed.getValue()));

        sliderAccessorySpeed.addChangeListener(e -> {
            int delay = sliderAccessorySpeed.getValue();
            // Apply the same delay to all accessory supplier threads.
            for (AccessorySupplier supplier : accessorySuppliers) {
                supplier.setDelayMs(delay);
            }
        });

        sliderDealerSpeed.addChangeListener(e -> {
            int delay = sliderDealerSpeed.getValue();
            // Apply the same delay to all dealer threads.
            for (Dealer dealer : dealers) {
                dealer.setDelayMs(delay);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Live-data refresh (called by the Swing Timer on the EDT)
    // -----------------------------------------------------------------------

    /**
     * Reads the current state from all factory components and updates the labels.
     * This method is always called on the Swing Event Dispatch Thread by the Timer.
     */
    private void refreshLabels() {
        labelBodyCount.setText(bodyStorage.getCount()
                + " / " + bodyStorage.getCapacity());

        labelEngineCount.setText(engineStorage.getCount()
                + " / " + engineStorage.getCapacity());

        labelAccessoryCount.setText(accessoryStorage.getCount()
                + " / " + accessoryStorage.getCapacity());

        labelCarCount.setText(carStorage.getCount()
                + " / " + carStorage.getCapacity());

        labelBodyProduced.setText(String.valueOf(bodySupplier.getTotalProduced()));
        labelEngineProduced.setText(String.valueOf(engineSupplier.getTotalProduced()));
        labelAccessoryProduced.setText(String.valueOf(AccessorySupplier.getTotalProducedAll()));

        labelTotalAssembled.setText(String.valueOf(AssemblyTask.getTotalCarsProduced()));
        labelTotalSold.setText(String.valueOf(Dealer.getTotalSold()));
        labelQueueSize.setText(String.valueOf(threadPool.getQueueSize()));
    }

    // -----------------------------------------------------------------------
    // Minor utility helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a JLabel with bold text.
     *
     * @param text label text
     * @return bold JLabel
     */
    private static JLabel bold(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    /**
     * Creates a JSlider for controlling delays.
     * Range: 100 ms (fast) … 5000 ms (slow).
     *
     * @param initialValue starting value in milliseconds
     * @return configured JSlider
     */
    private static JSlider makeDelaySlider(int initialValue) {
        JSlider slider = new JSlider(100, 5000, initialValue);
        slider.setMajorTickSpacing(1000);
        slider.setMinorTickSpacing(200);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }
}