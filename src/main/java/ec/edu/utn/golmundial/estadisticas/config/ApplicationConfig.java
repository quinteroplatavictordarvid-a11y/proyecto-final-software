package ec.edu.utn.golmundial.estadisticas.config;

import ec.edu.utn.golmundial.estadisticas.controller.AdminPartidoResource;
import ec.edu.utn.golmundial.estadisticas.controller.AdminSeleccionResource;
import ec.edu.utn.golmundial.estadisticas.controller.AdminAuditoriaResource;
import ec.edu.utn.golmundial.estadisticas.controller.EstadisticaResource;
import ec.edu.utn.golmundial.estadisticas.controller.GrupoResource;
import ec.edu.utn.golmundial.estadisticas.controller.PartidoResource;
import ec.edu.utn.golmundial.estadisticas.controller.PosicionResource;
import ec.edu.utn.golmundial.estadisticas.controller.SedeResource;
import ec.edu.utn.golmundial.estadisticas.controller.SeleccionResource;
import ec.edu.utn.golmundial.estadisticas.controller.TorneoResource;
import ec.edu.utn.golmundial.estadisticas.controller.AuthResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class ApplicationConfig extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(SeleccionResource.class);
        classes.add(PartidoResource.class);
        classes.add(GrupoResource.class);
        classes.add(SedeResource.class);
        classes.add(PosicionResource.class);
        classes.add(EstadisticaResource.class);
        classes.add(AdminPartidoResource.class);
        classes.add(AdminSeleccionResource.class);
        classes.add(AdminAuditoriaResource.class);
        classes.add(AuthResource.class);
        classes.add(TorneoResource.class);
        classes.add(CorsFilter.class);
        return classes;
    }
}