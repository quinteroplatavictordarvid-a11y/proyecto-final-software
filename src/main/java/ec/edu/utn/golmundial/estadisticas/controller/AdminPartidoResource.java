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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

@Path("/admin/partidos")
public class AdminPartidoResource {

    private static final String UTNGOLCOIN_LIQUIDACION_URL = "https://localhost:7002/api/liquidaciones/partidos/";

    /**
     * POST /admin/partidos -> crear partido si el calendario se amplia.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearPartido(String body) {
        Integer numeroPartidoFifa = extractJsonInt(body, "numeroPartidoFifa");
        String fase = extractJsonString(body, "fase");
        String grupo = extractJsonString(body, "grupo");
        Integer seleccionLocalId = extractJsonInt(body, "seleccionLocalId");
        Integer seleccionVisitanteId = extractJsonInt(body, "seleccionVisitanteId");
        String fechaHoraUtc = extractJsonString(body, "fechaHoraUtc");
        Integer sedeId = extractJsonInt(body, "sedeId");
        Integer usuarioAdminId = extractJsonInt(body, "usuarioAdminId");

        if (fase == null || seleccionLocalId == null || seleccionVisitanteId == null
                || fechaHoraUtc == null || sedeId == null) {
            return Response.status(400)
                .entity("{\"success\":false,\"message\":\"fase, seleccionLocalId, seleccionVisitanteId, fechaHoraUtc y sedeId son requeridos\"}")
                .build();
        }

        String sql = "INSERT INTO partidos (numero_partido_fifa, fase_codigo, grupo_codigo, seleccion_local_id, " +
            "seleccion_visitante_id, fecha_hora_utc, sede_id, estado) VALUES (?, ?, ?, ?, ?, ?, ?, 'PROGRAMADO')";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (numeroPartidoFifa != null) stmt.setInt(1, numeroPartidoFifa); else stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setString(2, fase);
            stmt.setString(3, grupo);
            stmt.setInt(4, seleccionLocalId);
            stmt.setInt(5, seleccionVisitanteId);
            stmt.setString(6, fechaHoraUtc);
            stmt.setInt(7, sedeId);
            stmt.executeUpdate();

            int nuevoId = -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) nuevoId = keys.getInt(1);
            }

            AuditoriaHelper.registrar(conn, usuarioAdminId, "CREAR", "PARTIDO", nuevoId,
                "Partido creado entre selecciones " + seleccionLocalId + " y " + seleccionVisitanteId);

            return Response.status(201)
                .entity("{\"success\":true,\"message\":\"Partido creado\",\"id\":" + nuevoId + "}")
                .build();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * PUT /admin/partidos/{id} -> actualizar fecha, sede o estado.
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarPartido(@PathParam("id") int id, String body) {
        String fechaHoraUtc = extractJsonString(body, "fechaHoraUtc");
        Integer sedeId = extractJsonInt(body, "sedeId");
        String estado = extractJsonString(body, "estado");
        Integer usuarioAdminId = extractJsonInt(body, "usuarioAdminId");

        StringBuilder sql = new StringBuilder("UPDATE partidos SET ");
        boolean first = true;
        if (fechaHoraUtc != null) { sql.append("fecha_hora_utc = ?"); first = false; }
        if (sedeId != null) { sql.append(first ? "" : ", ").append("sede_id = ?"); first = false; }
        if (estado != null) { sql.append(first ? "" : ", ").append("estado = ?"); first = false; }

        if (first) {
            return Response.status(400)
                .entity("{\"success\":false,\"message\":\"Debes enviar al menos un campo: fechaHoraUtc, sedeId o estado\"}")
                .build();
        }
        sql.append(" WHERE id = ?");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (fechaHoraUtc != null) stmt.setString(idx++, fechaHoraUtc);
            if (sedeId != null) stmt.setInt(idx++, sedeId);
            if (estado != null) stmt.setString(idx++, estado);
            stmt.setInt(idx, id);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                return Response.status(404)
                    .entity("{\"success\":false,\"message\":\"Partido no encontrado\"}")
                    .build();
            }

            AuditoriaHelper.registrar(conn, usuarioAdminId, "ACTUALIZAR", "PARTIDO", id, "Partido actualizado");

            return Response.ok("{\"success\":true,\"message\":\"Partido actualizado\",\"id\":" + id + "}").build();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @PUT
    @Path("/{id}/resultado")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarResultado(@PathParam("id") int id, String body) {
        try {
            Integer golesLocal = extractJsonInt(body, "golesLocal");
            Integer golesVisitante = extractJsonInt(body, "golesVisitante");
            String estado = extractJsonString(body, "estado");
            Integer usuarioAdminId = extractJsonInt(body, "usuarioAdminId");
            if (estado == null || estado.isEmpty()) estado = "FINALIZADO"; // valor por defecto -> AJUSTAR si tu tabla usa otro string

            if (golesLocal == null || golesVisitante == null) {
                return Response.status(400)
                    .entity("{\"success\":false,\"message\":\"golesLocal y golesVisitante son requeridos\"}")
                    .build();
            }

            String sql = "UPDATE partidos SET goles_local = ?, goles_visitante = ?, estado = ? WHERE id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, golesLocal);
                stmt.setInt(2, golesVisitante);
                stmt.setString(3, estado);
                stmt.setInt(4, id);
                int rows = stmt.executeUpdate();
                if (rows == 0) {
                    return Response.status(404)
                        .entity("{\"success\":false,\"message\":\"Partido no encontrado\"}")
                        .build();
                }

                AuditoriaHelper.registrar(conn, usuarioAdminId, "REGISTRAR_RESULTADO", "PARTIDO", id,
                    "Resultado: " + golesLocal + "-" + golesVisitante);
            }

            // Determinar resultado oficial según el marcador
            String resultadoOficial;
            if (golesLocal > golesVisitante) resultadoOficial = "LOCAL";
            else if (golesLocal < golesVisitante) resultadoOficial = "VISITANTE";
            else resultadoOficial = "EMPATE";

            // Notificar a UTNGolCoin para liquidar predicciones
            notificarLiquidacionUtnGolCoin(id, golesLocal, golesVisitante, resultadoOficial);

            return Response.ok("{\"success\":true,\"message\":\"Resultado actualizado\"," +
                "\"partidoId\":" + id + ",\"golesLocal\":" + golesLocal +
                ",\"golesVisitante\":" + golesVisitante + ",\"estado\":\"" + estado + "\"}").build();

        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Notifica al Backend UTNGolCoin para que liquide las predicciones del partido.
     * Si UTNGolCoin no responde, no se rompe la respuesta de este endpoint
     * (degradación controlada, sección 7.5 de la guía de coordinación).
     */
    private void notificarLiquidacionUtnGolCoin(int partidoId, int golesLocal, int golesVisitante, String resultadoOficial) {
        String liquidacionJson = "{"
            + "\"partidoId\":" + partidoId + ","
            + "\"golesLocal\":" + golesLocal + ","
            + "\"golesVisitante\":" + golesVisitante + ","
            + "\"resultadoOficial\":\"" + resultadoOficial + "\","
            + "\"fechaLiquidacionUtc\":\"" + Instant.now().toString() + "\""
            + "}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UTNGOLCOIN_LIQUIDACION_URL + partidoId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(liquidacionJson))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Liquidacion notificada a UTNGolCoin para partido " + partidoId);
            } else {
                System.err.println("UTNGolCoin respondio con codigo " + response.statusCode()
                    + " al liquidar partido " + partidoId);
            }
        } catch (Exception liquidacionError) {
            System.err.println("Error notificando a UTNGolCoin: " + liquidacionError.getMessage());
        }
    }

    private Integer extractJsonInt(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return null;
            int colon = json.indexOf(":", idx);
            int start = colon + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
            if (end == start) return null;
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) {
            return null;
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
}