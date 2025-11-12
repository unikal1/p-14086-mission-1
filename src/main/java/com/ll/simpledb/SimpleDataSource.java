package com.ll.simpledb;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 간단한 JDBC {@link DataSource} 구현체입니다.
 * 기본 인증 정보와 JDBC URL을 {@link SimpleConfig}에서 상속받아 사용하며,
 * {@link DriverManager}를 통해 실제 커넥션을 반환합니다.
 * <p>간단한 커넥션 풀을 내장하여, 최소 풀 사이즈만큼 선생성(prefill)하고,
 * 여유가 없을 때 최대 풀 사이즈까지 lazy 확장합니다.</p>
 */
public class SimpleDataSource extends SimpleConfig implements DataSource, AutoCloseable {

    /** DataSource 종료 여부를 나타내는 플래그 (shutdown 이후에는 신규 획득/반환 로직이 달라짐) */
    // AutoCloseable 에서 사용되는 플래그. 실제 close 시, 물리 커넥션 정리 여부를 결정하는 기준.
    private final AtomicBoolean isShutDown = new AtomicBoolean(false);

    /** DriverManager 와 연동되는 LogWriter (JDBC 표준 스펙) */
    private volatile PrintWriter logWriter;

    /** 커넥션 풀 큐 (반납된 커넥션을 재사용) */
    private final BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>();

    /** 현재까지 생성된 커넥션 수 (물리/프록시 1:1 생성 기준) */
    private final AtomicInteger created = new AtomicInteger(0);

    /**
     * 생성자.
     * <p>부모 {@link SimpleConfig}에 기본 설정(계정, URL, min/max/borrrowTimeout 등)을 전달하고,
     * {@code minPoolSize} 만큼 커넥션을 선생성하여 풀에 채웁니다.</p>
     */
    public SimpleDataSource(String username, String password, String jdbcUrl) {
        // username, password, jdbcUrl, min=5, max=20, borrowTimeout=3s (예시값)
        super(username, password, jdbcUrl, 5, 20, Duration.ofMillis(3000));

        LOGGER.debug("init connection pool: min pool size [" + getMinPoolSize() + "], " +
                     "max pool size [" + getMaxPoolSize() + "], " +
                     "timeout [" + getBorrowTimeout().toString() + "]");

        // 최소 풀 사이즈만큼 선차로 커넥션을 만들어 큐에 적재
        try {
            for (int i = 0; i < getMinPoolSize(); i++) {
                getNewConnection();
            }
        } catch (SQLException e) {
            // 초기화 실패는 치명적이므로 런타임 예외로 승격
            throw new IllegalStateException("connectionPool initial fail", e);
        }

        LOGGER.debug("connection pool init success [" + connectionPool.size() + "]");
    }

