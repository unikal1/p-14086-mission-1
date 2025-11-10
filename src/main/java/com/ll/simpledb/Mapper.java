package com.ll.simpledb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;
/**
 * 객체와 Map 간의 변환을 담당하는 Jackson 기반 매퍼 유틸리티 클래스입니다.
 */
public class Mapper {

    // Jackson ObjectMapper 설정: Java Time 지원 및 날짜 포맷 옵션 조정
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Map<String, Object> 값을 지정한 타입의 객체로 변환합니다.
     *
     * @param map     변환할 Map 데이터
     * @param tClass  변환 대상 클래스 타입
     * @param <T>     변환 결과 타입
     * @return tClass 타입의 객체
     */
    public static <T> T toObj(Map<String, Object> map, Class<T> tClass) {
        return MAPPER.convertValue(map, tClass);
    }

}
