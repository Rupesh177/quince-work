package com.quince.framework.db;

import java.util.List;
import java.util.Map;

/**
 * Interface for database query execution.
 * Implementations: JdbcDBClient
 */
public interface DBClient {
    
    /**
     * Executes a SELECT query and returns results.
     * @param sql SQL query with optional ? placeholders
     * @param params query parameters
     * @return list of rows, each row as a Map<columnName, value>
     */
    List<Map<String, Object>> query(String sql, Object... params);

    /**
     * Executes INSERT/UPDATE/DELETE.
     * @return number of rows affected
     */
    int update(String sql, Object... params);

    /**
     * Closes the database connection.
     */
    void close();
}
