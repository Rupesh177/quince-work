package com.quince.framework.db;

/**
 * Exception thrown for database operation failures.
 */
public class DBException extends RuntimeException {
    public DBException(String message) {
        super(message);
    }

    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
}
