package com.quince.framework.experiment.detection;

import com.quince.framework.core.DetectionContext;
import com.quince.framework.core.VariantDetector;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

/**
 * Detects variant from HTTP response headers or API endpoints.
 */
public class HeaderVariantDetector implements VariantDetector {
    private static final Logger logger = LogManager.getLogger(HeaderVariantDetector.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final String assignmentEndpoint;

    public HeaderVariantDetector(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:8080";
        this.assignmentEndpoint = "/api/experiment/assignment";
    }

    @Override
    public Optional<String> detect(String flagKey, DetectionContext context) {
        if (context.responseHeaders() != null && !context.responseHeaders().isEmpty()) {
            String variant = context.responseHeaders().get("X-Experiment-Variant");
            if (variant != null && !variant.isBlank()) {
                logger.info("Detected variant from response header: {}", variant);
                return Optional.of(variant);
            }
        }

        // Fallback: call assignment API
        try {
            return detectViaApi(flagKey, context.userId());
        } catch (Exception e) {
            logger.debug("Error detecting variant via API", e);
            return Optional.empty();
        }
    }

    /**
     * Calls /api/experiment/assignment?userId={userId}&flagKey={flagKey}
     */
    private Optional<String> detectViaApi(String flagKey, String userId) {
        try {
            Response response = RestAssured.given()
                    .baseUri(baseUrl)
                    .queryParam("userId", userId)
                    .queryParam("flagKey", flagKey)
                    .when()
                    .get(assignmentEndpoint)
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            String variant = response.getHeader("X-Experiment-Variant");
            if (variant != null && !variant.isBlank()) {
                logger.info("Detected variant from assignment API header: {}", variant);
                return Optional.of(variant);
            }

            // Try parsing response body
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(
                        response.getBody().asString(),
                        Map.class
                );
                Object variantObj = body.get("variant");
                if (variantObj != null) {
                    String variantStr = variantObj.toString();
                    logger.info("Detected variant from assignment API body: {}", variantStr);
                    return Optional.of(variantStr);
                }
            } catch (Exception e) {
                logger.debug("Could not parse assignment API response body", e);
            }

            return Optional.empty();
        } catch (Exception e) {
            logger.debug("Assignment API call failed", e);
            return Optional.empty();
        }
    }

    @Override
    public String getName() {
        return "HeaderVariantDetector";
    }
}