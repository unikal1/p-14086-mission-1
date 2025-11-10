package com.ll.simpledb;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * <br>package name   : com.ll.simpledb
 * <br>file name      : ConnectionManager
 * <br>date           : 2025-11-10
 * <pre>
 * <span style="color: white;">[description]</span>
 *
 * </pre>
 * <pre>
 * <span style="color: white;">usage:</span>
 * {@code
 *
 * } </pre>
 */
public class ConnectionManager {

    public static String getJdbcUrl(String host, String dbName) {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:mysql://").append(host);

        if (dbName != null && !dbName.isBlank()) {
            sb.append("/").append(dbName);
        }

        return sb.toString();
    }
    public static DataSource createDataSource(String host, String user, String password, String dbName) {
        String jdbcUrl = getJdbcUrl(host, dbName);
        SimpleDataSource dataSource = new SimpleDataSource(user, password, jdbcUrl);
        return dataSource;

    }
}