    /**
     * {@inheritDoc}
     * <p>JDBC 표준 디폴트에 위임. 본 구현에서는 사용하지 않음.</p>
     */
    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return DataSource.super.createConnectionBuilder();
    }

    /**
     * 기본 인증 정보(부모 설정)를 사용하여 커넥션을 획득합니다.
     * <p>종료 상태면 예외를 던지고, 큐에서 즉시/대기 획득을 시도합니다.</p>
     */
    @Override
    public Connection getConnection() throws SQLException {
        return getFromPool();
    }

    /**
     * 지정한 사용자/비밀번호로 즉시 신규 커넥션을 생성하여 반환합니다.
     * <p>이 경로는 풀 재사용이 아닌, 매 호출마다 새로운 물리 커넥션을 생성합니다.</p>
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (this.isClosed()) {
            throw new SQLException("SimpleDataSource " + this + " has been closed");
        }

        // loginTimeout(초)가 0 이상이면 DriverManager 레벨에도 전달
        if (getLoginTimeout() >= 0) {
            DriverManager.setLoginTimeout(getLoginTimeout());
        }
        return DriverManager.getConnection(getJdbcUrl(), buildProps(username, password));
    }

    /** {@inheritDoc} */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.logWriter;
    }

    /**
     * {@inheritDoc}
     * <p>DriverManager 로그 라이터도 함께 설정합니다.</p>
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
        DriverManager.setLogWriter(out);
    }

    /**
     * {@inheritDoc}
     * <p>음수 방지를 위해 0 이상으로 정규화하여 DriverManager 에 반영합니다.</p>
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        setLoginTimeoutSeconds(Math.max(0, seconds));
        DriverManager.setLoginTimeout(getLoginTimeoutSeconds());
    }

    /** {@inheritDoc} */
    @Override
    public int getLoginTimeout() throws SQLException {
        return getLoginTimeoutSeconds();
    }

    /** {@inheritDoc} — 샤딩 관련 기본 구현 위임 (사용하지 않음) */
    @Override
    public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
        return DataSource.super.createShardingKeyBuilder();
    }

    /** {@inheritDoc} — JUL 루트 로거 반환 */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    /**
     * {@inheritDoc}
     * <p>래퍼 판별: 자신이 지정 타입이면 캐스팅하여 반환</p>
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("not a wrapper");
    }

    /** {@inheritDoc} — 단순 타입 비교 */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    /** 외부에서 종료 여부를 확인하기 위한 헬퍼 */
    public boolean isClosed() {
        return this.isShutDown.get();
    }

    /**
     * 사용자/비밀번호/타임아웃 등을 Properties 로 빌드.
     * <p>드라이버별로 사용하는 키가 다를 수 있으나, 일반적으로 "user", "password", "connectTimeout"을 사용.</p>
     */
    private Properties buildProps(String username, String password) {
        Properties props = new Properties();
        if (username != null) props.setProperty("user", username);
        if (password != null) props.setProperty("password", password);

        long ms = getConnectionTimeout();
        if (ms > 0) {
            // MySQL 등 다수 드라이버가 millis 단위 connectTimeout 지원
            props.setProperty("connectTimeout", String.valueOf(ms));
        }
        return props;
    }

    /**
     * DataSource 종료 처리.
     * <p>이미 종료된 경우 재호출 시 아무 작업도 하지 않으며,
     * 풀에 남아있는 커넥션의 물리 연결을 정리 후 큐를 비웁니다.</p>
     */
    @Override
    public void close() throws Exception {
        LOGGER.info("close connection pool");
        if (isShutDown.getAndSet(true)) {
            // 이미 종료 상태였다면 재호출 무시
            return;
        }
        // 현재 큐에 담긴 커넥션의 물리 연결을 모두 종료
        for (Connection c : connectionPool) closePhysicalConnection(c);
        connectionPool.clear();
    }

    /**
     * 풀에서 커넥션을 하나 꺼내 반환한다.
     * <ol>
     *   <li>즉시 poll 시도</li>
     *   <li>없고, created &lt; max 면 신규 생성</li>
     *   <li>그래도 없으면 borrowTimeout 동안 대기</li>
     * </ol>
     * <p>어떠한 경로로도 획득 실패 시 SQLException 발생.</p>
     */
    private Connection getFromPool() throws SQLException {

        if (isShutDown.get()) throw new SQLException("closed pool");

        // 1) 즉시 꺼낼 수 있으면 반환
        Connection c = connectionPool.poll();
        if(c != null) LOGGER.debug("[" + Thread.currentThread().getName() + "] get connection from pool");

        // 2) 없으면 생성 여지가 있는지 확인 (이중 체크 + 동기화)
        if (c == null) {
            if (created.get() < getMaxPoolSize()) {
                synchronized (this) {
                    if (created.get() < getMaxPoolSize()) {
                        c = getNewConnection();
                    }
                }
            }
            // 3) 생성 여지도 없거나 경합으로 실패한 경우 -> 대기 획득
            if (c == null) {
                try {
                    c = connectionPool.poll(getBorrowTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    LOGGER.debug("[" + Thread.currentThread().getName() + "] until rented connection close and return...");
                } catch (InterruptedException e) {
                    // 인터럽트 전파 + SQLException 래핑
                    LOGGER.warn("wait connection from pool until connection timeout");
                    Thread.currentThread().interrupt();
                    throw new SQLException(e);
                }
            }
        }

        if (c == null) throw new SQLException("cannot pool Connection from CP");
        return c;
    }

    /**
     * 새로운 물리 커넥션을 생성하고 프록시로 래핑하여 풀에 적재한다.
     * <p>주의: created 카운트는 물리 커넥션 생성 성공 시 증가.</p>
     */
    private Connection getNewConnection() throws SQLException {
        // 실제 드라이버를 통해 물리 커넥션 생성
        Connection unwrapedConnection = DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword());
        created.addAndGet(1);

        LOGGER.debug("create new physical connection and add to pool, current created [" + created + "]");
        // 프록시로 감싸 close() 호출 시 반환 동작을 수행하도록 함
        Connection wrappedConnection = wrapConnection(unwrapedConnection);

        // 새로 만든 커넥션은 일단 풀에 넣어둔다 (즉시 사용될 수도 있음)
        connectionPool.offer(wrappedConnection);
        return wrappedConnection;
    }

    /**
     * 커넥션 프록시를 생성한다.
     * <ul>
     *   <li>{@code close()} 호출 시: 종료 상태면 물리 종료, 아니면 풀에 반환</li>
     *   <li>{@code isClosed()} 호출 시: DataSource 종료 상태를 우선 반환</li>
     *   <li>그 외 메서드: 실제 커넥션에 위임(InvocationTargetException 언랩)</li>
     * </ul>
     */
    private Connection wrapConnection(Connection connection) {
        InvocationHandler h = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();

                if ("close".equals(name)) {
                    // 사용자 측 close 호출 시: 종료 상태에 따라 반환/종료 분기
                    if (isShutDown.get()) {
                        closePhysicalConnection(connection);
                    } else {
                        LOGGER.debug("[" + Thread.currentThread().getName() + "] connection return to pool");
                        connectionPool.offer((Connection) proxy); // 프록시 자체를 반환
                    }
                    return null;
                } else if ("isClosed".equals(name)) {
                    // 풀 관점의 종료 상태를 우선시 (종료 후에는 항상 closed 로 간주)
                    return isShutDown.get();
                }

                try {
                    // 나머지 JDBC 메서드는 실제 커넥션으로 위임
                    return method.invoke(connection, args);
                } catch (InvocationTargetException e) {
                    // 실제 예외로 다시 던짐
                    throw e.getTargetException();
                }
            }
        };
        return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                new Class[]{Connection.class},
                h
        );
    }

    /**
     * 물리 커넥션 종료 유틸리티.
     * <p>프록시로 감싸져 있을 수 있으므로 unwrap 시도 후 close.</p>
     */
    private void closePhysicalConnection(Connection connection) {
        Connection c;
        try {
            c = connection.unwrap(Connection.class);
        } catch (SQLException e) {
            // unwrap 불가 시 원본으로 시도
            c = connection;
        }
        try {
            c.close();
        } catch (SQLException ignore) {}
    }

}
