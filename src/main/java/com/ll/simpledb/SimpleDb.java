package com.ll.simpledb;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Objects;

/**
 * 단일 스레드 기준으로 Connection 을 관리하며 SQL 실행 및 트랜잭션 기능을 제공하는 간단한 DB 유틸리티입니다.
 * 생성 시 지정된 DataSource 또는 등록된 DataSource 를 기반으로 ThreadLocal 에 커넥션을 보관합니다.
 */
@RequiredArgsConstructor
public class SimpleDb implements AutoCloseable {

    /** 스레드별 Connection 보관 */
    private static final ThreadLocal<Connection> HOLDER = new ThreadLocal<>();

    /** 기본 DataSource 이름 */
    private static final String DEFAULT = "DEFAULT";

    /** 사용 중인 DataSource 이름 */
    private final String connectionName;

    /** 실제 DB 연결을 제공하는 DataSource */
    private final DataSource dataSource;


    /**
     * 새로운 DataSource 를 생성하여 등록하고, 커넥션을 ThreadLocal 에 보관합니다.
     *
     * @param cName   DataSource 이름
     * @param host    DB 호스트
     * @param user    사용자명
     * @param password 비밀번호
     * @param dbName  DB 이름
     * @throws IllegalStateException 커넥션 생성 실패 시
     */
    public SimpleDb(String cName, String host, String user, String password, String dbName) {
        this.connectionName = cName;

        DataSource newDs = ConnectionManager.createDataSource(host, user, password, dbName);
        DataSourceRegistry.register(this.connectionName, newDs);
        this.dataSource = newDs;

        try {
            Connection connection = newDs.getConnection();
            if (connection == null) throw new SQLException();
            HOLDER.set(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("cannot create connection");
        }
    }

    /**
     * 기본 이름(DEFAULT)으로 DataSource 생성 및 등록.
     *
     * @param host DB 호스트
     * @param user 사용자명
     * @param password 비밀번호
     * @param dbName DB 이름
     */
    public SimpleDb(String host, String user, String password, String dbName) {
        this(DEFAULT, host, user, password, dbName);
    }

    /**
     * 이미 등록된 DataSource 를 기반으로 SimpleDb 생성.
     *
     * @param name DataSource 이름
     * @throws IllegalArgumentException 커넥션 생성 실패 시
     */
    public SimpleDb(String name) {
        this.connectionName = name;

        try {
            this.dataSource = DataSourceRegistry.get(name);
            HOLDER.set(this.dataSource.getConnection());
        } catch (IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot create connection");
        }
    }

    /**
     * DEFAULT DataSource 를 사용하는 생성자.
     */
    public SimpleDb() {
        this(DEFAULT);
    }

    public void setDevMode(boolean mode) {
        LOGGER.mode = mode;
    }

    /**
     * SQL 실행 보조 객체(Sql) 생성.
     *
     * @return Sql 인스턴스
     * @throws IllegalArgumentException 커넥션 획득 실패 시
     */
    public Sql genSql() {
        try {
            return new Sql(conn());
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot get connection");
        }
    }

    /**
     * INSERT/UPDATE/DELETE 수행.
     *
     * @param sql 실행할 SQL
     * @return 영향받은 행 수
     * @throws IllegalArgumentException 실행 실패 시
     */
    public int run(String sql) {
        try (Statement st = conn().createStatement()) {
            return st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot run sql : " + sql);
        }
    }

    /**
     * PreparedStatement 기반 INSERT/UPDATE/DELETE.
     *
     * @param sql SQL
     * @param args 바인딩 값
     * @return 영향받은 행 수
     * @throws IllegalArgumentException 실행 실패 시
     */
    public int run(String sql, Object... args) {
        Objects.requireNonNull(args);
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                binding(ps, i + 1, args[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot run sql : " + sql);
        }
    }

    /**
     * 트랜잭션 시작 (autoCommit = false).
     *
     * @throws IllegalStateException 설정 실패 시
     */
    public void startTransaction() {
        try {
            conn().setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("cannot start transaction");
        }
    }

    /**
     * 트랜잭션 커밋.
     *
     * @throws IllegalArgumentException 실패 시
     */
    public void commit() {
        try {
            Connection c = conn();
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new IllegalArgumentException("commit() fail");
        }
    }

    /**
     * 트랜잭션 롤백.
     *
     * @throws IllegalArgumentException 실패 시
     */
    public void rollback() {
        try {
            Connection c = conn();
            c.rollback();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new IllegalArgumentException("rollback() fail");
        }
    }

    /**
     * 커넥션 종료.
     *
     * @throws IllegalStateException 실패 시
     */
    @Override
    public void close() {
        try {
            conn().close();
            HOLDER.remove();
        } catch (SQLException e) {
            throw new IllegalStateException("cannot close connection");
        }
    }

    /**
     * ThreadLocal 에 보관된 커넥션 반환.
     * 필요 시 새 커넥션을 생성하여 보관.
     *
     * @return Connection
     * @throws SQLException 커넥션 생성 실패 시
     */
    private Connection conn() throws SQLException {
        Connection c = HOLDER.get();
        if (c == null || c.isClosed()) {
            c = dataSource.getConnection();
            HOLDER.set(c);
        }
        return c;
    }

    /**
     * PreparedStatement 값 바인딩.
     *
     * @param ps PreparedStatement
     * @param idx 파라미터 인덱스
     * @param value 바인딩 값
     * @throws SQLException JDBC 설정 오류
     */
    private void binding(PreparedStatement ps, int idx, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.NULL);
        } else if (value instanceof Integer i) {
            ps.setInt(idx, i);
        } else if (value instanceof Long l) {
            ps.setLong(idx, l);
        } else if (value instanceof Boolean b) {
            ps.setBoolean(idx, b);
        } else {
            ps.setString(idx, value.toString());
        }
    }

}
