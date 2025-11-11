package com.ll.simpledb;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * JDBC 연결에 필요한 기본 설정(사용자명, 비밀번호, URL, 타임아웃, 풀 크기 등)을 관리하는 구성 클래스입니다.
 * <p>
 * 이 클래스는 스레드 안전성을 높이기 위해 일부 필드에 {@code volatile}을 사용하며,
 * 런타임 중 설정 변경(예: username/password 교체)에 즉시 반영될 수 있습니다.
 */
@Getter
@Setter
public class SimpleConfig {

    /**
     * 기본 연결 타임아웃(30초).
     * 내부 DriverManager connectTimeout과는 구분되며,
     * 단순 timeout 설정 값으로만 사용됩니다.
     */
    // not used directly, but acts as a default initialization value
    private static final long CONNECTION_TIMEOUT;

    /** 연결 타임아웃(밀리초 단위). 기본값은 {@link #CONNECTION_TIMEOUT}. */
    private volatile long connectionTimeout;

    /** JDBC 접속 사용자명 (멀티 스레드에서 변경 가능성 있어 volatile 적용) */
    private volatile String username;

    /** JDBC 접속 비밀번호 (volatile 적용으로 즉시 반영됨) */
    private volatile String password;

    /**
     * JDBC DriverManager 의 loginTimeout (초 단위).
     * 이 값이 0이면 DriverManager의 기본 설정을 사용합니다.
     */
    private volatile int loginTimeoutSeconds = 0;

    /** JDBC URL 문자열 */
    private String jdbcUrl;

    /** 풀에서 유지해야 하는 최소 커넥션 수 */
    private final int minPoolSize;

    /** 풀에서 허용되는 최대 커넥션 수 */
    private final int maxPoolSize;

    /**
     * 풀에서 커넥션을 빌릴 때(c.getConnection())
     * 큐에서 빈 커넥션을 기다리는 최대 시간.
     */
    private final Duration borrowTimeout;

    /*
     * 정적 초기화 블록에서 기본 CONNECTION_TIMEOUT 값을 설정합니다.
     * (초 → 밀리초 변환)
     */
    static {
        CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);
    }

    /**
     * SimpleConfig 생성자.
     * <p>
     * DataSource 구현체(SimpleDataSource)가 필요로 하는 기본 설정을 모두 전달받습니다.
     *
     * @param username     JDBC 사용자명
     * @param password     JDBC 비밀번호
     * @param jdbcUrl      JDBC 연결 URL
     * @param minPoolSize  최소 커넥션 풀 크기(초기 prefill 개수)
     * @param maxPoolSize  최대 커넥션 풀 크기(lazy expand 제한 값)
     * @param borrowTimeout 풀에서 커넥션을 획득할 때 대기 가능한 최대 Duration
     */
    public SimpleConfig(String username,
                        String password,
                        String jdbcUrl,
                        int minPoolSize,
                        int maxPoolSize,
                        Duration borrowTimeout) {

        // 연결 타임아웃 기본값 설정 (not used in pool directly, but exposed as config)
        this.connectionTimeout = CONNECTION_TIMEOUT;

        // 인증 정보 및 URL 초기화
        this.username = username;
        this.password = password;
        this.jdbcUrl = jdbcUrl;

        // 커넥션 풀 관련 설정
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.borrowTimeout = borrowTimeout;
    }
}
