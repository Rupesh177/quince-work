package com.quince.framework.experiment.flags;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.quince.framework.core.config.ConfigReader;
import com.quince.framework.core.experiment.FlagProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * FlagProvider implementation using Optimizely Java SDK.
 * Manages datafile polling and flag evaluation.
 */
public class OptimizelyFlagProvider implements FlagProvider {
    private static final Logger logger = LogManager.getLogger(OptimizelyFlagProvider.class);

    private final Optimizely optimizelyClient;
    private final ScheduledExecutorService datafilePoller;
    private volatile boolean initialized = false;

    public OptimizelyFlagProvider() {
        this.datafilePoller = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "OptimizelyDatafilePoller");
            t.setDaemon(true);
            return t;
        });
        this.optimizelyClient = initializeOptimizely();
    }

    @Override
    public String getStringVariable(String flagKey, String variableKey, String userId) {
        Map<String, Object> attrs = Map.of(
                "env", ConfigReader.getInstance().get("env", "local"),
                "platform", "web",
                "device", "desktop"
        );

        String value = optimizelyClient.getFeatureVariableString(
                flagKey,
                variableKey,
                userId,
                attrs
        );

        logger.info("Flag={}, Variable={}, Value={}", flagKey, variableKey, value);
        return value;
    }

    public Boolean getBooleanVariable(String flagKey, String variableKey, String userId) {
        Map<String, Object> attrs = Map.of(
                "env", ConfigReader.getInstance().get("env", "local"),
                "platform", "web",
                "device", "desktop"
        );

        Boolean value = optimizelyClient.getFeatureVariableBoolean(
                flagKey,
                variableKey,
                userId,
                attrs
        );

        logger.info("Flag={}, BooleanVariable={}, Value={}",
                flagKey,
                variableKey,
                value);

        return value;
    }

    /**
     * Initializes Optimizely SDK from environment variable SDK key.
     */
    private Optimizely initializeOptimizely() {
        try {
            String sdkKey = System.getenv("OPTIMIZELY_SDK_KEY");
            if (sdkKey == null || sdkKey.isBlank()) {
                logger.error("OPTIMIZELY_SDK_KEY environment variable not set");
                throw new IllegalStateException("OPTIMIZELY_SDK_KEY not configured");
            }
            HttpProjectConfigManager configManager = HttpProjectConfigManager.builder()
                    .withSdkKey(sdkKey)
                    .withPollingInterval(5L, TimeUnit.MINUTES)
                    .build();

            Optimizely client = Optimizely.builder()
                    .withConfigManager(configManager)  //
                    .build();

            if (client == null) {
                throw new IllegalStateException("Optimizely client initialization failed");
            }

            logger.info("Optimizely SDK initialized with key: {}", sdkKey.substring(0, 8) + "***");
            initialized = true;

            // Start datafile polling every 5 minutes
            startDatafilePolling();

            return client;
        } catch (Exception e) {
            logger.error("Failed to initialize Optimizely SDK", e);
            throw new RuntimeException("Optimizely initialization error", e);
        }
    }

    /**
     * Starts background datafile polling thread.
     */
    private void startDatafilePolling() {
        datafilePoller.scheduleAtFixedRate(() -> {
            try {
                // Optimizely SDK handles polling internally;
                // this just logs status
                logger.debug("Optimizely datafile polling cycle");
            } catch (Exception e) {
                logger.warn("Datafile polling error", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public boolean isEnabled(String flagKey, String userId, Map<String, Object> attributes) {
        if (!initialized) {
            logger.warn("Optimizely SDK not initialized, returning false");
            return false;
        }
        try {
            Map<String, Object> attrs = attributes != null ? attributes : new HashMap<>();
            System.out.println("Evaluating flag: " + flagKey + " for user: " + userId + " with attributes: " + attrs);
            boolean result = optimizelyClient.isFeatureEnabled(flagKey, userId, attrs);
            logger.debug("isEnabled({}, {}) = {}", flagKey, userId, result);
            return result;
        } catch (Exception e) {
            logger.error("Error evaluating feature flag: {}", flagKey, e);
            return false;
        }
    }

    @Override
    public String getVariation(String flagKey, String userId, Map<String, Object> attributes) {
        try {
            Map<String, Object> attrs = attributes != null ? attributes : new HashMap<>();

            OptimizelyUserContext user = optimizelyClient.createUserContext(userId, attrs);
            OptimizelyDecision decision = user.decide(flagKey);

            String variation = decision.getVariationKey();

            logger.info("Flag: {}, User: {}, Enabled: {}, Variation: {}",
                    flagKey,
                    userId,
                    decision.getEnabled(),
                    variation
            );

            return variation != null ? variation : "control";

        } catch (Exception e) {
            logger.error("Error deciding feature flag variation: {}", flagKey, e);
            return "control";
        }
    }
//@Override
//public String getVariation(String flagKey, String userId, Map<String, Object> attributes) {
//        if (!initialized) {
//            logger.warn("Optimizely SDK not initialized, returning control");
//            return "control";
//        }
//        try {
//            Map<String, Object> attrs = attributes != null ? attributes : new HashMap<>();
//            System.out.println("Valid: " + optimizelyClient.isValid());
//
//// 2. Is the flag enabled for this user?
//            System.out.println("Enabled: " + optimizelyClient.isFeatureEnabled(flagKey, userId, attrs));
//
//// 3. Then fetch the variation
//            String variation = optimizelyClient.getFeatureVariableString(flagKey, "cta_position", userId, attrs);
//            System.out.println("Value: " + variation);
//
////            String variation = optimizelyClient.getFeatureVariableString(
////                flagKey, "variant_key", userId, attrs
////            );
//
//            // If no explicit variant key, fall back to checking experiment variation

    /// /            if (variation == null || variation.isBlank()) {
    /// /                variation = String.valueOf(optimizelyClient.activate(flagKey, userId, attrs));
    /// /            }
//
//            if (variation == null || variation.isBlank()) {
//                variation = "control";
//            }
//
//            logger.debug("getVariation({}, {}) = {}", flagKey, userId, variation);
//            return variation;
//        } catch (Exception e) {
//            logger.error("Error getting feature variation: {}", flagKey, e);
//            return "control";
//        }
//    }
    @Override
    public void forceVariation(String flagKey, String userId, String variationKey) {
        logger.warn(
                "Skipping forceVariation for feature flag. flagKey={}, userId={}, requestedVariation={}. " +
                        "Feature flag variations should be resolved using decide().",
                flagKey,
                userId,
                variationKey
        );
    }

    @Override
    public void clearForcedVariation(String flagKey, String userId) {
        if (!initialized) {
            return;
        }
        try {
            optimizelyClient.setForcedVariation(flagKey, userId, null);
            logger.info("Cleared forced variation: {}/{}", flagKey, userId);
        } catch (Exception e) {
            logger.error("Error clearing forced variation", e);
        }
    }

    /**
     * Shuts down the provider and cleans up resources.
     */
    public void shutdown() {
        try {
            if (optimizelyClient != null) {
                optimizelyClient.close();
            }
            datafilePoller.shutdownNow();
            if (!datafilePoller.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Datafile poller did not terminate gracefully");
            }
        } catch (Exception e) {
            logger.error("Error shutting down Optimizely provider", e);
        }
    }
}