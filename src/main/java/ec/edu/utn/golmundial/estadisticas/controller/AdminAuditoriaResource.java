/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ec.edu.utn.golmundial.estadisticas.controller;

import ec.edu.utn.golmundial.estadisticas.config.DatabaseConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Path("/admin/auditoria")
public class AdminAuditoriaResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuditoria(@QueryParam("entidad") String entidad) {
        String sql = "SELECT id, usuario_id, accion, entidad, entidad_id, fecha_hora_utc, detalle " +
            "FROM auditoria" + (entidad != null && !entidad.isEmpty() ? " WHERE entidad = ?" : "") +
            " ORDER BY fecha_hora_utc DESC";

        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (entidad != null && !entidad.isEmpty()) {
                stmt.setString(1, entidad);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    Object usuarioId = rs.getObject("usuario_id");
                    Object entidadId = rs.getObject("entidad_id");
                    json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"usuarioId\":").append(usuarioId != null ? usuarioId.toString() : "null").append(",")
                        .append("\"accion\":\"").append(rs.getString("accion")).append("\",")
                        .append("\"entidad\":\"").append(rs.getString("entidad")).append("\",")
                        .append("\"entidadId\":").append(entidadId != null ? entidadId.toString() : "null").append(",")
                        .append("\"fechaHoraUtc\":\"").append(rs.getString("fecha_hora_utc")).append("\",")
                        .append("\"detalle\":").append(rs.getString("detalle") != null ? "\"" + rs.getString("detalle").replace("\"", "'") + "\"" : "null")
                        .append("}");
                    first = false;
                }
            }
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
        json.append("]");
        return Response.ok(json.toString()).build();
    }
}
