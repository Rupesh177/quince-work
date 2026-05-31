package com.quince.framework.core.vault;

import com.bettercloud.vault.VaultException;

/**
 * Interface for retrieving secrets from a vault backend.
 * Implementations: HashiCorpVaultClient, EnvVarVaultClient
 */
public interface VaultClient {

    /**
     * Retrieves a secret from the vault.
     *
     * @param path vault path (e.g., "secret/data/quince/db-password")
     * @return secret value
     * @throws VaultException if retrieval fails
     */
    String getSecret(String path) throws VaultException;

    /**
     * Retrieves a secret with a default value if not found.
     */
    String getSecret(String path, String defaultValue);
}