package com.quince.framework.core.driver;

import org.openqa.selenium.By;

import java.util.List;

/**
 * Abstraction for UI driver implementations (Selenium, Playwright, etc).
 * Allows pluggable driver implementations without test code changes.
 */
public interface UIDriver {

    /**
     * Navigates to the specified URL.
     */
    void get(String url);

    /**
     * Finds a single element by locator.
     *
     * @throws org.openqa.selenium.NoSuchElementException if element not found
     */
    void findElement(By locator);

    /**
     * Finds all elements matching the locator.
     */
    List<String> findElements(By locator);

    /**
     * Clicks an element located by the given By.
     */
    void click(By locator);

    /**
     * Sends text to an element.
     */
    void sendKeys(By locator, String text);

    /**
     * Gets text content of an element.
     */
    String getText(By locator);

    /**
     * Gets an element's attribute value.
     */
    String getAttribute(By locator, String attributeName);

    /**
     * Checks if element is displayed.
     */
    boolean isDisplayed(By locator);

    /**
     * Waits for element to be visible.
     */
    void waitForElementVisible(By locator, int timeoutSeconds);

    /**
     * Waits for element to be clickable.
     */
    void waitForElementClickable(By locator, int timeoutSeconds);

    /**
     * Takes a screenshot and returns file path.
     */
    String takeScreenshot(String filename);

    /**
     * Executes JavaScript.
     */
    Object executeScript(String script, Object... args);

    /**
     * Gets the current page title.
     */
    String getTitle();

    /**
     * Gets the current page URL.
     */
    String getCurrentUrl();

    /**
     * Quits the driver and closes all windows.
     */
    void quit();

    /**
     * Returns the underlying driver object (for advanced operations).
     */
    Object getUnderlyingDriver();
}