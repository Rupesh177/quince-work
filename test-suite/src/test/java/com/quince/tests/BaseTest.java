package com.quince.tests;

import com.quince.framework.core.driver.DriverManager;
import com.quince.framework.core.driver.WaitManager;
import com.quince.framework.core.config.ConfigReader;
import com.quince.framework.experiment.helpers.ExperimentTestHelper;
import com.quince.framework.ui.actions.PDPActions;
import com.quince.framework.ui.driver.DriverFactory;
import com.quince.framework.experiment.detection.VariantResolver;
import com.quince.framework.data.DataRegistry;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;

import java.io.ByteArrayInputStream;
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
    protected ExperimentTestHelper experiment;
    protected PDPActions pdp;

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
        this.experiment = new ExperimentTestHelper(variantResolver);

        // Initialize driver
        DriverManager.setDriver(DriverFactory.createDriver());
        WaitManager.initializeWait();

        this.pdp = new PDPActions(DriverManager.getDriver());
    }

    @AfterMethod
    public void afterMethod(ITestResult result) {
        try {
            if (!result.isSuccess() && DriverManager.isDriverInitialized()) {

                byte[] screenshot =
                        ((TakesScreenshot) DriverManager.getDriver().getUnderlyingDriver())
                                .getScreenshotAs(OutputType.BYTES);

                Allure.addAttachment(
                        "Failure Screenshot",
                        "image/png",
                        new ByteArrayInputStream(screenshot),
                        ".png"
                );
            }
        } catch (Exception e) {
            logger.warn("Error attaching screenshot to Allure", e);
        }

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

        logger.info(
                "Test result: {}",
                result.isSuccess() ? "PASSED" : "FAILED"
        );
    }

    @AfterSuite
    public void afterSuite() {
        logger.info("========== TEST SUITE END ==========");
    }

    protected void openPDP() {
        String url = config.get("base.url") + "/product/DEMO_SKU";

        DriverManager.getDriver().get(url);
        Allure.parameter("URL", url);
    }
}
