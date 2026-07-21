/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ec.edu.utn.golmundial.estadisticas.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class TorneoResource {
    @GET
    @Path("/torneo/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadata() {
        String json = "{"
            + "\"nombreTorneo\":\"UTN GolMundial 2026\","
            + "\"anfitriones\":[\"Mexico\",\"Estados Unidos\",\"Canada\"],"
            + "\"fechaInicio\":\"2026-06-11T00:00:00Z\","
            + "\"fechaFin\":\"2026-07-19T00:00:00Z\","
            + "\"totalSelecciones\":48,"
            + "\"totalGrupos\":12,"
            + "\"totalPartidos\":104,"
            + "\"totalPartidosFaseGrupos\":72"
            + "}";
        return Response.ok(json).build();
    }
    @GET
    @Path("/fases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFases() {
        String json = "["
            + "{\"codigo\":\"GRUPOS\",\"nombre\":\"Fase de grupos\"},"
            + "{\"codigo\":\"DIECISEISAVOS\",\"nombre\":\"Dieciseisavos de final\"},"
            + "{\"codigo\":\"OCTAVOS\",\"nombre\":\"Octavos de final\"},"
            + "{\"codigo\":\"CUARTOS\",\"nombre\":\"Cuartos de final\"},"
            + "{\"codigo\":\"SEMIFINAL\",\"nombre\":\"Semifinal\"},"
            + "{\"codigo\":\"TERCER_PUESTO\",\"nombre\":\"Tercer puesto\"},"
            + "{\"codigo\":\"FINAL\",\"nombre\":\"Final\"}"
            + "]";
        return Response.ok(json).build();
    }
}
