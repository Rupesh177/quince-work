package com.quince.framework.ui.driver;

import com.quince.framework.core.driver.UIDriver;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selenium-based implementation of UIDriver.
 * Wraps WebDriver with thread safety and enhanced logging.
 */
public class SeleniumDriver implements UIDriver {
    private static final Logger logger = LogManager.getLogger(SeleniumDriver.class);

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final int defaultTimeoutSeconds;

    public SeleniumDriver(WebDriver driver) {
        this(driver, 10);
    }

    public SeleniumDriver(WebDriver driver, int defaultTimeoutSeconds) {
        this.driver = driver;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeoutSeconds));
        logger.info("SeleniumDriver initialized with timeout: {} seconds", defaultTimeoutSeconds);
    }

    @Override
    public void get(String url) {
        logger.info("Navigating to: {}", url);
        try {
            driver.get(url);
            Allure.parameter("URL", url);
        } catch (Exception e) {
            logger.error("Navigation failed to: {}", url, e);
            throw e;
        }
    }

    @Override
    public void findElement(By locator) {
        logger.debug("Finding element: {}", locator);
        try {
            driver.findElement(locator);
        } catch (NoSuchElementException e) {
            logger.error("Element not found: {}", locator);
            throw e;
        }
    }

    @Override
    public List<String> findElements(By locator) {
        logger.debug("Finding elements: {}", locator);
        try {
            return driver.findElements(locator).stream()
                    .map(el -> el.getText())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error finding elements: {}", locator, e);
            return List.of();
        }
    }

    @Override
    public void click(By locator) {
        logger.debug("Clicking element: {}", locator);
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            element.click();
            logger.info("Clicked: {}", locator);
        } catch (TimeoutException e) {
            logger.error("Element not clickable within timeout: {}", locator);
            takeScreenshot("click-timeout");
            throw e;
        }
    }

    @Override
    public void sendKeys(By locator, String text) {
        logger.debug("Sending keys to {}: {}", locator, "***");
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            element.clear();
            element.sendKeys(text);
            logger.info("Text sent to: {}", locator);
        } catch (Exception e) {
            logger.error("Error sending keys to: {}", locator, e);
            takeScreenshot("sendkeys-error");
            throw e;
        }
    }

    @Override
    public String getText(By locator) {
        logger.debug("Getting text from: {}", locator);
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            String text = element.getText();
            logger.debug("Retrieved text: {}", text);
            return text;
        } catch (Exception e) {
            logger.error("Error getting text from: {}", locator, e);
            throw e;
        }
    }

    @Override
    public String getAttribute(By locator, String attributeName) {
        logger.debug("Getting attribute {} from: {}", attributeName, locator);
        try {
            WebElement element = driver.findElement(locator);
            String value = element.getAttribute(attributeName);
            logger.debug("Attribute {} = {}", attributeName, value);
            return value;
        } catch (Exception e) {
            logger.error("Error getting attribute from: {}", locator, e);
            throw e;
        }
    }

    @Override
    public boolean isDisplayed(By locator) {
        logger.debug("Checking if displayed: {}", locator);
        try {
            WebElement element = driver.findElement(locator);
            boolean displayed = element.isDisplayed();
            logger.debug("Element displayed: {}", displayed);
            return displayed;
        } catch (NoSuchElementException e) {
            logger.debug("Element not found, returning false");
            return false;
        }
    }

    @Override
    public void waitForElementVisible(By locator, int timeoutSeconds) {
        logger.debug("Waiting for element visible ({}s): {}", timeoutSeconds, locator);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            logger.info("Element visible: {}", locator);
        } catch (TimeoutException e) {
            logger.error("Element not visible within {} seconds: {}", timeoutSeconds, locator);
            throw e;
        }
    }

    @Override
    public void waitForElementClickable(By locator, int timeoutSeconds) {
        logger.debug("Waiting for element clickable ({}s): {}", timeoutSeconds, locator);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.elementToBeClickable(locator));
            logger.info("Element clickable: {}", locator);
        } catch (TimeoutException e) {
            logger.error("Element not clickable within {} seconds: {}", timeoutSeconds, locator);
            throw e;
        }
    }

    @Override
    public String takeScreenshot(String filename) {
        logger.debug("Taking screenshot: {}", filename);
        try {
            TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
            File srcFile = screenshotDriver.getScreenshotAs(OutputType.FILE);

            Path targetPath = Paths.get("target/screenshots", filename + ".png");
            Files.createDirectories(targetPath.getParent());
            Files.copy(srcFile.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Allure.addAttachment(filename, Files.newInputStream(targetPath));
            logger.info("Screenshot saved: {}", targetPath);
            return targetPath.toString();
        } catch (IOException e) {
            logger.error("Error taking screenshot", e);
            return null;
        }
    }

    @Override
    public Object executeScript(String script, Object... args) {
        logger.debug("Executing script");
        try {
            JavascriptExecutor executor = (JavascriptExecutor) driver;
            Object result = executor.executeScript(script, args);
            logger.debug("Script executed successfully");
            return result;
        } catch (Exception e) {
            logger.error("Error executing script", e);
            throw e;
        }
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public void quit() {
        logger.info("Quitting WebDriver");
        try {
            driver.quit();
        } catch (Exception e) {
            logger.warn("Error quitting driver", e);
        }
    }

    @Override
    public Object getUnderlyingDriver() {
        return driver;
    }
}
