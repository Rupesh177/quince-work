package com.quince.framework.experiment.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quince.framework.core.DetectionContext;
import com.quince.framework.core.VariantDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;

import java.util.Map;
import java.util.Optional;

/**
 * Detects variant from browser cookies.
 * Supports formats: plain string or JSON object.
 */
public class CookieVariantDetector implements VariantDetector {
    private static final Logger logger = LogManager.getLogger(CookieVariantDetector.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Configurable cookie names (can be overridden via properties)
    private final String[] cookieNames;

    public CookieVariantDetector(String... cookieNames) {
        this.cookieNames = cookieNames != null && cookieNames.length > 0 
            ? cookieNames 
            : new String[]{"optimizelyEndUserId", "x-experiment-variant", "quince-ab-variant"};
    }

    @Override
    public Optional<String> detect(String flagKey, DetectionContext context) {
        if (context.webDriver() == null) {
            logger.debug("No WebDriver in context, skipping cookie detection");
            return Optional.empty();
        }

        try {
            for (String cookieName : cookieNames) {
                Cookie cookie = context.webDriver().manage().getCookieNamed(cookieName);
                if (cookie != null && cookie.getValue() != null) {
                    String variant = parseCookieValue(cookieName, cookie.getValue(), flagKey);
                    if (variant != null && !variant.isBlank()) {
                        logger.info("Detected variant from cookie {}: {}", cookieName, variant);
                        return Optional.of(variant);
                    }
                }
            }
            logger.debug("No variant found in cookies for {}", flagKey);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error reading cookies", e);
            return Optional.empty();
        }
    }

    /**
     * Parses cookie value; handles both plain strings and JSON.
     */
    private String parseCookieValue(String cookieName, String value, String flagKey) {
        try {
            // Try parsing as JSON first
            if (value.startsWith("{")) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = objectMapper.readValue(value, Map.class);
                return map.get(flagKey);
            } else {
                // Plain string value
                return value;
            }
        } catch (Exception e) {
            logger.debug("Error parsing cookie {}, treating as plain value", cookieName);
            return value;
        }
    }

    @Override
    public String getName() {
        return "CookieVariantDetector";
    }
}