package ec.edu.utn.golmundial.estadisticas.controller;

import ec.edu.utn.golmundial.estadisticas.config.DatabaseConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Path("/selecciones")
public class SeleccionResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSelecciones() {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT id, codigo_fifa, nombre, grupo_codigo, confederacion, es_anfitrion, clasificacion " +
                 "FROM selecciones ORDER BY grupo_codigo, nombre")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(filaToJson(rs));
                first = false;
            }
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
        json.append("]");
        return Response.ok(json.toString()).build();
    }
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSeleccion(@PathParam("id") int id) {
        String sql = "SELECT id, codigo_fifa, nombre, grupo_codigo, confederacion, es_anfitrion, clasificacion " +
            "FROM selecciones WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Seleccion no encontrada\"}").build();
                }
                return Response.ok(filaToJson(rs).toString()).build();
            }
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    private StringBuilder filaToJson(ResultSet rs) throws Exception {
        String clasificacion = rs.getString("clasificacion");
        StringBuilder json = new StringBuilder();
        json.append("{")
            .append("\"id\":").append(rs.getInt("id")).append(",")
            .append("\"codigoFifa\":\"").append(rs.getString("codigo_fifa")).append("\",")
            .append("\"nombre\":\"").append(rs.getString("nombre")).append("\",")
            .append("\"grupo\":\"").append(rs.getString("grupo_codigo")).append("\",")
            .append("\"confederacion\":\"").append(rs.getString("confederacion")).append("\",")
            .append("\"esAnfitrion\":").append(rs.getBoolean("es_anfitrion")).append(",")
            .append("\"clasificacion\":").append(clasificacion != null ? "\"" + clasificacion + "\"" : "null")
            .append("}");
        return json;
    }
}