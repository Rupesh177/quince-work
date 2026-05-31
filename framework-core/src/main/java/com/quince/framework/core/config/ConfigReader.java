package com.quince.framework.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe singleton for reading test configuration.
 * Priority: System property > Environment variable > properties file
 * <p>
 * Loads config/{env}.properties where env comes from -Denv or QUINCE_ENV or defaults to "local"
 */
public class ConfigReader {
    private static final Logger logger = LogManager.getLogger(ConfigReader.class);
    private static ConfigReader instance;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Properties properties = new Properties();
    private final String environment;

    private ConfigReader() {
        this.environment = resolveEnvironment();
        loadConfiguration();
    }

    /**
     * Returns singleton instance of ConfigReader.
     */
    public static ConfigReader getInstance() {
        if (instance == null) {
            lock.writeLock().lock();
            try {
                if (instance == null) {
                    instance = new ConfigReader();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return instance;
    }

    /**
     * Resolves the active environment from system property, env var, or default.
     */
    private String resolveEnvironment() {
        String env = System.getProperty("env");
        if (env == null || env.isBlank()) {
            env = System.getenv("QUINCE_ENV");
        }
        if (env == null || env.isBlank()) {
            env = "local";
        }
        logger.info("Active environment: {}", env);
        return env;
    }

    /**
     * Loads configuration from config/{env}.properties file.
     */
    private void loadConfiguration() {
        String configPath = String.format("config/%s.properties", environment);
        try (InputStream is = openConfig(configPath)) {
            if (is == null) {
                logger.warn("Config file not found: {}. Using defaults.", configPath);
                return;
            }
            properties.load(is);
            logger.info("Loaded {} properties from {}", properties.size(), configPath);
        } catch (IOException e) {
            logger.error("Failed to load config from {}", configPath, e);
            throw new RuntimeException("Configuration load failed", e);
        }
    }

    private InputStream openConfig(String configPath) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream classpathStream = classLoader.getResourceAsStream(configPath);
        if (classpathStream != null) {
            return classpathStream;
        }

        Path projectRelativePath = Path.of(configPath);
        if (Files.exists(projectRelativePath)) {
            return new FileInputStream(projectRelativePath.toFile());
        }

        Path testResourcesPath = Path.of("src", "test", "resources", configPath);
        if (Files.exists(testResourcesPath)) {
            return new FileInputStream(testResourcesPath.toFile());
        }

        return null;
    }

    /**
     * Gets a configuration value with priority: system property > env var > properties file > default
     */
    public String get(String key, String defaultValue) {
        lock.readLock().lock();
        try {
            // Priority 1: System property
            String value = System.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value;
            }

            // Priority 2: Environment variable (replace dots with underscores)
            String envKey = key.toUpperCase().replace(".", "_");
            value = System.getenv(envKey);
            if (value != null && !value.isBlank()) {
                return value;
            }

            // Priority 3: Properties file
            value = properties.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value;
            }

            // Priority 4: Default
            return defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a configuration value without default; throws if not found.
     */
    public String get(String key) {
        lock.readLock().lock();
        try {
            String value = System.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value;
            }

            String envKey = key.toUpperCase().replace(".", "_");
            value = System.getenv(envKey);
            if (value != null && !value.isBlank()) {
                return value;
            }

            value = properties.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value;
            }

            throw new IllegalArgumentException("Config key not found: " + key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a boolean configuration value.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets an integer configuration value.
     */
    public int getInt(String key, int defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key {}: {}", key, value);
            return defaultValue;
        }
    }

    public String getEnvironment() {
        return environment;
    }
}
