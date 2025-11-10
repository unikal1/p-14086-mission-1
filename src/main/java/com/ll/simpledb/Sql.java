package com.ll.simpledb;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private String query;
    private final Connection conn;

    private List<Object> args = new ArrayList<>();

    public Sql(Connection conn) {
        this.query = "";
        this.conn = conn;
    }

    public Sql append(String str, Object... values) {
        query = query + " " + str;
        args.addAll(Arrays.asList(values));
        return this;
    }

    public Sql append(String str) {
        query = query + " " + str;
        return this;
    }

    public Sql appendIn(String str, Object... values) {
        return appendIn(str, Arrays.asList(values));
    }
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

    public long insert() {
        try (PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)){
            for(int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if(rs.next()) return rs.getLong(1);
            }
            throw new IllegalArgumentException("cannot get generated key");

        } catch (SQLException e) {
            throw new IllegalStateException("cannot insert query : " + query);
        }
    }

    public int update() {
        try (PreparedStatement ps = conn.prepareStatement(query)){
            for(int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("cannot update query : " + query);
        }
    }

    public int delete() {
        try (PreparedStatement ps = conn.prepareStatement(query)){
            for(int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("cannot delete query : " + query);
        }
    }

    public List<Map<String, Object>> selectRows() {
        try (PreparedStatement ps = conn.prepareStatement(query)){
            for(int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                List<Map<String, Object>> result = new ArrayList<>();
                while(rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for(int i = 0; i < metaData.getColumnCount(); i++) {
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

    public <T> List<T> selectRows(Class<T> clazz) {
        List<Map<String, Object>> rawResult = selectRows();
        return rawResult.stream().map(map -> Mapper.toObj(map, clazz)).toList();
    }

    public Map<String, Object> selectRow() {
        return selectRows().getFirst();
    }

    public <T> T selectRow(Class<T> clazz) {
        return selectRows(clazz).getFirst();
    }


    public LocalDateTime selectDatetime() {
        return select(rs -> rs.getTimestamp(1).toLocalDateTime());
    }

    public Long selectLong() {
        return select(rs -> rs.getLong(1));
    }

    public String selectString() {
        return select(rs -> rs.getString(1));
    }

    public Boolean selectBoolean() {
        return select(rs -> rs.getBoolean(1));
    }

    public List<Long> selectLongs() {
        return selects(rs -> rs.getLong(1));
    }


    private <T> T select(RsMapper<T> func) {
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            for(int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    return func.apply(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("cannot run query : " + query);
        }
    }

    private <T> List<T> selects(RsMapper<T> func) {
        List<T> listedResult = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            for(int i = 0; i < args.size(); i++) {
                binding(ps, i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    listedResult.add(func.apply(rs));
                }
                return listedResult;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("cannot run query : " + query);
        }
    }

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
