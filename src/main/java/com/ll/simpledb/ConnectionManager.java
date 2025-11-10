package com.ll.simpledb;

import javax.sql.DataSource;

/**
 * JDBC 연결 정보 생성 및 DataSource 생성을 담당하는 유틸리티 클래스입니다.
 * <p>호스트, DB 이름 등으로부터 JDBC URL을 생성하며, 이를 기반으로 {@link SimpleDataSource} 인스턴스를 반환합니다.</p>
 */
public class ConnectionManager {

    /**
     * 주어진 호스트와 DB 이름을 이용하여 JDBC URL 문자열을 생성합니다.
     *
     * @param host   MySQL 서버 주소
     * @param dbName 연결할 데이터베이스 이름
     * @return 생성된 JDBC URL
     */
    public static String getJdbcUrl(String host, String dbName) {
        // StringBuilder 사용: 문자열 결합 성능 확보
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:mysql://").append(host);

        // DB 이름이 존재할 때만 URL 뒤에 추가
        if (dbName != null && !dbName.isBlank()) {
            sb.append("/").append(dbName);
        }

        return sb.toString();
    }

    /**
     * SimpleDataSource 기반의 DataSource 인스턴스를 생성합니다.
     * {@link #getJdbcUrl(String, String)} 로 생성한 JDBC URL을 사용합니다.
     *
     * @param host     MySQL 서버 주소
     * @param user     로그인 사용자명
     * @param password 비밀번호
     * @param dbName   데이터베이스 이름
     * @return 생성된 DataSource 구현체 ({@link SimpleDataSource})
     */
    public static DataSource createDataSource(String host, String user, String password, String dbName) {
        // JDBC URL 생성
        String jdbcUrl = getJdbcUrl(host, dbName);

        // SimpleDataSource 생성하여 반환
        SimpleDataSource dataSource = new SimpleDataSource(user, password, jdbcUrl);
        return dataSource;
    }
}