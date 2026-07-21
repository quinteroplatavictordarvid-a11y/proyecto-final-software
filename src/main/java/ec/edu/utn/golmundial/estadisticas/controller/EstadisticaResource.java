/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
import java.util.*;

@Path("/estadisticas")
public class EstadisticaResource {

    private static class Stats {
        int seleccionId;
        String codigoFifa;
        String seleccionNombre;
        String grupo;
        int pj = 0, pg = 0, pe = 0, pp = 0, gf = 0, gc = 0;
    }

    /**
     * GET /estadisticas/selecciones -> estadisticas de todas las selecciones.
     */
    @GET
    @Path("/selecciones")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEstadisticasSelecciones() {
        Map<Integer, Stats> statsMap = cargarStats(null);
        if (statsMap == null) {
            return Response.serverError().entity("{\"error\":\"Error al calcular estadisticas\"}").build();
        }

        List<Stats> lista = new ArrayList<>(statsMap.values());
        lista.sort((a, b) -> b.gf - a.gf); // goleadoras primero

        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Stats s : lista) {
            if (!first) json.append(",");
            json.append(toJson(s));
            first = false;
        }
        json.append("]");
        return Response.ok(json.toString()).build();
    }

    /**
     * GET /estadisticas/selecciones/{id} -> estadistica de una sola seleccion.
     */
    @GET
    @Path("/selecciones/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEstadisticaPorSeleccion(@PathParam("id") int id) {
        Map<Integer, Stats> statsMap = cargarStats(id);
        if (statsMap == null) {
            return Response.serverError().entity("{\"error\":\"Error al calcular estadisticas\"}").build();
        }

        Stats s = statsMap.get(id);
        if (s == null) {
            return Response.status(404)
                .entity("{\"error\":\"Seleccion no encontrada\"}")
                .build();
        }
        return Response.ok(toJson(s)).build();
    }

    /**
     * Carga las estadisticas calculadas. Si idFiltro no es null, solo trae esa seleccion
     * (pero igual recorre todos los partidos para que los calculos de pj/pg/pe/pp/gf/gc sean correctos).
     */
    private Map<Integer, Stats> cargarStats(Integer idFiltro) {
        Map<Integer, Stats> statsMap = new LinkedHashMap<>();

        try (Connection conn = DatabaseConfig.getConnection()) {

            String selSql = "SELECT id, codigo_fifa, nombre, grupo_codigo FROM selecciones"
                + (idFiltro != null ? " WHERE id = ?" : "");

            try (PreparedStatement selStmt = conn.prepareStatement(selSql)) {
                if (idFiltro != null) {
                    selStmt.setInt(1, idFiltro);
                }
                try (ResultSet rs = selStmt.executeQuery()) {
                    while (rs.next()) {
                        Stats s = new Stats();
                        s.seleccionId = rs.getInt("id");
                        s.codigoFifa = rs.getString("codigo_fifa");
                        s.seleccionNombre = rs.getString("nombre");
                        s.grupo = rs.getString("grupo_codigo");
                        statsMap.put(s.seleccionId, s);
                    }
                }
            }

            if (statsMap.isEmpty()) {
                return statsMap;
            }

            // Para el detalle de una sola seleccion tambien necesitamos sus partidos,
            // asi que filtramos por seleccion_local_id OR seleccion_visitante_id cuando aplica.
            String partSql = "SELECT seleccion_local_id, seleccion_visitante_id, goles_local, goles_visitante "
                + "FROM partidos WHERE goles_local IS NOT NULL AND goles_visitante IS NOT NULL"
                + (idFiltro != null ? " AND (seleccion_local_id = ? OR seleccion_visitante_id = ?)" : "");

            try (PreparedStatement partStmt = conn.prepareStatement(partSql)) {
                if (idFiltro != null) {
                    partStmt.setInt(1, idFiltro);
                    partStmt.setInt(2, idFiltro);
                }
                try (ResultSet rs = partStmt.executeQuery()) {
                    while (rs.next()) {
                        int localId = rs.getInt("seleccion_local_id");
                        int visitId = rs.getInt("seleccion_visitante_id");
                        int gl = rs.getInt("goles_local");
                        int gv = rs.getInt("goles_visitante");

                        Stats local = statsMap.get(localId);
                        Stats visit = statsMap.get(visitId);

                        if (local != null) {
                            local.pj++; local.gf += gl; local.gc += gv;
                            if (gl > gv) local.pg++;
                            else if (gl < gv) local.pp++;
                            else local.pe++;
                        }
                        if (visit != null) {
                            visit.pj++; visit.gf += gv; visit.gc += gl;
                            if (gv > gl) visit.pg++;
                            else if (gv < gl) visit.pp++;
                            else visit.pe++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        return statsMap;
    }

    private String toJson(Stats s) {
        int pts = s.pg * 3 + s.pe;
        int dg = s.gf - s.gc;
        double promedioGoles = s.pj > 0 ? Math.round((s.gf / (double) s.pj) * 100.0) / 100.0 : 0.0;
        return "{"
            + "\"seleccionId\":" + s.seleccionId + ","
            + "\"codigoFifa\":\"" + s.codigoFifa + "\","
            + "\"seleccionNombre\":\"" + s.seleccionNombre + "\","
            + "\"grupo\":\"" + s.grupo + "\","
            + "\"pj\":" + s.pj + ","
            + "\"pg\":" + s.pg + ","
            + "\"pe\":" + s.pe + ","
            + "\"pp\":" + s.pp + ","
            + "\"gf\":" + s.gf + ","
            + "\"gc\":" + s.gc + ","
            + "\"dg\":" + dg + ","
            + "\"pts\":" + pts + ","
            + "\"promedioGolesPorPartido\":" + promedioGoles
            + "}";
    }
}