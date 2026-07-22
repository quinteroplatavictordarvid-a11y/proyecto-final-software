/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ec.edu.utn.golmundial.estadisticas.controller;

import ec.edu.utn.golmundial.estadisticas.config.DatabaseConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mindrot.jbcrypt.BCrypt;

@Path("/auth")
public class AuthResource {
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String body) {
        try {
            String username = extractJson(body, "username");
            String password = extractJson(body, "password");
            if (username == null || password == null) {
                return Response.status(400)
                    .entity("{\"success\":false,\"message\":\"Username y password requeridos\"}")
                    .build();
            }
            String sql = "SELECT u.id, u.username, u.nombre, u.password_hash, r.nombre AS rol " +
                         "FROM usuarios u JOIN roles r ON u.rol_id = r.id " +
                         "WHERE u.username = ? AND u.activo = true";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    if (BCrypt.checkpw(password, hash)) {
                        String token = java.util.UUID.randomUUID().toString();
                        return Response.ok("{" +
                            "\"token\":\"" + token + "\"," +
                            "\"usuarioId\":" + rs.getLong("id") + "," +
                            "\"rol\":\"" + rs.getString("rol") + "\"" +
                            "}").build();
                    }
                }
                return Response.status(401)
                    .entity("{\"success\":false,\"message\":\"Credenciales incorrectas\"}")
                    .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }
    @POST
    @Path("/registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registro(String body) {
        try {
            String username = extractJson(body, "username");
            String nombre = extractJson(body, "nombre");
            String password = extractJson(body, "password");

            if (username == null || nombre == null || password == null) {
                return Response.status(400)
                    .entity("{\"success\":false,\"message\":\"username, nombre y password requeridos\"}")
                    .build();
            }
            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            String sql = "INSERT INTO usuarios(username, nombre, password_hash, rol_id) " +
                         "VALUES (?, ?, ?, (SELECT id FROM roles WHERE nombre = 'USUARIO')) " +
                         "RETURNING id";

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, nombre);
                stmt.setString(3, hash);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int newId = rs.getInt("id");
                    return Response.status(201).entity("{" +
                        "\"success\":true," +
                        "\"message\":\"Usuario registrado exitosamente\"," +
                        "\"usuarioId\":" + newId +
                        "}").build();
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("unique")) {
                return Response.status(409)
                    .entity("{\"success\":false,\"message\":\"El username ya existe\"}")
                    .build();
            }
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}")
                .build();
        }
        return Response.serverError().entity("{\"success\":false,\"message\":\"Error inesperado\"}").build();
    }
    private String extractJson(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return null;
            int colon = json.indexOf(":", idx);
            int start = json.indexOf("\"", colon) + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}