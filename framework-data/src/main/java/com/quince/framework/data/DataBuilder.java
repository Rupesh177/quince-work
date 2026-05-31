package com.quince.framework.data;

import java.util.*;

/**
 * Builder pattern for constructing test data objects.
 */
public class DataBuilder {
    
    /**
     * Builder for UserData.
     */
    public static class UserDataBuilder {
        private String userId = "user_" + UUID.randomUUID();
        private String email = "user@example.com";
        private String firstName = "Test";
        private String lastName = "User";
        private String phone = "1234567890";

        public UserDataBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UserDataBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserData build() {
            return new UserData(userId, email, firstName, lastName, phone);
        }
    }

    /**
     * Builder for ProductData.
     */
    public static class ProductDataBuilder {
        private String sku = "SKU_" + UUID.randomUUID();
        private String name = "Test Product";
        private double price = 99.99;
        private String description = "Test product description";
        private int quantity = 100;

        public ProductDataBuilder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public ProductDataBuilder price(double price) {
            this.price = price;
            return this;
        }

        public ProductData build() {
            return new ProductData(sku, name, price, description, quantity);
        }
    }

    /**
     * Builder for OrderData.
     */
    public static class OrderDataBuilder {
        private String orderId = "ORD_" + UUID.randomUUID();
        private String userId = "user_1";
        private List<String> skus = List.of("SKU_1", "SKU_2");
        private double total = 199.98;
        private String status = "pending";

        public OrderDataBuilder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderDataBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OrderData build() {
            return new OrderData(orderId, userId, skus, total, status);
        }
    }

    /**
     * UserData record.
     */
    public record UserData(String userId, String email, String firstName, String lastName, String phone) {}

    /**
     * ProductData record.
     */
    public record ProductData(String sku, String name, double price, String description, int quantity) {}

    /**
     * OrderData record.
     */
    public record OrderData(String orderId, String userId, List<String> skus, double total, String status) {}
}


