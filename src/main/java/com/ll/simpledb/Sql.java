package com.ll.simpledb;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PreparedStatement 기반 SQL 빌더 및 실행 유틸리티.
 * 연결은 외부에서 주입받아 사용한다.
 */
public class Sql {
    private String query;
    private final Connection conn;

    private List<Object> args = new ArrayList<>();

    /**
     * 커넥션을 받아 SQL 실행 객체를 생성한다.
     *
     * @param conn JDBC Connection
     */
    public Sql(Connection conn) {
        this.query = "";
        this.conn = conn;
    }

    /**
     * SQL 문자열을 뒤에 이어 붙이고 바인딩 값을 추가한다.
     *
     * @param str 이어 붙일 SQL 조각
     * @param values 바인딩 값(가변)
     * @return this
     */
    public Sql append(String str, Object... values) {
        // SQL 누적 및 파라미터 누적
        query = query + " " + str;
        args.addAll(Arrays.asList(values));
        return this;
    }

    /**
     * SQL 문자열만 뒤에 이어 붙인다.
     *
     * @param str 이어 붙일 SQL 조각
     * @return this
     */
    public Sql append(String str) {
        query = query + " " + str;
        return this;
    }

    /**
     * IN (?) 형태의 플레이스홀더를 values 개수만큼 확장하여 추가한다.
     *
     * @param str IN 구문이 포함된 SQL 조각(예: "where id in (?)")
     * @param values 바인딩 값(가변)
     * @return this
     */
    public Sql appendIn(String str, Object... values) {
        return appendIn(str, Arrays.asList(values));
    }

    /**
     * IN (?) 형태의 플레이스홀더를 values 개수만큼 확장하여 추가한다.
     *
     * @param str IN 구문이 포함된 SQL 조각(예: "where id in (?)")
     * @param values 바인딩 값 목록
     * @return this
     */
    public Sql appendIn(String str, List<?> values) {
        int len = values.size();
        StringBuilder sb = new StringBuilder();

        sb.append("?, ".repeat(len));
        String replaceString = sb.substring(0, sb.length() - 2);

        str = str.replace("?", replaceString);
        query = query + " " + str;

        args.addAll(values);
        return this;
    }

    /**
     * INSERT 실행 후 생성된 키를 반환한다.
     *
     * @return 생성된 키(첫 번째 컬럼)
     * @throws IllegalStateException 실행 실패 또는 키 추출 실패 시
     */
    public long insert() {
        try (PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)){
            // 파라미터 바인딩
            for (int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new IllegalArgumentException("cannot get generated key");

        } catch (SQLException e) {
            throw new IllegalStateException("cannot insert query : " + query);
        }
    }

