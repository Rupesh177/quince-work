package com.quince.framework.core.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe manager for UIDriver instances using ThreadLocal.
 * Ensures each test thread has its own driver instance.
 */
public class DriverManager {
    private static final Logger logger = LogManager.getLogger(DriverManager.class);
    private static final ThreadLocal<UIDriver> driverThreadLocal = new ThreadLocal<>();

    /**
     * Sets the driver for the current thread.
     */
    public static void setDriver(UIDriver driver) {
        logger.debug("Setting driver for thread: {}", Thread.currentThread().getId());
        driverThreadLocal.set(driver);
    }

    /**
     * Gets the driver for the current thread.
     *
     * @throws IllegalStateException if driver not initialized for this thread
     */
    public static UIDriver getDriver() {
        UIDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "Driver not initialized for thread: " + Thread.currentThread().getId() +
                            ". Call DriverManager.setDriver() first."
            );
        }
        return driver;
    }

    /**
     * Checks if driver is initialized for the current thread.
     */
    public static boolean isDriverInitialized() {
        return driverThreadLocal.get() != null;
    }

    /**
     * Cleans up driver for the current thread.
     */
    public static void removeDriver() {
        UIDriver driver = driverThreadLocal.get();
        if (driver != null) {
            logger.debug("Removing driver for thread: {}", Thread.currentThread().getId());
            try {
                driver.quit();
            } catch (Exception e) {
                logger.warn("Error closing driver", e);
            }
            driverThreadLocal.remove();
        }
    }
}