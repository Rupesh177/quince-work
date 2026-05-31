package com.quince.framework.core.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Thread-safe manager for WebDriverWait instances.
 */
public class WaitManager {
    private static final Logger logger = LogManager.getLogger(WaitManager.class);
    private static final ThreadLocal<WebDriverWait> waitThreadLocal = new ThreadLocal<>();
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    /**
     * Initializes wait with default timeout.
     */
    public static void initializeWait() {
        initializeWait(DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Initializes wait with specified timeout.
     */
    public static void initializeWait(int timeoutSeconds) {
        UIDriver driver = DriverManager.getDriver();
        Object rawDriver = driver.getUnderlyingDriver();
        if (rawDriver == null) {
            throw new IllegalStateException("Underlying WebDriver is null for thread: " + Thread.currentThread().getId());
        }
        WebDriver underlying = (WebDriver) rawDriver;
        WebDriverWait wait = new WebDriverWait(underlying, Duration.ofSeconds(timeoutSeconds));
        logger.debug("Initialized WebDriverWait with {} seconds timeout", timeoutSeconds);
        waitThreadLocal.set(wait);
    }

    /**
     * Gets the wait instance for the current thread.
     */
    public static WebDriverWait getWait() {
        WebDriverWait wait = waitThreadLocal.get();
        if (wait == null) {
            logger.warn("Wait not initialized, initializing with default timeout");
            initializeWait();
            wait = waitThreadLocal.get();
        }
        return wait;
    }

    /**
     * Removes wait for the current thread.
     */
    public static void removeWait() {
        waitThreadLocal.remove();
    }
}