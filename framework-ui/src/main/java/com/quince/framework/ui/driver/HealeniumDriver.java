package com.quince.framework.ui.driver;

import com.quince.framework.core.driver.UIDriver;
import com.epam.healenium.SelfHealingDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UIDriver wrapper that applies Healenium auto-healing.
 * Delegates all UIDriver interface calls to the underlying Healenium-wrapped driver.
 * <p>
 * Configuration: src/test/resources/healenium.properties
 */
public class HealeniumDriver implements UIDriver {
    private static final Logger logger = LogManager.getLogger(HealeniumDriver.class);

    private final UIDriver delegate;
    private final SelfHealingDriver healedDriver;
    private final WebDriver underlyingWebDriver;

    public HealeniumDriver(UIDriver delegate) {
        this.delegate = delegate;
        this.underlyingWebDriver = (WebDriver) delegate.getUnderlyingDriver();

        SelfHealingDriver healed = null;
        try {
            healed = SelfHealingDriver.create(underlyingWebDriver);
            logger.info("Healenium SelfHealingDriver initialized");
        } catch (Exception e) {
            logger.warn("Failed to initialize Healenium; continuing without healing", e);
        }
        this.healedDriver = healed;
    }

    @Override
    public void get(String url) {
        logger.info("Navigating to: {}", url);
        delegate.get(url);
    }

    @Override
    public void findElement(By locator) {
        try {
            logger.debug("Finding element: {}", locator);
            activeDriver().findElement(locator);
        } catch (Exception e) {
            logger.error("Element not found (even with healing): {}", locator);
            throw e;
        }
    }

    @Override
    public List<String> findElements(By locator) {
        try {
            return activeDriver().findElements(locator).stream()
                    .map(el -> el.getText())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error finding elements: {}", locator, e);
            return List.of();
        }
    }

    @Override
    public void click(By locator) {
        try {
            logger.debug("Clicking element: {}", locator);
            activeDriver().findElement(locator).click();
            logger.info("Clicked: {}", locator);
        } catch (Exception e) {
            logger.error("Error clicking element: {}", locator, e);
            throw e;
        }
    }

    @Override
    public void sendKeys(By locator, String text) {
        try {
            logger.debug("Sending keys to: {}", locator);
            var element = activeDriver().findElement(locator);
            element.clear();
            element.sendKeys(text);
            logger.info("Text sent to: {}", locator);
        } catch (Exception e) {
            logger.error("Error sending keys: {}", locator, e);
            throw e;
        }
    }

    @Override
    public String getText(By locator) {
        try {
            return activeDriver().findElement(locator).getText();
        } catch (Exception e) {
            logger.error("Error getting text: {}", locator, e);
            throw e;
        }
    }

    @Override
    public String getAttribute(By locator, String attributeName) {
        try {
            return activeDriver().findElement(locator).getAttribute(attributeName);
        } catch (Exception e) {
            logger.error("Error getting attribute: {}", locator, e);
            throw e;
        }
    }

    @Override
    public boolean isDisplayed(By locator) {
        try {
            return activeDriver().findElement(locator).isDisplayed();
        } catch (Exception e) {
            logger.debug("Element not found or not displayed");
            return false;
        }
    }

    @Override
    public void waitForElementVisible(By locator, int timeoutSeconds) {
        delegate.waitForElementVisible(locator, timeoutSeconds);
    }

    @Override
    public void waitForElementClickable(By locator, int timeoutSeconds) {
        delegate.waitForElementClickable(locator, timeoutSeconds);
    }

    @Override
    public String takeScreenshot(String filename) {
        return delegate.takeScreenshot(filename);
    }

    @Override
    public Object executeScript(String script, Object... args) {
        return delegate.executeScript(script, args);
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getCurrentUrl() {
        return delegate.getCurrentUrl();
    }

    @Override
    public void quit() {
        logger.info("Quitting Healenium-wrapped driver");
        try {
            if (healedDriver != null) {
                healedDriver.quit();
            } else {
                delegate.quit();
            }
        } catch (Exception e) {
            logger.warn("Error quitting driver", e);
            if (healedDriver != null) {
                delegate.quit();
            }
        }
    }

    @Override
    public Object getUnderlyingDriver() {
        return activeDriver();
    }

    private WebDriver activeDriver() {
        return healedDriver != null ? healedDriver : underlyingWebDriver;
    }
}
