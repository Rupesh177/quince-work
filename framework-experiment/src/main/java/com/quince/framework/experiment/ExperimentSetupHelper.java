package com.quince.framework.experiment;

import com.quince.framework.core.experiment.FlagProvider;
import com.quince.framework.experiment.flags.OptimizelyFlagProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

/**
 * Utility helper for setting up experiments in tests.
 * Allows pinning specific variants before test execution.
 */
public class ExperimentSetupHelper {
    private static final Logger logger = LogManager.getLogger(ExperimentSetupHelper.class);
    private final FlagProvider flagProvider;

    public ExperimentSetupHelper(FlagProvider flagProvider) {
        this.flagProvider = flagProvider;
    }

    public ExperimentSetupHelper() {
        this(new OptimizelyFlagProvider());
    }

    /**
     * Forces a specific variant via SDK for deterministic testing.
     */
    public void pinVariant(String flagKey, String userId, String variantKey) {
        flagProvider.forceVariation(flagKey, userId, variantKey);
        logger.info("Pinned variant for test: {}={}", flagKey, variantKey);
    }

    /**
     * Clears any forced variant.
     */
    public void clearForcedVariant(String flagKey, String userId) {
        flagProvider.clearForcedVariation(flagKey, userId);
        logger.info("Cleared forced variant: {}", flagKey);
    }

    /**
     * Injects Optimizely user ID cookie before page load.
     * Must be called before navigation.
     */
    public void setOptimizelyUserCookie(WebDriver driver, String userId) {
        // Cannot add cookies until driver navigates to a domain
        // Caller should navigate to base URL first, then call this
        try {
            Cookie cookie = new Cookie.Builder("optimizelyEndUserId", userId)
                    .domain(extractDomain(driver.getCurrentUrl()))
                    .build();
            driver.manage().addCookie(cookie);
            logger.info("Added Optimizely user cookie: {}", userId);
        } catch (Exception e) {
            logger.warn("Error adding user cookie", e);
        }
    }

    /**
     * Sets experiment variant cookie.
     */
    public void setCookieVariant(WebDriver driver, String flagKey, String variantKey) {
        try {
            String cookieValue = String.format("{\"%s\":\"%s\"}", flagKey, variantKey);
            Cookie cookie = new Cookie.Builder("x-experiment-variant", cookieValue)
                    .domain(extractDomain(driver.getCurrentUrl()))
                    .build();
            driver.manage().addCookie(cookie);
            logger.info("Added variant cookie: {}={}", flagKey, variantKey);
        } catch (Exception e) {
            logger.warn("Error adding variant cookie", e);
        }
    }

    /**
     * Injects JavaScript variable for variant detection.
     */
    public void injectJsVariant(WebDriver driver, String flagKey, String variantKey) {
        try {
            String script = String.format(
                    "window.__EXPERIMENT__ = window.__EXPERIMENT__ || {}; " +
                            "window.__EXPERIMENT__['%s'] = '%s';",
                    flagKey, variantKey
            );
            org.openqa.selenium.JavascriptExecutor jsExecutor =
                    (org.openqa.selenium.JavascriptExecutor) driver;
            jsExecutor.executeScript(script);
            logger.info("Injected JS variant: {}={}", flagKey, variantKey);
        } catch (Exception e) {
            logger.warn("Error injecting JS variant", e);
        }
    }

    private String extractDomain(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return "localhost";
        }
    }
}