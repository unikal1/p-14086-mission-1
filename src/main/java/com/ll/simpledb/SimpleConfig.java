package com.ll.simpledb;


import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

/**
 * <br>package name   : com.ll.simpledb
 * <br>file name      : SimpleConfig
 * <br>date           : 2025-11-08
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

@Getter
@Setter
public class SimpleConfig {

    private static final long CONNECTION_TIMEOUT;
    //private static final int DEFAULT_POOL_SIZE = 10; use when need connection pool
    private volatile long connectionTimeout;
    private volatile String username;
    private volatile String password;

    private volatile int loginTimeoutSeconds = 0;
    private String jdbcUrl;

    static {
        CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);
    }

    public SimpleConfig(String username, String password, String jdbcUrl) {
        this.connectionTimeout = CONNECTION_TIMEOUT;
        this.username = username;
        this.password = password;
        this.jdbcUrl = jdbcUrl;

    }
}
