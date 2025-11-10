package com.ll.simpledb;


import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface IdxRsMapper<T> {
    T apply(ResultSet rs, int idx) throws SQLException;

}
