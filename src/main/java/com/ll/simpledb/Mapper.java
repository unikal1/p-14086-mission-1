package com.ll.simpledb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * <br>package name   : com.ll.simpledb
 * <br>file name      : Mapper
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
public class Mapper {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static Map<String, Object> toMap(Object obj) {
        return MAPPER.convertValue(obj, Map.class);
    }

    public static <T> T toObj(Map<String, Object> map, Class<T> tClass) {
        return MAPPER.convertValue(map, tClass);
    }

}
