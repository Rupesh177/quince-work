package com.quince.framework.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;

/**
 * ThreadLocal registry of created test data IDs for cleanup.
 */
public class DataRegistry {
    private static final Logger logger = LogManager.getLogger(DataRegistry.class);
    private static final ThreadLocal<List<String>> idsThreadLocal = new ThreadLocal<>();
    
    private static DataService dataService;

    public static void setDataService(DataService service) {
        dataService = service;
    }

    /**
     * Registers a created data ID for cleanup.
     */
    public static void register(String id) {
        List<String> ids = idsThreadLocal.get();
        if (ids == null) {
            ids = new ArrayList<>();
            idsThreadLocal.set(ids);
        }
        ids.add(id);
        logger.debug("Registered data ID for cleanup: {}", id);
    }

    /**
     * Cleans up all registered IDs.
     */
    public static void cleanup() {
        List<String> ids = idsThreadLocal.get();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        for (String id : ids) {
            try {
                if (dataService != null) {
                    dataService.cleanup(id);
                    logger.info("Cleaned up: {}", id);
                }
            } catch (Exception e) {
                logger.warn("Error cleaning up: {}", id, e);
            }
        }
        
        idsThreadLocal.remove();
    }
}
