package com.quince.framework.ui.driver;

import com.quince.framework.core.driver.UIDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Stub implementation of UIDriver for Playwright.
 * Placeholder only — no actual Playwright dependency.
 * Can be replaced with real Playwright implementation in future.
 */
public class PlaywrightDriver implements UIDriver {
    private static final Logger logger = LogManager.getLogger(PlaywrightDriver.class);

    public PlaywrightDriver() {
        logger.warn("PlaywrightDriver is a stub implementation");
        throw new UnsupportedOperationException(
                "PlaywrightDriver not yet implemented. Use SeleniumDriver or implement " +
                        "PlaywrightDriver with actual Playwright dependency."
        );
    }

    @Override
    public void get(String url) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public void findElement(By locator) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public List<String> findElements(By locator) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public void click(By locator) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public void sendKeys(By locator, String text) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public String getText(By locator) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public String getAttribute(By locator, String attributeName) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public boolean isDisplayed(By locator) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public void waitForElementVisible(By locator, int timeoutSeconds) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public void waitForElementClickable(By locator, int timeoutSeconds) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public String takeScreenshot(String filename) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public Object executeScript(String script, Object... args) {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public String getCurrentUrl() {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public void quit() {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }

    @Override
    public Object getUnderlyingDriver() {
        throw new UnsupportedOperationException("PlaywrightDriver stub");
    }
}
