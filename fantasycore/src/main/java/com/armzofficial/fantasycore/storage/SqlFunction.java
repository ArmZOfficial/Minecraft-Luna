package com.armzofficial.fantasycore.storage;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<A, R> {
    R apply(A input) throws SQLException;
}
