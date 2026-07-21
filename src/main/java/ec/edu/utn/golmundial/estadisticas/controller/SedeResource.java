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

@Path("/sedes")
public class SedeResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSedes() {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nombre, ciudad, pais, capacidad_aprox FROM sedes ORDER BY pais, ciudad")) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"nombre\":\"").append(rs.getString("nombre").replace("\"", "\\\"")).append("\",")
                    .append("\"ciudad\":\"").append(rs.getString("ciudad")).append("\",")
                    .append("\"pais\":\"").append(rs.getString("pais")).append("\",")
                    .append("\"capacidadAprox\":").append(rs.getInt("capacidad_aprox"))
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