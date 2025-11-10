package com.ll.simpledb;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet을 원하는 객체 타입으로 매핑하기 위한 함수형 인터페이스입니다.
 * 하나의 ResultSet 행(row)을 T 타입 객체로 변환시킵니다.
 *
 * @param <T> 매핑 결과 객체 타입
 */
@FunctionalInterface
interface RsMapper<T> {

    /**
     * ResultSet의 현재 행을 T 타입 객체로 변환합니다.
     *
     * @param rs JDBC ResultSet
     * @return 변환된 객체
     * @throws SQLException 컬럼 접근 중 오류가 발생한 경우
     */
    T apply(ResultSet rs) throws SQLException;
}