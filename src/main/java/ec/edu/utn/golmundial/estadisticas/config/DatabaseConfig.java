package ec.edu.utn.golmundial.estadisticas.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

//    private static final String URL = "jdbc:postgresql://localhost:5432/GolMundial2026";
//    private static final String USER = "postgres";
//    private static final String PASSWORD = "postgres123";
    private static final String DEFAULT_URL = "jdbc:postgresql://sakura.proxy.rlwy.net:56826/railway?sslmode=require";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "DPGLZxXJhmelcZDYkezzaWRAgcGUpPDH";

    public static Connection getConnection() throws SQLException {
        String databaseUrlEnv = System.getenv("DATABASE_URL");
        if (databaseUrlEnv != null && !databaseUrlEnv.isBlank()) {
            try {
                String url = databaseUrlEnv.trim();
                if (url.startsWith("postgres://")) {
                    url = url.replaceFirst("postgres://", "postgresql://");
                }
                java.net.URI uri = new java.net.URI(url);
                String userInfo = uri.getUserInfo();
                String user = DEFAULT_USER;
                String password = DEFAULT_PASSWORD;
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    user = parts[0];
                    if (parts.length > 1) password = parts[1];
                }
                String host = uri.getHost();
                int port = uri.getPort();
                String db = uri.getPath();
                if (db != null && db.startsWith("/")) {
                    db = db.substring(1);
                }
                String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require", host, port, db);
                return DriverManager.getConnection(jdbcUrl, user, password);
            } catch (Exception ex) {
                System.err.println("Warning: failed to parse DATABASE_URL, falling back to defaults: " + ex.getMessage());
            }
        }
        return DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }
}