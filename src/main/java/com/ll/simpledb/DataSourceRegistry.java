package com.ll.simpledb;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <br>package name   : com.ll.simpledb
 * <br>file name      : DataSourceRegistry
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
public final class DataSourceRegistry {
    private static final Map<String, DataSource> MAP = new ConcurrentHashMap<>();
    private static final String DEFAULT = "DEFAULT";

    private DataSourceRegistry() {}

    public static void register(String name, DataSource dataSource) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(dataSource);

        MAP.put(name, dataSource);
    }

    public static void register(DataSource dataSource) {
        try {
            register(DEFAULT, dataSource);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DEFAULT datasource already registered");
        }
    }

    public static DataSource get(String name) {
        Objects.requireNonNull(name);
        DataSource dataSource = MAP.get(name);
        if(dataSource == null) throw new IllegalStateException("unknown dataSource : " + name);
        return dataSource;
    }

    public static DataSource get() {
        DataSource dataSource = MAP.get(DEFAULT);
        if(dataSource == null) throw new IllegalStateException("unknown datasource : " + DEFAULT);
        return dataSource;
    }

    public static String getDefault() {
        return DEFAULT;
    }

}
