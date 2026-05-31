package com.quince.framework.api;

import com.quince.framework.core.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract base class for API clients using RestAssured.
 * Handles common setup: base URI, auth, logging, response validation.
 */
public abstract class BaseApiClient {
    protected static final Logger logger = LogManager.getLogger(BaseApiClient.class);

    protected final String baseUri;
    protected final RequestSpecification requestSpec;
    protected final ResponseSpecification responseSpec;

    /**
     * Constructor sets up base URI and specifications.
     */
    protected BaseApiClient(String baseUri) {
        this.baseUri = baseUri;
        this.requestSpec = buildRequestSpec();
        this.responseSpec = buildResponseSpec();
        logger.info("Initialized {} with baseUri: {}", this.getClass().getSimpleName(), baseUri);
    }

    /**
     * Builds default request specification.
     */
    protected RequestSpecification buildRequestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
    }

    /**
     * Builds default response specification.
     */
    protected ResponseSpecification buildResponseSpec() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();
    }

    /**
     * Gets a RequestSpecification configured with auth and headers.
     */
    protected RequestSpecification getAuthenticatedRequest() {
        ConfigReader config = ConfigReader.getInstance();
        String apiKey = config.get("api.key", "");

        RequestSpecification spec = RestAssured.given()
                .spec(requestSpec);

        if (!apiKey.isEmpty()) {
            spec.header("Authorization", "Bearer " + apiKey);
        }

        return spec;
    }

    /**
     * Makes a GET request and logs the response.
     */
    protected Response get(String path, Object... pathParams) {
        logger.debug("GET {}", path);
        return getAuthenticatedRequest()
                .get(path, pathParams);
    }

    /**
     * Makes a POST request with body.
     */
    protected Response post(String path, Object body) {
        logger.debug("POST {} with body: {}", path, body);
        return getAuthenticatedRequest()
                .body(body)
                .post(path);
    }

    /**
     * Makes a PUT request with body.
     */
    protected Response put(String path, Object body) {
        logger.debug("PUT {} with body: {}", path, body);
        return getAuthenticatedRequest()
                .body(body)
                .put(path);
    }

    /**
     * Makes a DELETE request.
     */
    protected Response delete(String path, Object... pathParams) {
        logger.debug("DELETE {}", path);
        return getAuthenticatedRequest()
                .delete(path, pathParams);
    }
}
