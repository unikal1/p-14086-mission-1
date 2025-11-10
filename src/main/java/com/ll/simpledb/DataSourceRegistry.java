package com.ll.simpledb;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전역적으로 DataSource 인스턴스를 등록,조회하는 레지스트리 클래스입니다.
 * 이름 기반으로 여러 DataSource를 관리하며, 기본 DataSource(KEY: "DEFAULT")를 사용하여 단일 DataSource 관리도 지원합니다.
 */
public final class DataSourceRegistry {

    // DataSource 이름과 인스턴스를 매핑하는 저장소 (스레드 안전)
    private static final Map<String, DataSource> MAP = new ConcurrentHashMap<>();

    // 기본 DataSource의 이름
    private static final String DEFAULT = "DEFAULT";

    // 유틸리티 클래스이므로 인스턴스화 방지
    private DataSourceRegistry() {}

    /**
     * 지정된 이름으로 DataSource를 등록합니다.
     *
     * @param name        등록 이름 (null 불가)
     * @param dataSource  저장할 DataSource 인스턴스 (null 불가)
     * @throws NullPointerException name 또는 dataSource 가 null 일 때
     */
    public static void register(String name, DataSource dataSource) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(dataSource);

        // 동일 이름이 이미 있을 경우 덮어씀
        MAP.put(name, dataSource);
    }

    /**
     * 기본 이름("DEFAULT")으로 DataSource를 등록합니다.
     *
     * @param dataSource 등록할 기본 DataSource
     */
    public static void register(DataSource dataSource) {
        register(DEFAULT, dataSource);
    }

    /**
     * 지정된 이름의 DataSource를 조회합니다.
     *
     * @param name DataSource 이름
     * @return 매칭되는 DataSource
     * @throws IllegalStateException 등록되지 않은 name 요청 시
     */
    public static DataSource get(String name) {
        Objects.requireNonNull(name);
        DataSource dataSource = MAP.get(name);
        if (dataSource == null) throw new IllegalStateException("unknown dataSource : " + name);
        return dataSource;
    }

    /**
     * 기본 DataSource("DEFAULT")를 조회합니다.
     *
     * @return DEFAULT DataSource
     * @throws IllegalStateException DEFAULT가 등록되지 않은 경우
     */
    public static DataSource get() {
        DataSource dataSource = MAP.get(DEFAULT);
        if (dataSource == null) throw new IllegalStateException("unknown datasource : " + DEFAULT);
        return dataSource;
    }

    /**
     * 기본 DataSource의 이름 문자열("DEFAULT")을 반환합니다.
     *
     * @return 기본 DataSource 키 값
     */
    public static String getDefault() {
        return DEFAULT;
    }
}
