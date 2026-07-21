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

@Path("/posiciones")
public class PosicionResource {

    private static class Stats {
        int seleccionId;
        String codigoFifa;
        String seleccionNombre;
        String grupo;
        int pj = 0, pg = 0, pe = 0, pp = 0, gf = 0, gc = 0;
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPosiciones() {
        return calcularYResponder(null);
    }
    @GET
    @Path("/{grupo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPosicionesPorGrupo(@PathParam("grupo") String grupo) {
        return calcularYResponder(grupo);
    }

    private Response calcularYResponder(String grupoFiltro) {
        Map<Integer, Stats> statsMap = new LinkedHashMap<>();

        try (Connection conn = DatabaseConfig.getConnection()) {

            String selSql = "SELECT id, codigo_fifa, nombre, grupo_codigo FROM selecciones"
                + (grupoFiltro != null && !grupoFiltro.isEmpty() ? " WHERE grupo_codigo = ?" : "")
                + " ORDER BY grupo_codigo, nombre";

            try (PreparedStatement selStmt = conn.prepareStatement(selSql)) {
                if (grupoFiltro != null && !grupoFiltro.isEmpty()) {
                    selStmt.setString(1, grupoFiltro);
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
                return Response.ok("[]").build();
            }

            String partSql = "SELECT seleccion_local_id, seleccion_visitante_id, goles_local, goles_visitante "
                + "FROM partidos WHERE goles_local IS NOT NULL AND goles_visitante IS NOT NULL"
                + (grupoFiltro != null && !grupoFiltro.isEmpty() ? " AND grupo_codigo = ?" : "");

            try (PreparedStatement partStmt = conn.prepareStatement(partSql)) {
                if (grupoFiltro != null && !grupoFiltro.isEmpty()) {
                    partStmt.setString(1, grupoFiltro);
                }
                try (ResultSet rs = partStmt.executeQuery()) {
                    while (rs.next()) {
                        int localId = rs.getInt("seleccion_local_id");
                        int visitId = rs.getInt("seleccion_visitante_id");
                        int gl = rs.getInt("goles_local");
                        int gv = rs.getInt("goles_visitante");

                        Stats local = statsMap.get(localId);
                        Stats visit = statsMap.get(visitId);
                        if (local == null || visit == null) continue;

                        local.pj++; visit.pj++;
                        local.gf += gl; local.gc += gv;
                        visit.gf += gv; visit.gc += gl;

                        if (gl > gv) { local.pg++; visit.pp++; }
                        else if (gl < gv) { visit.pg++; local.pp++; }
                        else { local.pe++; visit.pe++; }
                    }
                }
            }
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }

        List<Stats> lista = new ArrayList<>(statsMap.values());
        lista.sort((a, b) -> {
            int cmpGrupo = a.grupo.compareTo(b.grupo);
            if (cmpGrupo != 0) return cmpGrupo;
            int ptsA = a.pg * 3 + a.pe, ptsB = b.pg * 3 + b.pe;
            if (ptsB != ptsA) return ptsB - ptsA;
            int dgA = a.gf - a.gc, dgB = b.gf - b.gc;
            if (dgB != dgA) return dgB - dgA;
            return b.gf - a.gf;
        });

        StringBuilder json = new StringBuilder("[");
        String grupoActual = null;
        int posicion = 0;
        boolean first = true;
        for (Stats s : lista) {
            if (!s.grupo.equals(grupoActual)) {
                grupoActual = s.grupo;
                posicion = 0;
            }
            posicion++;
            int pts = s.pg * 3 + s.pe;
            int dg = s.gf - s.gc;
            if (!first) json.append(",");
            json.append("{")
                .append("\"grupo\":\"").append(s.grupo).append("\",")
                .append("\"posicion\":").append(posicion).append(",")
                .append("\"seleccionId\":").append(s.seleccionId).append(",")
                .append("\"codigoFifa\":\"").append(s.codigoFifa).append("\",")
                .append("\"seleccionNombre\":\"").append(s.seleccionNombre).append("\",")
                .append("\"pj\":").append(s.pj).append(",")
                .append("\"pg\":").append(s.pg).append(",")
                .append("\"pe\":").append(s.pe).append(",")
                .append("\"pp\":").append(s.pp).append(",")
                .append("\"gf\":").append(s.gf).append(",")
                .append("\"gc\":").append(s.gc).append(",")
                .append("\"dg\":").append(dg).append(",")
                .append("\"pts\":").append(pts)
                .append("}");
            first = false;
        }
        json.append("]");
        return Response.ok(json.toString()).build();
    }
}