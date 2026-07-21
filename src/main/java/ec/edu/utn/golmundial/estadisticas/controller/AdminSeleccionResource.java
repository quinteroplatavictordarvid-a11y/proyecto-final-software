/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ec.edu.utn.golmundial.estadisticas.controller;

import ec.edu.utn.golmundial.estadisticas.config.DatabaseConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Path("/admin/selecciones")
public class AdminSeleccionResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearSeleccion(String body) {
        String codigoFifa = extractJsonString(body, "codigoFifa");
        String nombre = extractJsonString(body, "nombre");
        String grupo = extractJsonString(body, "grupo");
        String confederacion = extractJsonString(body, "confederacion");
        Boolean esAnfitrion = extractJsonBoolean(body, "esAnfitrion");
        String clasificacion = extractJsonString(body, "clasificacion");

        if (codigoFifa == null || nombre == null) {
            return Response.status(400)
                .entity("{\"success\":false,\"message\":\"codigoFifa y nombre son requeridos\"}")
                .build();
        }

        String sql = "INSERT INTO selecciones (codigo_fifa, nombre, grupo_codigo, confederacion, es_anfitrion, clasificacion) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, codigoFifa);
            stmt.setString(2, nombre);
            stmt.setString(3, grupo);
            stmt.setString(4, confederacion);
            stmt.setBoolean(5, esAnfitrion != null ? esAnfitrion : false);
            stmt.setString(6, clasificacion);
            stmt.executeUpdate();

            int nuevoId = -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) nuevoId = keys.getInt(1);
            }

            AuditoriaHelper.registrar(conn, null, "CREAR", "SELECCION", nuevoId, "Seleccion creada: " + nombre);

            return Response.status(201)
                .entity("{\"success\":true,\"message\":\"Seleccion creada\",\"id\":" + nuevoId + "}")
                .build();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarSeleccion(@PathParam("id") int id, String body) {
        String codigoFifa = extractJsonString(body, "codigoFifa");
        String nombre = extractJsonString(body, "nombre");
        String grupo = extractJsonString(body, "grupo");
        String confederacion = extractJsonString(body, "confederacion");
        Boolean esAnfitrion = extractJsonBoolean(body, "esAnfitrion");
        String clasificacion = extractJsonString(body, "clasificacion");

        String sql = "UPDATE selecciones SET codigo_fifa = ?, nombre = ?, grupo_codigo = ?, " +
            "confederacion = ?, es_anfitrion = ?, clasificacion = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigoFifa);
            stmt.setString(2, nombre);
            stmt.setString(3, grupo);
            stmt.setString(4, confederacion);
            stmt.setBoolean(5, esAnfitrion != null ? esAnfitrion : false);
            stmt.setString(6, clasificacion);
            stmt.setInt(7, id);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                return Response.status(404)
                    .entity("{\"success\":false,\"message\":\"Seleccion no encontrada\"}")
                    .build();
            }

            AuditoriaHelper.registrar(conn, null, "ACTUALIZAR", "SELECCION", id, "Seleccion actualizada: " + nombre);

            return Response.ok("{\"success\":true,\"message\":\"Seleccion actualizada\",\"id\":" + id + "}").build();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    private String extractJsonString(String json, String key) {
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

    private Boolean extractJsonBoolean(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return null;
            int colon = json.indexOf(":", idx);
            int start = colon + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            if (json.startsWith("true", start)) return true;
            if (json.startsWith("false", start)) return false;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
