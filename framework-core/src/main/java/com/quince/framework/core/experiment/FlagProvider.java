package com.quince.framework.core.experiment;

import java.util.Map;

/**
 * Interface for feature flag providers (Optimizely, LaunchDarkly, etc).
 */
public interface FlagProvider {
    
    /**
     * Checks if a flag is enabled for a user.
     * @param flagKey flag identifier
     * @param userId user identifier
     * @param attributes optional user attributes
     * @return true if flag is enabled
     */
    boolean isEnabled(String flagKey, String userId, Map<String, Object> attributes);

    String getStringVariable(String flagKey, String variableKey, String userId);

    /**
     * Gets the variation assigned to a user for a flag.
     * @return variation key (e.g., "control", "treatment_a")
     */
    String getVariation(String flagKey, String userId, Map<String, Object> attributes);

    /**
     * Forces a specific variation for testing purposes.
     */
    void forceVariation(String flagKey, String userId, String variationKey);

    /**
     * Clears forced variation.
     */
    void clearForcedVariation(String flagKey, String userId);
}