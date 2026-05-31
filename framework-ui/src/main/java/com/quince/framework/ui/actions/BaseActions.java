package com.quince.framework.ui.actions;

import com.quince.framework.core.driver.UIDriver;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;

/**
 * Abstract base class for all action classes.
 * Provides common utilities for element interaction with retry logic and healing.
 */
public abstract class BaseActions {
    protected static final Logger logger = LogManager.getLogger(BaseActions.class);
    
    protected final UIDriver driver;
    private static final int MAX_RETRIES = 2;
    private static final int RETRY_DELAY_MS = 500;

    /**
     * Constructor accepting a UIDriver instance.
     */
    protected BaseActions(UIDriver driver) {
        this.driver = driver;
        logger.debug("Initialized {}", this.getClass().getSimpleName());
    }

    /**
     * Finds an element with retry logic.
     * On failure, attaches screenshot to Allure.
     */
    protected void find(By locator) {
        executeWithRetry(() -> {
            driver.findElement(locator);
            return null;
        }, "find", locator);
    }

    /**
     * Clicks an element with retry and soft assertion.
     */
    protected void click(By locator) {
        executeWithRetry(() -> {
            driver.click(locator);
            logger.info("Clicked element: {}", locator);
            return null;
        }, "click", locator);
    }

    /**
     * Types text into an element.
     */
    protected void type(By locator, String text) {
        executeWithRetry(() -> {
            driver.sendKeys(locator, text);
            logger.info("Typed text in element: {}", locator);
            return null;
        }, "type", locator);
    }

    /**
     * Gets text from an element.
     */
    protected String getText(By locator) {
        return executeWithRetry(() -> driver.getText(locator), "getText", locator);
    }

    /**
     * Gets attribute value.
     */
    protected String getAttribute(By locator, String attributeName) {
        return executeWithRetry(
            () -> driver.getAttribute(locator, attributeName),
            "getAttribute:" + attributeName,
            locator
        );
    }

    /**
     * Waits for element visibility.
     */
    protected void waitForVisible(By locator) {
        try {
            driver.waitForElementVisible(locator, 10);
            logger.info("Element visible: {}", locator);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for visible element: {}", locator);
            driver.takeScreenshot("visibility-timeout-" + System.currentTimeMillis());
            Allure.addAttachment("visibility-timeout", "text/plain", 
                "Timeout waiting for: " + locator);
            throw e;
        }
    }

    /**
     * Waits for element clickability.
     */
    protected void waitForClickable(By locator) {
        try {
            driver.waitForElementClickable(locator, 10);
            logger.info("Element clickable: {}", locator);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for clickable element: {}", locator);
            driver.takeScreenshot("clickable-timeout-" + System.currentTimeMillis());
            throw e;
        }
    }

    /**
     * Soft assertion: checks element visibility without throwing.
     */
    protected boolean softAssertVisible(By locator) {
        try {
            return driver.isDisplayed(locator);
        } catch (Exception e) {
            logger.warn("Soft assertion failed for: {}", locator);
            return false;
        }
    }

    /**
     * Executes an operation with retry logic.
     */
    @SuppressWarnings("unchecked")
    private <T> T executeWithRetry(Executable<T> operation, String operationName, By locator) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.debug("Executing {} (attempt {}/{}): {}", 
                    operationName, attempt, MAX_RETRIES, locator);
                T result = operation.execute();
                if (attempt > 1) {
                    logger.info("Retry successful on attempt {}", attempt);
                }
                return result;
            } catch (NoSuchElementException | TimeoutException e) {
                logger.warn("{} failed (attempt {}/{}): {}", 
                    operationName, attempt, MAX_RETRIES, locator);
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.error("All retries exhausted for: {}", locator);
                    driver.takeScreenshot("action-failure-" + operationName);
                    Allure.addAttachment(
                        operationName + "-failure",
                        "text/plain",
                        e.getMessage()
                    );
                    throw e;
                }
            }
        }
        return null;
    }

    /**
     * Functional interface for retryable operations.
     */
    @FunctionalInterface
    protected interface Executable<T> {
        T execute() throws NoSuchElementException, TimeoutException;
    }
}
