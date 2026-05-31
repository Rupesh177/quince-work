package com.quince.framework.api;

import com.quince.framework.core.config.ConfigReader;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * API client for product-related endpoints.
 */
public class ProductApiClient extends BaseApiClient {
    private static final Logger logger = LogManager.getLogger(ProductApiClient.class);

    public ProductApiClient() {
        super(ConfigReader.getInstance().get("api.base.url", "http://localhost:8080"));
    }

    /**
     * Gets product details by SKU.
     */
    public ProductResponse getProduct(String sku) {
        logger.info("Fetching product: {}", sku);
        try {
            Response response = get("/api/products/{sku}", sku)
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            return response.as(ProductResponse.class);
        } catch (Exception e) {
            logger.error("Failed to get product: {}", sku, e);
            throw new RuntimeException("Product API call failed", e);
        }
    }

    /**
     * Gets product inventory.
     */
    public InventoryResponse getInventory(String sku) {
        logger.info("Fetching inventory for: {}", sku);
        try {
            Response response = get("/api/inventory/{sku}", sku)
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            return response.as(InventoryResponse.class);
        } catch (Exception e) {
            logger.error("Failed to get inventory: {}", sku, e);
            throw new RuntimeException("Inventory API call failed", e);
        }
    }

    /**
     * Product response DTO.
     */
    public static class ProductResponse {
        public String sku;
        public String name;
        public double price;
        public String description;
        public boolean available;
    }

    /**
     * Inventory response DTO.
     */
    public static class InventoryResponse {
        public String sku;
        public int quantity;
        public String warehouse;
    }
}
