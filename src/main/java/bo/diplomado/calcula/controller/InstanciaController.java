package bo.diplomado.calcula.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Identifica que instancia atendio la peticion.
 *
 * Sin este endpoint el Blue-Green seria indemostrable: un despliegue
 * correcto y uno que no conmuto el trafico se veen exactamente igual.
 *
 * La identidad NO esta compilada en el binario: viene de las variables
 * de entorno que pone systemd. Por eso el mismo .jar sirve para BLUE y
 * para GREEN, que es lo que permite promover el artefacto sin
 * reconstruirlo.
 */
@RestController
public class InstanciaController {

    private final String instancia;
    private final String puerto;
    private final String version;

    public InstanciaController(
            @Value("${app.instance:LOCAL}")     String instancia,
            @Value("${server.port:8080}")       String puerto,
            @Value("${app.version:desconocida}") String version) {
        this.instancia = instancia;
        this.puerto    = puerto;
        this.version   = version;
    }

    @GetMapping("/api/instance")
    public Map<String, String> instancia() {
        return Map.of(
                "instance", instancia,
                "port",     puerto,
                "date", LocalDateTime.now().toString(),
                "version",  version);
    }
}
