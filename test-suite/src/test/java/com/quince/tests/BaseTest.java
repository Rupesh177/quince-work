package com.quince.tests;

import com.quince.framework.core.driver.DriverManager;
import com.quince.framework.core.driver.WaitManager;
import com.quince.framework.core.config.ConfigReader;
import com.quince.framework.ui.driver.DriverFactory;
import com.quince.framework.experiment.detection.VariantResolver;
import com.quince.framework.data.DataRegistry;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;

import java.util.UUID;

/**
 * Base class for all test classes.
 * Handles driver initialization, variant resolution, cleanup.
 */
public class BaseTest {
    protected static final Logger logger = LogManager.getLogger(BaseTest.class);
    
    protected VariantResolver variantResolver;
    protected String testUserId;
    protected ConfigReader config;

    @BeforeSuite
    public void beforeSuite() {
        logger.info("========== TEST SUITE START ==========");
        logger.info("Environment: {}", ConfigReader.getInstance().getEnvironment());
        logger.info("Browser: {}", ConfigReader.getInstance().get("browser", "chrome"));
        logger.info("Parallel enabled: {}", ConfigReader.getInstance().getBoolean("parallel.enabled", false));
    }

    @BeforeMethod
    public void beforeMethod(ITestContext context) {
        logger.info("========== {} ==========", context.getCurrentXmlTest().getName());
        
        this.config = ConfigReader.getInstance();
        this.testUserId = "user_" + UUID.randomUUID();
        this.variantResolver = new VariantResolver();
        
        // Initialize driver
        DriverManager.setDriver(DriverFactory.createDriver());
        WaitManager.initializeWait();
    }

    @AfterMethod
    public void afterMethod(ITestResult result) {
        try {
            DataRegistry.cleanup();
        } catch (Exception e) {
            logger.warn("Error cleaning up data", e);
        }

        try {
            if (DriverManager.isDriverInitialized()) {
                DriverManager.removeDriver();
                WaitManager.removeWait();
            }
        } catch (Exception e) {
            logger.warn("Error closing driver", e);
        }

        try {
            if (variantResolver != null) {
                variantResolver.shutdown();
            }
        } catch (Exception e) {
            logger.warn("Error shutting down variant resolver", e);
        }

        logger.info("Test result: {}", 
            result.isSuccess() ? "PASSED" : "FAILED");
    }

    @AfterSuite
    public void afterSuite() {
        logger.info("========== TEST SUITE END ==========");
    }
}