    /**
     * UPDATE 실행.
     *
     * @return 영향받은 행 수
     * @throws IllegalStateException 실행 실패 시
     */
    public int update() {
        try (PreparedStatement ps = conn.prepareStatement(query)){
            for (int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("cannot update query : " + query);
        }
    }

    /**
     * DELETE 실행.
     *
     * @return 영향받은 행 수
     * @throws IllegalStateException 실행 실패 시
     */
    public int delete() {
        try (PreparedStatement ps = conn.prepareStatement(query)){
            for (int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("cannot delete query : " + query);
        }
    }

    /**
     * SELECT 실행 후 행 목록을 Map 형태로 반환한다.
     *
     * @return 각 행을 컬럼라벨→값으로 담은 Map 리스트
     * @throws IllegalStateException 실행 실패 시
     */
    public List<Map<String, Object>> selectRows() {
        try (PreparedStatement ps = conn.prepareStatement(query)){
            for (int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                List<Map<String, Object>> result = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 0; i < metaData.getColumnCount(); i++) {
                        row.put(metaData.getColumnLabel(i + 1), rs.getObject(i + 1));
                    }
                    result.add(row);
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("cannot run query : " + query);
        }
    }

    /**
     * SELECT 실행 후 지정 타입으로 매핑된 객체 리스트를 반환한다.
     *
     * @param clazz 매핑 대상 타입
     * @param <T> 결과 타입
     * @return 타입 T의 리스트
     * @throws IllegalStateException 실행 실패 시
     */
    public <T> List<T> selectRows(Class<T> clazz) {
        List<Map<String, Object>> rawResult = selectRows();
        return rawResult.stream().map(map -> Mapper.toObj(map, clazz)).toList();
    }

    /**
     * 단일 행을 Map 형태로 반환한다.
     * 결과가 없으면 예외가 발생할 수 있다.
     *
     * @return 첫 번째 행의 Map
     * @throws IndexOutOfBoundsException 결과가 비어있는 경우
     */
    public Map<String, Object> selectRow() {
        return selectRows().getFirst();
    }

    /**
     * 단일 행을 지정 타입으로 매핑하여 반환한다.
     * 결과가 없으면 예외가 발생할 수 있다.
     *
     * @param clazz 매핑 대상 타입
     * @param <T> 결과 타입
     * @return 첫 번째 행의 매핑 결과
     * @throws IndexOutOfBoundsException 결과가 비어있는 경우
     */
    public <T> T selectRow(Class<T> clazz) {
        return selectRows(clazz).getFirst();
    }

    /**
     * 첫 번째 컬럼을 LocalDateTime 으로 반환한다.
     *
     * @return LocalDateTime 또는 null
     * @throws IllegalStateException 실행 실패 시
     */
    public LocalDateTime selectDatetime() {
        return select(rs -> rs.getTimestamp(1).toLocalDateTime());
    }

    /**
     * 첫 번째 컬럼을 Long 으로 반환한다.
     *
     * @return Long 또는 null
     * @throws IllegalStateException 실행 실패 시
     */
    public Long selectLong() {
        return select(rs -> rs.getLong(1));
    }

    /**
     * 첫 번째 컬럼을 String 으로 반환한다.
     *
     * @return String 또는 null
     * @throws IllegalStateException 실행 실패 시
     */
    public String selectString() {
        return select(rs -> rs.getString(1));
    }

    /**
     * 첫 번째 컬럼을 Boolean 으로 반환한다.
     *
     * @return Boolean 또는 null
     * @throws IllegalStateException 실행 실패 시
     */
    public Boolean selectBoolean() {
        return select(rs -> rs.getBoolean(1));
    }

    /**
     * 첫 번째 컬럼을 Long 리스트로 반환한다.
     *
     * @return Long 리스트(빈 리스트 가능)
     * @throws IllegalStateException 실행 실패 시
     */
    public List<Long> selectLongs() {
        return selects(rs -> rs.getLong(1));
    }

    /**
     * 단일 결과를 매퍼로 변환하여 반환한다.
     *
     * @param func ResultSet → T 매퍼
     * @param <T> 결과 타입
     * @return 매핑된 결과 또는 null
     * @throws IllegalStateException 실행 실패 시
     */
    private <T> T select(RsMapper<T> func) {
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            for (int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return func.apply(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("cannot run query : " + query);
        }
    }

    /**
     * 다중 결과를 매퍼로 변환하여 리스트로 반환한다.
     *
     * @param func ResultSet → T 매퍼
     * @param <T> 결과 타입
     * @return 매핑된 리스트(빈 리스트 가능)
     * @throws IllegalStateException 실행 실패 시
     */
    private <T> List<T> selects(RsMapper<T> func) {
        List<T> listedResult = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            for (int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listedResult.add(func.apply(rs));
                }
                return listedResult;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("cannot run query : " + query);
        }
    }

    /**
     * PreparedStatement 파라미터 바인딩.
     *
     * @param ps PreparedStatement
     * @param idx 1부터 시작하는 파라미터 인덱스
     * @param value 바인딩 값
     * @throws SQLException JDBC 설정 오류
     */
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
