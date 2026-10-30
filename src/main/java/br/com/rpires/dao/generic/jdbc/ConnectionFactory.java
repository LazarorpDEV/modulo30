package br.com.rpires.dao.generic.jdbc;
import java.sql.*;
public final class ConnectionFactory {
    private ConnectionFactory() { }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config("db.url", "DB_URL", "jdbc:postgresql://localhost:5432/modulo30"),
            config("db.user", "DB_USER", "postgres"), config("db.password", "DB_PASSWORD", ""));
    }
    private static String config(String prop, String env, String padrao) {
        String valor = System.getProperty(prop);
        if (valor == null) valor = System.getenv(env);
        return valor == null ? padrao : valor;
    }
}
