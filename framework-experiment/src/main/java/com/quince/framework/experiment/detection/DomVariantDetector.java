package com.quince.framework.experiment.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quince.framework.core.DetectionContext;
import com.quince.framework.core.VariantDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.Optional;

/**
 * Detects variant from DOM signals:
 * 1. data-variant or data-experiment attributes on root element
 * 2. meta tags
 * 3. CSS classes
 * 4. JavaScript variables
 */
public class DomVariantDetector implements VariantDetector {
    private static final Logger logger = LogManager.getLogger(DomVariantDetector.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<String> detect(String flagKey, DetectionContext context) {
        if (context.webDriver() == null) {
            logger.debug("No WebDriver in context, skipping DOM detection");
            return Optional.empty();
        }

        WebDriver driver = context.webDriver();
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

        // Try data attributes on root element
        Optional<String> dataAttrVariant = detectViaDataAttribute(jsExecutor, flagKey);
        if (dataAttrVariant.isPresent()) {
            return dataAttrVariant;
        }

        // Try meta tags
        Optional<String> metaVariant = detectViaMetaTag(jsExecutor, flagKey);
        if (metaVariant.isPresent()) {
            return metaVariant;
        }

        // Try CSS classes
        Optional<String> classVariant = detectViaCssClass(jsExecutor);
        if (classVariant.isPresent()) {
            return classVariant;
        }

        // Try JavaScript variable
        Optional<String> jsVariant = detectViaJsVariable(jsExecutor);
        if (jsVariant.isPresent()) {
            return jsVariant;
        }

        logger.debug("No variant found in DOM for {}", flagKey);
        return Optional.empty();
    }

    private Optional<String> detectViaDataAttribute(JavascriptExecutor executor, String flagKey) {
        try {
            String script = "return document.documentElement.dataset.variant;";
            Object result = executor.executeScript(script);
            if (result != null && !result.toString().isBlank()) {
                logger.info("Detected variant from data-variant attribute: {}", result);
                return Optional.of(result.toString());
            }

            script = "return document.documentElement.dataset.experiment;";
            result = executor.executeScript(script);
            if (result != null && !result.toString().isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> experiments = objectMapper.readValue(
                            result.toString(),
                            Map.class
                    );
                    String variant = experiments.get(flagKey);
                    if (variant != null && !variant.isBlank()) {
                        logger.info("Detected variant from data-experiment: {}", variant);
                        return Optional.of(variant);
                    }
                } catch (Exception e) {
                    logger.debug("Could not parse data-experiment as JSON");
                }
            }
        } catch (Exception e) {
            logger.debug("Error reading data attributes", e);
        }
        return Optional.empty();
    }

    private Optional<String> detectViaMetaTag(JavascriptExecutor executor, String flagKey) {
        try {
            String script = "return document.querySelector('meta[name=\"x-experiment-variant\"]')?.getAttribute('content');";
            Object result = executor.executeScript(script);
            if (result != null && !result.toString().isBlank()) {
                logger.info("Detected variant from meta tag: {}", result);
                return Optional.of(result.toString());
            }
        } catch (Exception e) {
            logger.debug("Error reading meta tags", e);
        }
        return Optional.empty();
    }

    private Optional<String> detectViaCssClass(JavascriptExecutor executor) {
        try {
            String script = "const classes = document.body.className; " +
                    "const match = classes.match(/variant--([\\w-]+)/); " +
                    "return match ? match[1] : null;";
            Object result = executor.executeScript(script);
            if (result != null && !result.toString().isBlank()) {
                logger.info("Detected variant from CSS class: {}", result);
                return Optional.of(result.toString());
            }
        } catch (Exception e) {
            logger.debug("Error detecting variant from CSS classes", e);
        }
        return Optional.empty();
    }

    private Optional<String> detectViaJsVariable(JavascriptExecutor executor) {
        try {
            String script = "return window.__EXPERIMENT__ ? window.__EXPERIMENT__.variant : null;";
            Object result = executor.executeScript(script);
            if (result != null && !result.toString().isBlank()) {
                logger.info("Detected variant from JS variable: {}", result);
                return Optional.of(result.toString());
            }
        } catch (Exception e) {
            logger.debug("Error reading JS variable", e);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "DomVariantDetector";
    }
}