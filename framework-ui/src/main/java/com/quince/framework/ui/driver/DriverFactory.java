package com.quince.framework.ui.driver;

import com.quince.framework.core.config.ConfigReader;
import com.quince.framework.core.driver.UIDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory for creating UIDriver instances with healing integration.
 * Configurable via driver.type property (selenium|playwright).
 * Healenium auto-healing applied when heal.enabled=true.
 */
public class DriverFactory {
    private static final Logger logger = LogManager.getLogger(DriverFactory.class);

    /**
     * Creates a UIDriver instance based on configuration.
     */
    public static UIDriver createDriver() {
        ConfigReader config = ConfigReader.getInstance();
        String driverType = config.get("driver.type", "selenium");
        String browser = config.get("browser", "chrome");
        boolean remoteEnabled = config.getBoolean("remote.enabled", false);
        boolean healingEnabled = config.getBoolean("heal.enabled", false);

        logger.info("Creating driver: type={}, browser={}, remote={}, healing={}",
                driverType, browser, remoteEnabled, healingEnabled);

        UIDriver driver = switch (driverType.toLowerCase()) {
            case "selenium" -> createSeleniumDriver(browser, remoteEnabled);
            case "playwright" -> new PlaywrightDriver(); // Will throw UnsupportedOperationException
            default -> throw new IllegalArgumentException("Unknown driver type: " + driverType);
        };

        // Wrap with Healenium if enabled
        if (healingEnabled) {
            try {
                driver = new HealeniumDriver(driver);
                logger.info("Applied Healenium healing wrapper");
            } catch (Exception e) {
                logger.warn("Healenium wrapper could not be applied. Continuing with normal driver.", e);
            }
        }

        return driver;
    }

    /**
     * Creates a Selenium WebDriver instance.
     */
    private static UIDriver createSeleniumDriver(String browser, boolean remoteEnabled) {
        WebDriver webDriver = switch (browser.toLowerCase()) {
            case "chrome" -> createChromeDriver(remoteEnabled);
            case "firefox" -> createFirefoxDriver(remoteEnabled);
            case "edge" -> createEdgeDriver(remoteEnabled);
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };

        logger.info("Created {} WebDriver", browser);
        return new SeleniumDriver(webDriver);
    }

    /**
     * Creates ChromeDriver with standard options.
     */
    private static WebDriver createChromeDriver(boolean remoteEnabled) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        ConfigReader config = ConfigReader.getInstance();

        // Headless mode
        if (config.getBoolean("headless", false)) {
            options.addArguments("--headless=new");
            logger.info("Chrome running in headless mode");
        }

        // Standard options
        options.addArguments(
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
                "--no-sandbox",
                "--disable-gpu"
        );

        if (remoteEnabled) {
            String gridUrl = config.get("remote.url", "http://localhost:4444/wd/hub");
            logger.info("Using Selenium Grid at {}", gridUrl);
            try {
                return new RemoteWebDriver(new URL(gridUrl), options);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid remote.url: " + gridUrl, e);
            }
        }

        return new ChromeDriver(options);
    }

    /**
     * Creates FirefoxDriver with standard options.
     */
    private static WebDriver createFirefoxDriver(boolean remoteEnabled) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();

        ConfigReader config = ConfigReader.getInstance();

        if (config.getBoolean("headless", false)) {
            options.addArguments("--headless");
            logger.info("Firefox running in headless mode");
        }

        if (remoteEnabled) {
            String gridUrl = config.get("remote.url", "http://localhost:4444/wd/hub");
            logger.info("Using Selenium Grid at {}", gridUrl);
            try {
                return new RemoteWebDriver(new URL(gridUrl), options);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid remote.url: " + gridUrl, e);
            }
        }

        return new FirefoxDriver(options);
    }

    /**
     * Creates EdgeDriver with standard options.
     */
    private static WebDriver createEdgeDriver(boolean remoteEnabled) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();

        ConfigReader config = ConfigReader.getInstance();

        if (config.getBoolean("headless", false)) {
            options.addArguments("--headless=new");
            logger.info("Edge running in headless mode");
        }

        if (remoteEnabled) {
            String gridUrl = config.get("remote.url", "http://localhost:4444/wd/hub");
            logger.info("Using Selenium Grid at {}", gridUrl);
            try {
                return new RemoteWebDriver(new URL(gridUrl), options);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid remote.url: " + gridUrl, e);
            }
        }

        return new EdgeDriver(options);
    }
}
