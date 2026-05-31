package com.quince.framework.experiment;

import java.util.HashMap;
import java.util.Map;

/**
 * Context record containing experiment/variant information for a test.
 * Immutable record capturing the state of an experiment at detection time.
 */
public record ExperimentContext(
        String flagKey,
        String userId,
        String variationKey,
        boolean enabled,
        String detectionSource,
        double confidence,
        Map<String, String> allSignals,
        Map<String, Object> attributes
) {
    public ExperimentContext {
        // Validation
        if (flagKey == null || flagKey.isBlank()) {
            throw new IllegalArgumentException("flagKey cannot be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (variationKey == null || variationKey.isBlank()) {
            throw new IllegalArgumentException("variationKey cannot be null");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }

        // Defensive copy of mutable fields
        allSignals = Map.copyOf(allSignals != null ? allSignals : new HashMap<>());
        attributes = Map.copyOf(attributes != null ? attributes : new HashMap<>());
    }

    /**
     * Builder for creating ExperimentContext instances.
     */
    public static class Builder {
        private String flagKey;
        private String userId;
        private String variationKey = "control";
        private boolean enabled = true;
        private String detectionSource;
        private double confidence = 1.0;
        private final Map<String, String> allSignals = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();

        public Builder flagKey(String flagKey) {
            this.flagKey = flagKey;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder variationKey(String variationKey) {
            this.variationKey = variationKey;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder detectionSource(String detectionSource) {
            this.detectionSource = detectionSource;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder addSignal(String detectorName, String value) {
            this.allSignals.put(detectorName, value);
            return this;
        }

        public Builder addAttribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public ExperimentContext build() {
            return new ExperimentContext(
                    flagKey, userId, variationKey, enabled,
                    detectionSource, confidence, allSignals, attributes
            );
        }
    }
}