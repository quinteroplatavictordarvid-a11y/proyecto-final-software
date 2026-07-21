package ec.edu.utn.golmundial.estadisticas.controller;

import ec.edu.utn.golmundial.estadisticas.config.DatabaseConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Path("/grupos")
public class GrupoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGrupos() {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT codigo, nombre FROM grupos ORDER BY codigo")) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"codigo\":\"").append(rs.getString("codigo")).append("\",")
                    .append("\"nombre\":\"").append(rs.getString("nombre")).append("\"")
                    .append("}");
                first = false;
            }
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
        json.append("]");
        return Response.ok(json.toString()).build();
    }
}