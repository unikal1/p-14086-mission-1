package com.ll.simpledb;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

/**
 * JDBC 연결에 필요한 사용자명, 비밀번호, JDBC URL, 타임아웃 등을 관리합니다.
 * <br> volatile 을 사용해 멀티 스레딩 환경에서 변수 변경에 실시간으로 대응 가능해집니다.
 */
@Getter
@Setter
public class SimpleConfig {

    /** 기본 연결 타임아웃(30초, 밀리초 단위) */
    //not used
    private static final long CONNECTION_TIMEOUT;

    /** 연결 타임아웃(밀리초). 기본값은 CONNECTION_TIMEOUT. */
    private volatile long connectionTimeout;

    /** JDBC 접속 사용자명 */
    private volatile String username;

    /** JDBC 접속 비밀번호 */
    private volatile String password;

    /**
     * DriverManager 의 loginTimeout(seconds) 에 대응하는 값.
     * 0이면 기본 설정이 사용됩니다.
     */
    private volatile int loginTimeoutSeconds = 0;

    /** JDBC URL 문자열 */
    private String jdbcUrl;

    // 정적 블록에서 타임아웃 기본값 초기화
    static {
        CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);
    }

    /**
     * SimpleConfig 생성자.
     *
     * @param username JDBC 사용자명
     * @param password JDBC 비밀번호
     * @param jdbcUrl  JDBC 연결 URL
     */
    public SimpleConfig(String username, String password, String jdbcUrl) {
        // 기본 연결 타임아웃 설정
        this.connectionTimeout = CONNECTION_TIMEOUT;

        // 인증 정보 및 URL 초기화
        this.username = username;
        this.password = password;
        this.jdbcUrl = jdbcUrl;
    }
}
