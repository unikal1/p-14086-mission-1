package com.ll.simpledb;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <br>package name   : com.ll.simpledb
 * <br>file name      : RsMapper
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
@FunctionalInterface
interface RsMapper<T> {
    T apply(ResultSet rs) throws SQLException;
}
