/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ec.edu.utn.golmundial.estadisticas.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
/**
 */
public class AuditoriaHelper {

    public static void registrar(Connection conn, Integer usuarioId, String accion,
                                  String entidad, Integer entidadId, String detalle) {
        String sql = "INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, fecha_hora_utc, detalle) " +
            "VALUES (?, ?, ?, ?, (now() AT TIME ZONE 'utc'), ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (usuarioId != null) {
                stmt.setInt(1, usuarioId);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setString(2, accion);
            stmt.setString(3, entidad);
            if (entidadId != null) {
                stmt.setInt(4, entidadId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setString(5, detalle);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("No se pudo registrar auditoria: " + e.getMessage());
        }
    }
}