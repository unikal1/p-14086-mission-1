package com.ll.simpledb;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * 간단한 JDBC {@link DataSource} 구현체입니다.
 * 기본 인증 정보와 JDBC URL을 {@link SimpleConfig}에서 상속받아 사용하며,
 * {@link DriverManager}를 통해 실제 커넥션을 반환합니다.
 * <p>커넥션 풀 없이 즉시 커넥션을 생성하며, 종료 상태를 원자적으로 관리합니다.</p>
 */
public class SimpleDataSource extends SimpleConfig implements DataSource {

    /** DataSource 종료 여부를 나타내는 플래그 */
    //AutoClosable 시 사용되는 변수이나, 사용하지 않음 - 단순 플래그
    private final AtomicBoolean isShutDown = new AtomicBoolean(false);
    private volatile PrintWriter logWriter;

    public SimpleDataSource(String username, String password, String jdbcUrl) {
        super(username, password, jdbcUrl);
    }


    /**
     * connectionBuilder 를 생성하는 팩토리 메서드. 상위 기본 구현에 위임.
     * 그러나 사용하지 않음
     * @return 만들어진 connection builder
     * @throws SQLException 실패
     */
    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return DataSource.super.createConnectionBuilder();
    }


    /**
     * 기본 인증 정보로 DB 커넥션을 생성하여 반환합니다.
     * <p>종료 상태인 경우 예외를 던지며, loginTimeout 값이 0 이상이면 {@link DriverManager#setLoginTimeout(int)}을 적용합니다.</p>
     *
     * @return 활성 {@link Connection}
     * @throws SQLException DataSource가 종료되었거나 커넥션 획득에 실패한 경우
     */
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

    /**
     * 주어진 사용자명/비밀번호로 DB 커넥션을 생성하여 반환합니다.
     * 종료 상태인 경우 예외를 던지며, loginTimeout 값이 0 이상이면 {@link DriverManager#setLoginTimeout(int)}을 적용합니다.
     *
     * @param username JDBC 사용자명
     * @param password JDBC 비밀번호
     * @return 활성 {@link Connection}
     * @throws SQLException DataSource가 종료되었거나 커넥션 획득에 실패한 경우
     */
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
