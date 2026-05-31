package com.quince.framework.core;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for detecting experiment variants through different signals.
 */
public interface VariantDetector {

    /**
     * Detects variant from this signal.
     *
     * @param flagKey the experiment flag key
     * @param context detection context (WebDriver, HTTP client, etc)
     * @return variant key if detected
     */
    Optional<String> detect(String flagKey, DetectionContext context);

    /**
     * Returns the name of this detector for logging/auditing.
     */
    String getName();
}