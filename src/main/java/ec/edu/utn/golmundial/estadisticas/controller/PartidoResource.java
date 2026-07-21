package ec.edu.utn.golmundial.estadisticas.controller;

import ec.edu.utn.golmundial.estadisticas.config.DatabaseConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Path("/partidos")
public class PartidoResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPartidos(
            @QueryParam("grupo") String grupo,
            @QueryParam("fase") String fase,
            @QueryParam("estado") String estado) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.id, p.numero_partido_fifa, p.fase_codigo, p.grupo_codigo, " +
            "p.seleccion_local_id, sl.codigo_fifa AS local_codigo, sl.nombre AS local_nombre, " +
            "p.seleccion_visitante_id, sv.codigo_fifa AS visitante_codigo, sv.nombre AS visitante_nombre, " +
            "p.fecha_hora_utc, p.sede_id, se.nombre AS sede_nombre, se.ciudad, se.pais, " +
            "p.estado, p.goles_local, p.goles_visitante " +
            "FROM partidos p " +
            "JOIN selecciones sl ON p.seleccion_local_id = sl.id " +
            "JOIN selecciones sv ON p.seleccion_visitante_id = sv.id " +
            "JOIN sedes se ON p.sede_id = se.id " +
            "WHERE 1=1 ");
        List<String> params = new ArrayList<>();
        if (grupo != null && !grupo.isEmpty()) {
            sql.append("AND p.grupo_codigo = ? ");
            params.add(grupo);
        }
        if (fase != null && !fase.isEmpty()) {
            sql.append("AND p.fase_codigo = ? ");
            params.add(fase);
        }
        if (estado != null && !estado.isEmpty()) {
            sql.append("AND p.estado = ? ");
            params.add(estado);
        }
        sql.append("ORDER BY p.fecha_hora_utc");
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append(filaToJson(rs));
                    first = false;
                }
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
    public Response getPartido(@PathParam("id") int id) {
        String sql = "SELECT p.id, p.numero_partido_fifa, p.fase_codigo, p.grupo_codigo, " +
            "p.seleccion_local_id, sl.codigo_fifa AS local_codigo, sl.nombre AS local_nombre, " +
            "p.seleccion_visitante_id, sv.codigo_fifa AS visitante_codigo, sv.nombre AS visitante_nombre, " +
            "p.fecha_hora_utc, p.sede_id, se.nombre AS sede_nombre, se.ciudad, se.pais, " +
            "p.estado, p.goles_local, p.goles_visitante " +
            "FROM partidos p " +
            "JOIN selecciones sl ON p.seleccion_local_id = sl.id " +
            "JOIN selecciones sv ON p.seleccion_visitante_id = sv.id " +
            "JOIN sedes se ON p.sede_id = se.id " +
            "WHERE p.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Partido no encontrado\"}").build();
                }
                return Response.ok(filaToJson(rs).toString()).build();
            }
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    private StringBuilder filaToJson(ResultSet rs) throws Exception {
        StringBuilder json = new StringBuilder();
        json.append("{")
            .append("\"id\":").append(rs.getInt("id")).append(",")
            .append("\"numeroPartidoFifa\":").append(rs.getInt("numero_partido_fifa")).append(",")
            .append("\"fase\":\"").append(rs.getString("fase_codigo")).append("\",")
            .append("\"grupo\":").append(rs.getString("grupo_codigo") != null ? "\"" + rs.getString("grupo_codigo") + "\"" : "null").append(",")
            .append("\"seleccionLocalId\":").append(rs.getInt("seleccion_local_id")).append(",")
            .append("\"seleccionLocalCodigo\":\"").append(rs.getString("local_codigo")).append("\",")
            .append("\"seleccionLocalNombre\":\"").append(rs.getString("local_nombre")).append("\",")
            .append("\"seleccionVisitanteId\":").append(rs.getInt("seleccion_visitante_id")).append(",")
            .append("\"seleccionVisitanteCodigo\":\"").append(rs.getString("visitante_codigo")).append("\",")
            .append("\"seleccionVisitanteNombre\":\"").append(rs.getString("visitante_nombre")).append("\",")
            .append("\"fechaHoraUtc\":\"").append(rs.getString("fecha_hora_utc")).append("\",")
            .append("\"sedeId\":").append(rs.getInt("sede_id")).append(",")
            .append("\"sedeNombre\":\"").append(rs.getString("sede_nombre")).append("\",")
            .append("\"ciudad\":\"").append(rs.getString("ciudad")).append("\",")
            .append("\"pais\":\"").append(rs.getString("pais")).append("\",")
            .append("\"estado\":\"").append(rs.getString("estado")).append("\",")
            .append("\"golesLocal\":").append(rs.getObject("goles_local") != null ? rs.getInt("goles_local") : "null").append(",")
            .append("\"golesVisitante\":").append(rs.getObject("goles_visitante") != null ? rs.getInt("goles_visitante") : "null")
            .append("}");
        return json;
    }
}