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
        // Validar que ambas selecciones hayan avanzado de la fase anterior
        try (Connection connCheck = DatabaseConfig.getConnection()) {
            String errorLocal = validarAvanceSeleccion(connCheck, seleccionLocalId, fase);
            if (errorLocal != null) {
                return Response.status(400)
                    .entity("{\"success\":false,\"message\":\"Selección local inválida: " + errorLocal + "\"}")
                    .build();
            }
            String errorVisitante = validarAvanceSeleccion(connCheck, seleccionVisitanteId, fase);
            if (errorVisitante != null) {
                return Response.status(400)
                    .entity("{\"success\":false,\"message\":\"Selección visitante inválida: " + errorVisitante + "\"}")
                    .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"success\":false,\"message\":\"Error validando clasificación: " + e.getMessage() + "\"}")
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
            stmt.setObject(6, java.time.OffsetDateTime.parse(fechaHoraUtc));
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
            if (fechaHoraUtc != null) stmt.setObject(idx++, java.time.OffsetDateTime.parse(fechaHoraUtc));
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
            Integer ganadorPenalesId = extractJsonInt(body, "ganadorPenalesId");
            if (estado == null || estado.isEmpty()) estado = "FINALIZADO"; 
            if (golesLocal == null || golesVisitante == null) {
                return Response.status(400)
                    .entity("{\"success\":false,\"message\":\"golesLocal y golesVisitante son requeridos\"}")
                    .build();
            }
            if (golesLocal.equals(golesVisitante) && ganadorPenalesId == null) {
                return Response.status(400)
                    .entity("{\"success\":false,\"message\":\"Resultado empatado: debes indicar ganadorPenalesId\"}")
                    .build();
            }
            String sql = "UPDATE partidos SET goles_local = ?, goles_visitante = ?, estado = ?, ganador_penales_id = ? WHERE id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, golesLocal);
                stmt.setInt(2, golesVisitante);
                stmt.setString(3, estado);
                if (ganadorPenalesId != null) stmt.setInt(4, ganadorPenalesId); else stmt.setNull(4, java.sql.Types.INTEGER);
                stmt.setInt(5, id);
                int rows = stmt.executeUpdate();
                if (rows == 0) {
                    return Response.status(404)
                        .entity("{\"success\":false,\"message\":\"Partido no encontrado\"}")
                        .build();
                }
                AuditoriaHelper.registrar(conn, usuarioAdminId, "REGISTRAR_RESULTADO", "PARTIDO", id,
                    "Resultado: " + golesLocal + "-" + golesVisitante +
                    (ganadorPenalesId != null ? " (penales: gana seleccion " + ganadorPenalesId + ")" : ""));
            }

            // Determinar resultado oficial según el marcador (o penales si hubo empate)
            String resultadoOficial;
            if (ganadorPenalesId != null) {
                resultadoOficial = "PENALES";
            } else if (golesLocal > golesVisitante) resultadoOficial = "LOCAL";
            else if (golesLocal < golesVisitante) resultadoOficial = "VISITANTE";
            else resultadoOficial = "EMPATE";

            // Notificar a UTNGolCoin para liquidar predicciones
            notificarLiquidacionUtnGolCoin(id, golesLocal, golesVisitante, resultadoOficial);

            return Response.ok("{\"success\":true,\"message\":\"Resultado actualizado\"," +
                "\"partidoId\":" + id + ",\"golesLocal\":" + golesLocal +
                ",\"golesVisitante\":" + golesVisitante + ",\"estado\":\"" + estado +
                (ganadorPenalesId != null ? "\",\"ganadorPenalesId\":" + ganadorPenalesId : "\"") + "}").build();

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
    private String validarAvanceSeleccion(Connection conn, int seleccionId, String faseNueva) throws java.sql.SQLException {
        switch (faseNueva) {
            case "GRUPOS":
                return null; 
            case "DIECISEISAVOS":
                return validarClasificacionGrupos(conn, seleccionId);
            case "OCTAVOS":
                return validarGanoFaseAnterior(conn, seleccionId, "DIECISEISAVOS");
            case "CUARTOS":
                return validarGanoFaseAnterior(conn, seleccionId, "OCTAVOS");
            case "SEMIFINAL":
                return validarGanoFaseAnterior(conn, seleccionId, "CUARTOS");
            case "FINAL":
                return validarGanoFaseAnterior(conn, seleccionId, "SEMIFINAL");
            case "TERCER_PUESTO":
                return validarPerdioFaseAnterior(conn, seleccionId, "SEMIFINAL");
            default:
                return null;
        }
    }
    private String validarGanoFaseAnterior(Connection conn, int seleccionId, String faseAnterior) throws java.sql.SQLException {
        String sql = "SELECT seleccion_local_id, seleccion_visitante_id, goles_local, goles_visitante, ganador_penales_id " +
            "FROM partidos WHERE fase_codigo = ? AND (seleccion_local_id = ? OR seleccion_visitante_id = ?) " +
            "AND estado = 'FINALIZADO' AND goles_local IS NOT NULL AND goles_visitante IS NOT NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, faseAnterior);
            stmt.setInt(2, seleccionId);
            stmt.setInt(3, seleccionId);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean jugoLaFase = false;
                while (rs.next()) {
                    jugoLaFase = true;
                    int local = rs.getInt("seleccion_local_id");
                    int gl = rs.getInt("goles_local");
                    int gv = rs.getInt("goles_visitante");
                    Integer ganadorPenales = (Integer) rs.getObject("ganador_penales_id");
                    boolean esLocal = (local == seleccionId);

                    boolean gano;
                    if (gl == gv) {
                        gano = (ganadorPenales != null && ganadorPenales == seleccionId);
                    } else {
                        gano = esLocal ? (gl > gv) : (gv > gl);
                    }
                    if (gano) return null; // avanzó, es válido
                }
                if (!jugoLaFase) {
                    return "La selección no jugó ningún partido finalizado en la fase " + faseAnterior;
                }
                return "La selección no avanzó de la fase " + faseAnterior + " (fue eliminada)";
            }
        }
    }
    private String validarPerdioFaseAnterior(Connection conn, int seleccionId, String faseAnterior) throws java.sql.SQLException {
        String sql = "SELECT seleccion_local_id, seleccion_visitante_id, goles_local, goles_visitante, ganador_penales_id " +
            "FROM partidos WHERE fase_codigo = ? AND (seleccion_local_id = ? OR seleccion_visitante_id = ?) " +
            "AND estado = 'FINALIZADO' AND goles_local IS NOT NULL AND goles_visitante IS NOT NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, faseAnterior);
            stmt.setInt(2, seleccionId);
            stmt.setInt(3, seleccionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int local = rs.getInt("seleccion_local_id");
                    int gl = rs.getInt("goles_local");
                    int gv = rs.getInt("goles_visitante");
                    Integer ganadorPenales = (Integer) rs.getObject("ganador_penales_id");
                    boolean esLocal = (local == seleccionId);

                    boolean perdio;
                    if (gl == gv) {
                        perdio = (ganadorPenales != null && ganadorPenales != seleccionId);
                    } else {
                        perdio = esLocal ? (gl < gv) : (gv < gl);
                    }
                    if (perdio) return null; // perdió la semifinal, es válido para tercer puesto
                }
                return "La selección no perdió en " + faseAnterior + ", no corresponde a tercer puesto";
            }
        }
    }
    private String validarClasificacionGrupos(Connection conn, int seleccionId) throws java.sql.SQLException {
        String grupoSel = null;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT grupo_codigo FROM selecciones WHERE id = ?")) {
            stmt.setInt(1, seleccionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) grupoSel = rs.getString("grupo_codigo");
            }
        }
        if (grupoSel == null) return "No se pudo determinar el grupo de la selección";
        java.util.Map<Integer, int[]> stats = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, String> gruposDe = new java.util.HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, grupo_codigo FROM selecciones WHERE grupo_codigo IS NOT NULL")) {
            while (rs.next()) {
                stats.put(rs.getInt("id"), new int[6]);
                gruposDe.put(rs.getInt("id"), rs.getString("grupo_codigo"));
            }
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT seleccion_local_id, seleccion_visitante_id, goles_local, goles_visitante " +
                 "FROM partidos WHERE fase_codigo = 'GRUPOS' AND goles_local IS NOT NULL AND goles_visitante IS NOT NULL")) {
            while (rs.next()) {
                int l = rs.getInt("seleccion_local_id"), v = rs.getInt("seleccion_visitante_id");
                int gl = rs.getInt("goles_local"), gv = rs.getInt("goles_visitante");
                int[] sL = stats.get(l), sV = stats.get(v);
                if (sL == null || sV == null) continue;
                sL[0]++; sV[0]++; sL[4] += gl; sL[5] += gv; sV[4] += gv; sV[5] += gl;
                if (gl > gv) { sL[1]++; sV[3]++; } else if (gl < gv) { sV[1]++; sL[3]++; } else { sL[2]++; sV[2]++; }
            }
        }
        java.util.Map<String, java.util.List<Integer>> porGrupo = new java.util.HashMap<>();
        for (var e : gruposDe.entrySet()) {
            porGrupo.computeIfAbsent(e.getValue(), k -> new java.util.ArrayList<>()).add(e.getKey());
        }
        java.util.Comparator<Integer> cmp = (a, b) -> {
            int[] sa = stats.get(a), sb = stats.get(b);
            int ptsA = sa[1] * 3 + sa[2], ptsB = sb[1] * 3 + sb[2];
            if (ptsB != ptsA) return ptsB - ptsA;
            int dgA = sa[4] - sa[5], dgB = sb[4] - sb[5];
            if (dgB != dgA) return dgB - dgA;
            return sb[4] - sa[4];
        };
        java.util.Set<Integer> primerosYSegundos = new java.util.HashSet<>();
        java.util.List<Integer> terceros = new java.util.ArrayList<>();
        for (var lista : porGrupo.values()) {
            lista.sort(cmp);
            if (lista.size() > 0) primerosYSegundos.add(lista.get(0));
            if (lista.size() > 1) primerosYSegundos.add(lista.get(1));
            if (lista.size() > 2) terceros.add(lista.get(2));
        }
        terceros.sort(cmp);
        java.util.Set<Integer> mejoresTerceros = new java.util.HashSet<>(
            terceros.subList(0, Math.min(8, terceros.size())));
        if (primerosYSegundos.contains(seleccionId) || mejoresTerceros.contains(seleccionId)) {
            return null; // clasificó
        }
        return "La selección no clasificó a dieciseisavos (no quedó 1°, 2° de grupo ni entre los 8 mejores terceros)";
    }
    private Integer extractJsonInt(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return null;
            int colon = json.indexOf(":", idx);
            int start = colon + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            if (json.startsWith("null", start)) return null;
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
            int pos = colon + 1;
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            if (json.startsWith("null", pos)) return null;
            int start = json.indexOf("\"", pos) + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}