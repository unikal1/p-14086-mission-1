package com.ll.simpledb;


import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Objects;

@RequiredArgsConstructor
public class SimpleDb  implements AutoCloseable {
    private static final ThreadLocal<Connection> HOLDER = new ThreadLocal<>();

    private static final String DEFAULT = "DEFAULT";

    private final String connectionName;

    private final DataSource dataSource;

    @Setter
    private boolean devMode;

    public SimpleDb(String cName, String host, String user, String password, String dbName) {

        this.connectionName = cName;
        this.devMode = false;
        DataSource newDs = ConnectionManager.createDataSource(host, user, password, dbName);

        DataSourceRegistry.register(this.connectionName, newDs);
        this.dataSource = newDs;
        try {
            Connection connection = newDs.getConnection();
            if(connection == null) {
                throw new SQLException();
            }
            HOLDER.set(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("cannot create connection");
        }
    }

    public SimpleDb(String host, String user, String password, String dbName) {
        this(DEFAULT, host, user, password, dbName);
    }

    public SimpleDb(String name) {
        this.connectionName = name;
        this.devMode = false;
        try {
            this.dataSource = DataSourceRegistry.get(name);
            HOLDER.set(this.dataSource.getConnection());
        } catch (IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot create connection");
        }
    }

    public SimpleDb() {
        this(DEFAULT);
    }

    public Sql genSql() {
        try {
            return new Sql(conn());
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot get connection");
        }
    }

    //for insert, update, delete
    public int run(String sql) {
        try (Statement st = conn().createStatement()) {
            return st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot run sql : " + sql);
        }
    }

    public int run(String sql, Object... args) {
        Objects.requireNonNull(args);
        try (PreparedStatement ps = conn().prepareStatement(sql)){
            for(int i = 0; i < args.length; i++) {
                binding(ps, i + 1, args[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalArgumentException("cannot run sql : " + sql);
        }
    }

    public void startTransaction() {
        try {
            conn().setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("cannot start transaction");
        }
    }

    public void commit() {
        try {
            Connection c = conn();
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new IllegalArgumentException("commit() fail");
        }
    }

    public void rollback() {
        try {
            Connection c = conn();
            c.rollback();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new IllegalArgumentException("rollback() fail");
        }
    }


    @Override
    public void close() {
        try {
            conn().close();
        } catch (SQLException e) {
            throw new IllegalStateException("cannot close connection");
        }
    }

    private Connection conn() throws SQLException {
        Connection c = HOLDER.get();
        if(c == null || c.isClosed()) {
            c = dataSource.getConnection();
            HOLDER.set(c);
        }
        return c;
    }

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
