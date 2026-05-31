package com.quince.framework.core;

/**
 * Exception thrown when vault operations fail.
 */
public class VaultException extends RuntimeException {
    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }
}