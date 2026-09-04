package bo.diplomado.calcula.controller;

import bo.diplomado.calcula.model.Liquidacion;
import bo.diplomado.calcula.service.PlanillaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Expone la liquidacion de planilla como API REST. */
@RestController
@RequestMapping("/api/planilla")
public class PlanillaController {

    private final PlanillaService servicio;

    public PlanillaController(PlanillaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/liquidar")
    public Liquidacion liquidar(@RequestParam double salario, @RequestParam int anios) {
        return servicio.liquidar(salario, anios);
    }

    /** Expone la configuracion vigente, para comparar instancias entre si. */
    @GetMapping("/parametros")
    public Map<String, Double> parametros() {
        return servicio.parametros();
    }

    /**
     * Una entrada invalida es un error del cliente, no del servidor: sin
     * esto Spring devolveria 500 y el health check del deployment
     * quedaria contaminado por errores que no son fallos del servicio.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> entradaInvalida(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
