package com.quince.framework.db;

import com.quince.framework.core.config.ConfigReader;
import com.quince.framework.core.vault.VaultClient;
import com.quince.framework.core.vault.EnvVarVaultClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * JDBC-based DBClient using HikariCP connection pooling.
 */
public class JdbcDBClient implements DBClient {
    private static final Logger logger = LogManager.getLogger(JdbcDBClient.class);
    
    private final HikariDataSource dataSource;

    /**
     * Constructor initializes connection pool from configuration.
     */
    public JdbcDBClient() {
        ConfigReader config = ConfigReader.getInstance();
        String env = config.getEnvironment();
        
        String jdbcUrl = config.get("db.jdbc.url");
        String username = config.get("db.username");
        String password = config.get("db.password");

        // Try to get password from Vault if not in plain config
        if (password == null || password.isEmpty()) {
            try {
                VaultClient vault = new EnvVarVaultClient();
                password = vault.getSecret(String.format("secret/data/quince/%s/db-password", env));
            } catch (Exception e) {
                logger.warn("Could not get password from Vault, using config value");
            }
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(10000);

        this.dataSource = new HikariDataSource(hikariConfig);
        logger.info("HikariCP connection pool initialized for: {}", jdbcUrl);
    }

    @Override
    public List<Map<String, Object>> query(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metadata = rs.getMetaData();
                int columnCount = metadata.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metadata.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            logger.debug("Query returned {} rows", results.size());
        } catch (SQLException e) {
            logger.error("Database query failed: {}", sql, e);
            throw new DBException("Query execution failed", e);
        }
        return results;
    }

    @Override
    public int update(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            int rowsAffected = stmt.executeUpdate();
            logger.info("Update completed: {} rows affected", rowsAffected);
            return rowsAffected;
        } catch (SQLException e) {
            logger.error("Database update failed: {}", sql, e);
            throw new DBException("Update execution failed", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("HikariCP connection pool closed");
        }
    }

    /**
     * Sets prepared statement parameters.
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}
