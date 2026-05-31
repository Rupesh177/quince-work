package com.quince.framework.api;

import com.quince.framework.core.config.ConfigReader;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * API client for experiment/variant assignment endpoints.
 */
public class ExperimentApiClient extends BaseApiClient {
    private static final Logger logger = LogManager.getLogger(ExperimentApiClient.class);

    public ExperimentApiClient() {
        super(ConfigReader.getInstance().get("api.base.url", "http://localhost:8080"));
    }

    /**
     * Gets variant assignment for a user and flag.
     * Endpoint: GET /api/experiment/assignment?userId={userId}&flagKey={flagKey}
     */
    public AssignmentResponse getAssignment(String userId, String flagKey) {
        logger.info("Fetching assignment: userId={}, flagKey={}", userId, flagKey);
        try {
            Response response = getAuthenticatedRequest()
                    .queryParam("userId", userId)
                    .queryParam("flagKey", flagKey)
                    .get("/api/experiment/assignment")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            return response.as(AssignmentResponse.class);
        } catch (Exception e) {
            logger.error("Failed to get assignment: {}/{}", flagKey, userId, e);
            throw new RuntimeException("Assignment API call failed", e);
        }
    }

    /**
     * Gets all active experiments for a user.
     */
    public List<ExperimentResponse> getActiveExperiments(String userId) {
        logger.info("Fetching active experiments for user: {}", userId);
        try {
            Response response = getAuthenticatedRequest()
                    .queryParam("userId", userId)
                    .get("/api/experiments/active")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            ExperimentListResponse listResponse = response.as(ExperimentListResponse.class);
            return listResponse.experiments;
        } catch (Exception e) {
            logger.error("Failed to get active experiments", e);
            throw new RuntimeException("Active experiments API call failed", e);
        }
    }

    /**
     * Forces a variant (admin endpoint).
     */
    public void forceVariant(String experimentId, String userId, String variantKey) {
        logger.info("Forcing variant: exp={}, user={}, variant={}",
                experimentId, userId, variantKey);
        try {
            ForceVariantRequest request = new ForceVariantRequest(userId, variantKey);
            post("/api/experiments/{expId}/force-variant", request);
        } catch (Exception e) {
            logger.error("Failed to force variant", e);
            throw new RuntimeException("Force variant API call failed", e);
        }
    }

    /**
     * Assignment response DTO.
     */
    public static class AssignmentResponse {
        public String flagKey;
        public String userId;
        public String variant;
        public boolean enabled;
    }

    /**
     * Experiment response DTO.
     */
    public static class ExperimentResponse {
        public String id;
        public String flagKey;
        public String userId;
        public String variant;
        public boolean active;
    }

    /**
     * Experiment list response DTO.
     */
    public static class ExperimentListResponse {
        public List<ExperimentResponse> experiments;
    }

    /**
     * Force variant request DTO.
     */
    public static class ForceVariantRequest {
        public String userId;
        public String variantKey;

        public ForceVariantRequest(String userId, String variantKey) {
            this.userId = userId;
            this.variantKey = variantKey;
        }
    }
}
