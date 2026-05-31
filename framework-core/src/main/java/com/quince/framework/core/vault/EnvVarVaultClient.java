package com.quince.framework.core.vault;

import com.bettercloud.vault.VaultException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Vault implementation that reads secrets from environment variables.
 * Default for local and CI environments.
 * <p>
 * Maps vault path "secret/data/quince/db-password" to env var "QUINCE_DB_PASSWORD"
 */
public class EnvVarVaultClient implements VaultClient {
    private static final Logger logger = LogManager.getLogger(EnvVarVaultClient.class);

    @Override
    public String getSecret(String path) throws VaultException {
        String envKey = pathToEnvVar(path);
        String value = System.getenv(envKey);
        if (value == null) {
            throw new VaultException("Environment variable not found: " + envKey);
        }
        logger.debug("Retrieved secret from env var: {}", envKey);
        return value;
    }

    @Override
    public String getSecret(String path, String defaultValue) {
        String envKey = pathToEnvVar(path);
        String value = System.getenv(envKey);
        if (value == null) {
            logger.warn("Secret not found: {}, using default", envKey);
            return defaultValue;
        }
        return value;
    }

    /**
     * Converts vault path to environment variable name.
     * Example: "secret/data/quince/db-password" -> "QUINCE_DB_PASSWORD"
     */
    private String pathToEnvVar(String path) {
        return "QUINCE_" + path
                .replace("secret/data/", "")
                .replace("secret/", "")
                .replace("/", "_")
                .replace("-", "_")
                .toUpperCase();
    }
}