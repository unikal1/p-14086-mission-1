package com.ll.simpledb;

import javax.sql.DataSource;


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
