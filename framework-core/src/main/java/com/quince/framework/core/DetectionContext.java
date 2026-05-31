package com.quince.framework.core;

import org.openqa.selenium.WebDriver;
import io.restassured.RestAssured;

import java.util.HashMap;
import java.util.Map;

/**
 * Context passed to variant detectors containing all available detection signals.
 */
public record DetectionContext(
        String flagKey,
        String userId,
        WebDriver webDriver,
        Map<String, String> cookies,
        Map<String, String> responseHeaders,
        String domSnapshot,
        Map<String, Object> attributes
) {
    public DetectionContext {
        // Validation and normalization
        if (flagKey == null || flagKey.isBlank()) {
            throw new IllegalArgumentException("flagKey cannot be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
    }
}