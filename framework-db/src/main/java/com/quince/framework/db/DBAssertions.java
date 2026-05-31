package com.quince.framework.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for database assertions in tests.
 */
public class DBAssertions {
    private static final Logger logger = LogManager.getLogger(DBAssertions.class);

    /**
     * Asserts that a row exists matching the query.
     */
    public static void assertRowExists(DBClient dbClient, String sql, Object... params) {
        List<Map<String, Object>> results = dbClient.query(sql, params);
        Assert.assertFalse(results.isEmpty(), 
            String.format("Expected at least one row for query: %s", sql));
        logger.info("Assertion passed: Row exists");
    }

    /**
     * Asserts that a specific column value matches expected.
     */
    public static void assertColumnEquals(DBClient dbClient, String sql, 
                                         String columnName, Object expected, 
                                         Object... params) {
        List<Map<String, Object>> results = dbClient.query(sql, params);
        Assert.assertFalse(results.isEmpty(), 
            String.format("No rows found for query: %s", sql));
        
        Object actual = results.get(0).get(columnName);
        Assert.assertEquals(actual, expected, 
            String.format("Column %s expected %s but was %s", columnName, expected, actual));
        logger.info("Assertion passed: {} = {}", columnName, expected);
    }

    /**
     * Asserts that a row count matches expected.
     */
    public static void assertRowCount(DBClient dbClient, String sql, 
                                      int expectedCount, Object... params) {
        List<Map<String, Object>> results = dbClient.query(sql, params);
        Assert.assertEquals(results.size(), expectedCount, 
            String.format("Expected %d rows but got %d", expectedCount, results.size()));
        logger.info("Assertion passed: Row count = {}", expectedCount);
    }
}
