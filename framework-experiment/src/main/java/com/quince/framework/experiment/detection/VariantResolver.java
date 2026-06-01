package com.quince.framework.experiment.detection;

import com.quince.framework.core.DetectionContext;
import com.quince.framework.core.VariantDetector;
import com.quince.framework.core.config.ConfigReader;
import com.quince.framework.experiment.ExperimentContext;
import com.quince.framework.experiment.flags.OptimizelyFlagProvider;
import com.quince.framework.core.experiment.FlagProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.qameta.allure.Allure;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates variant detection through multiple signals in priority order:
 * 1. Optimizely SDK (primary)
 * 2. Cookies
 * 3. HTTP Headers/API
 * 4. DOM
 * 5. Structural Heuristic
 * 
 * Logs all detection attempts and handles signal disagreements.
 */
public class VariantResolver {
    private static final Logger logger = LogManager.getLogger(VariantResolver.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final FlagProvider flagProvider;
    private final List<VariantDetector> detectors;
    private final Map<String, ExperimentContext> resolutionCache = new ConcurrentHashMap<>();
    private final Path resolutionReportPath = Paths.get("target/variant-resolution-report.json");
    private final List<Map<String, Object>> resolutionHistory = Collections.synchronizedList(new ArrayList<>());

    public VariantResolver() {
        this.flagProvider = new OptimizelyFlagProvider();
        this.detectors = List.of(
            new CookieVariantDetector(),
            new HeaderVariantDetector("http://localhost:8080"),
            new DomVariantDetector(),
            new StructuralVariantDetector()
        );
    }

    /**
     * Resolves the variant through all available signals.
     * Returns ExperimentContext with detected variant and source attribution.
     */
    public ExperimentContext resolve(String flagKey, String userId, Object webDriver) {
        String cacheKey = flagKey + ":" + userId;
        
        // Return cached result if available
        if (resolutionCache.containsKey(cacheKey)) {
            logger.debug("Returning cached variant for {}", cacheKey);
            return resolutionCache.get(cacheKey);
        }

        Map<String, String> allSignals = new HashMap<>();
        Map<String, Object> attributes = Map.of(
            "env", ConfigReader.getInstance().get("env", "local"),
            "platform", "web",
            "device", "desktop"
        );

        // Signal 1: Optimizely SDK (Primary)
        logger.info("=== Resolving variant for {} ===", flagKey);
        ExperimentContext result = tryOptimizelySDK(flagKey, userId, attributes, allSignals);
        if (result != null) {
            cacheAndReport(cacheKey, result, allSignals);
            attachToAllure(result);
            return result;
        }

        // Build detection context
        DetectionContext context = new DetectionContext(
            flagKey, userId,
            webDriver instanceof org.openqa.selenium.WebDriver ? (org.openqa.selenium.WebDriver) webDriver : null,
            new HashMap<>(), new HashMap<>(), "", attributes
        );

        // Try remaining detectors
        for (VariantDetector detector : detectors) {
            try {
                Optional<String> variant = detector.detect(flagKey, context);
                if (variant.isPresent()) {
                    allSignals.put(detector.getName(), variant.get());
                    result = new ExperimentContext.Builder()
                        .flagKey(flagKey)
                        .userId(userId)
                        .variationKey(variant.get())
                        .enabled(true)
                        .detectionSource(detector.getName())
                        .confidence(0.8)
                        .addAttribute("platform", "web")
                        .build();
                    cacheAndReport(cacheKey, result, allSignals);
                    attachToAllure(result);
                    return result;
                }
            } catch (Exception e) {
                logger.warn("Detector {} failed", detector.getName(), e);
                allSignals.put(detector.getName(), "ERROR: " + e.getMessage());
            }
        }

        // Fallback: control variant
        result = new ExperimentContext.Builder()
            .flagKey(flagKey)
            .userId(userId)
            .variationKey("control")
            .enabled(true)
            .detectionSource("default-fallback")
            .confidence(1.0)
            .addAttribute("platform", "web")
            .build();
        
        logger.error("All detection signals failed, using control variant for {}", flagKey);
        cacheAndReport(cacheKey, result, allSignals);
        attachToAllure(result);
        return result;
    }

    /**
     * Attempts detection via Optimizely SDK.
     */
    private ExperimentContext tryOptimizelySDK(String flagKey, String userId, 
                                              Map<String, Object> attributes, 
                                              Map<String, String> allSignals) {
        try {
            String variation = flagProvider.getVariation(flagKey, userId, attributes);
            boolean enabled = flagProvider.isEnabled(flagKey, userId, attributes);
            
            allSignals.put("OptimizelySDK", variation);
            
            ExperimentContext context = new ExperimentContext.Builder()
                .flagKey(flagKey)
                .userId(userId)
                .variationKey(variation)
                .enabled(enabled)
                .detectionSource("OptimizelySDK")
                .confidence(1.0)
                .addAttribute("platform", "web")
                .build();
            
            logger.info("SDK resolved variant: {} -> {}", flagKey, variation);
            return context;
        } catch (Exception e) {
            logger.warn("Optimizely SDK detection failed", e);
            allSignals.put("OptimizelySDK", "FAILED: " + e.getMessage());
            return null;
        }
    }

    /**
     * Caches result and appends to resolution history.
     */
    private void cacheAndReport(String cacheKey, ExperimentContext context, Map<String, String> allSignals) {
        resolutionCache.put(cacheKey, context);
        
        Map<String, Object> entry = new HashMap<>();
        entry.put("timestamp", System.currentTimeMillis());
        entry.put("cacheKey", cacheKey);
        entry.put("flagKey", context.flagKey());
        entry.put("userId", context.userId());
        entry.put("variant", context.variationKey());
        entry.put("source", context.detectionSource());
        entry.put("confidence", context.confidence());
        entry.put("allSignals", allSignals);
        
        resolutionHistory.add(entry);
        
        // Write to report file
        try {
            Files.createDirectories(resolutionReportPath.getParent());
            Files.writeString(resolutionReportPath, 
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resolutionHistory));
        } catch (IOException e) {
            logger.warn("Error writing variant resolution report", e);
        }
    }

    /**
     * Attaches resolution details to Allure report.
     */
    private void attachToAllure(ExperimentContext context) {
        try {
            Allure.parameter("flagKey", context.flagKey());
            Allure.parameter("detectionSource", context.detectionSource());
            Allure.parameter("confidence", String.format("%.2f", context.confidence()));
        } catch (Exception e) {
            logger.debug("Could not attach to Allure", e);
        }
    }

    /**
     * Shuts down the resolver and its providers.
     */
    public void shutdown() {
        if (flagProvider instanceof OptimizelyFlagProvider) {
            ((OptimizelyFlagProvider) flagProvider).shutdown();
        }
    }

    public String getStringVariable(String flagKey,
                                    String variableKey,
                                    String userId) {

        if (flagProvider instanceof OptimizelyFlagProvider optimizelyProvider) {
            return optimizelyProvider.getStringVariable(
                    flagKey,
                    variableKey,
                    userId
            );
        }

        throw new UnsupportedOperationException(
                "Current FlagProvider does not support variables"
        );
    }

    public Boolean getBooleanVariable(String flagKey, String variableKey, String userId) {
        if (flagProvider instanceof OptimizelyFlagProvider optimizelyProvider) {
            return optimizelyProvider.getBooleanVariable(flagKey, variableKey, userId);
        }

        throw new UnsupportedOperationException(
                "Current FlagProvider does not support boolean variables"
        );
    }

    public FlagProvider getFlagProvider() {
        return flagProvider;
    }
}