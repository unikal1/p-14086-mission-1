package com.ll.simpledb;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SimpleDataSource extends SimpleConfig implements DataSource {

    private final AtomicBoolean isShutDown = new AtomicBoolean(false);
    private volatile PrintWriter logWriter;

    public SimpleDataSource(String username, String password, String jdbcUrl) {
        super(username, password, jdbcUrl);
    }


    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return DataSource.super.createConnectionBuilder();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if(this.isClosed()) {
            throw new SQLException("SimpleDataSource " + this + " has been closed");
        }

        if(getLoginTimeout() >= 0) {
            DriverManager.setLoginTimeout(getLoginTimeout());
        }
        return DriverManager.getConnection(getJdbcUrl(), buildProps(getUsername(), getPassword()));
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if(this.isClosed()) {
            throw new SQLException("SimpleDataSource " + this + " has been closed");
        }

        if(getLoginTimeout() >= 0) {
            DriverManager.setLoginTimeout(getLoginTimeout());
        }
        return DriverManager.getConnection(getJdbcUrl(), buildProps(username, password));

    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        setLoginTimeoutSeconds(Math.max(0, seconds));
        DriverManager.setLoginTimeout(getLoginTimeoutSeconds());
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getLoginTimeoutSeconds();
    }

    @Override
    public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
        return DataSource.super.createShardingKeyBuilder();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }



    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if(iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("not a wrapper");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public boolean isClosed() {
        return this.isShutDown.get();
    }

    private Properties buildProps(String username, String password) {
        Properties props = new Properties();
        if (username != null) props.setProperty("user", username);
        if (password != null) props.setProperty("password", password);

        long ms = getConnectionTimeout();
        if (ms > 0) {
            props.setProperty("connectTimeout", String.valueOf(ms));
        }
        return props;
    }
}
