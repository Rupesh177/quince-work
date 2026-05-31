package com.quince.framework.data;

/**
 * Interface for test data generation and management.
 */
public interface DataService {
    
    /**
     * Creates a test data object.
     * @param type the class to instantiate
     * @return generated data object
     */
    <T> T create(Class<T> type);

    /**
     * Cleans up a created resource.
     */
    void cleanup(String id);
}
