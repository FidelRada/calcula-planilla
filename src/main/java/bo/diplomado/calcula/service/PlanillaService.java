package bo.diplomado.calcula.service;

import bo.diplomado.calcula.model.Liquidacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Liquidacion de planilla.
 *
 * Funciones puras y sin estado: se prueban con JUnit sin levantar el
 * contexto de Spring. Que el servicio no guarde nada es lo que permite
 * que dos instancias (BLUE y GREEN) convivan sin conflicto, que es la
 * precondicion del despliegue Blue-Green.
 *
 * Los parametros son valores de ejemplo parametrizados en
 * application.properties, NO una fuente legal. Las pruebas fijan la
 * logica -donde cortan los escalones, que el tope se aplique, que la
 * exencion funcione- no la exactitud normativa de las cifras.
 */
@Service
public class PlanillaService {

    /** Escalones de antiguedad: {aniosMinimos, porcentaje del salario minimo}. */
    private static final double[][] ESCALONES = {
        {15, 0.34}, {11, 0.26}, {8, 0.18}, {6, 0.11}, {2, 0.05}, {0, 0.00}
    };

    private final double salarioMinimo;
    private final double tasaAfp;
    private final double topeAfpMinimos;
    private final double tasaRcIva;
    private final double minimosExentos;

    public PlanillaService(
            @Value("${planilla.salario-minimo}")   double salarioMinimo,
            @Value("${planilla.tasa-afp}")         double tasaAfp,
            @Value("${planilla.tope-afp-minimos}") double topeAfpMinimos,
            @Value("${planilla.tasa-rc-iva}")      double tasaRcIva,
            @Value("${planilla.minimos-exentos}")  double minimosExentos) {
        this.salarioMinimo  = salarioMinimo;
        this.tasaAfp        = tasaAfp;
        this.topeAfpMinimos = topeAfpMinimos;
        this.tasaRcIva      = tasaRcIva;
        this.minimosExentos = minimosExentos;
    }

    /**
     * Porcentaje del bono de antiguedad. Seis ramas, una por escalon:
     * es la parte del servicio donde la cobertura de ramas dice algo.
     */
    public double porcentajeAntiguedad(int anios) {
        for (double[] escalon : ESCALONES) {
            if (anios >= escalon[0]) {
                return escalon[1];
            }
        }
        return 0.0;
    }

    /** Aporte a la AFP, con tope. Una rama: el tope se aplica o no. */
    public double aporteAfp(double totalGanado) {
        return Math.min(totalGanado, topeAfp()) * tasaAfp;
    }

    /** RC-IVA sobre el excedente. Una rama: por debajo del exento no se paga. */
    public double rcIva(double neto) {
        double exento = salarioMinimo * minimosExentos;
        return Math.max(0.0, neto - exento) * tasaRcIva;
    }

    /**
     * Los parametros vigentes en esta instancia.
     *
     * Sirve para comprobar que BLUE y GREEN estan configurados igual: si
     * dos instancias liquidan distinto, lo primero que hay que descartar
     * es que una tenga otros parametros.
     */
    public Map<String, Double> parametros() {
        return Map.of(
                "salarioMinimo",  salarioMinimo,
                "tasaAfp",        tasaAfp,
                "topeAfpMinimos", topeAfpMinimos,
                "tasaRcIva",      tasaRcIva,
                "minimosExentos", minimosExentos);
    }

    private double topeAfp() {
        return salarioMinimo * topeAfpMinimos;
    }

    /**
     * Liquida un salario. Dos ramas mas: las validaciones de entrada.
     *
     * @throws IllegalArgumentException si la antiguedad es negativa o el
     *         salario esta por debajo del minimo nacional.
     */
    public Liquidacion liquidar(double salarioBruto, int anios) {
        if (anios < 0) {
            throw new IllegalArgumentException("La antiguedad no puede ser negativa");
        }
        if (salarioBruto < salarioMinimo) {
            throw new IllegalArgumentException(
                    "El salario no puede ser menor al minimo nacional (Bs " + salarioMinimo + ")");
        }

        double porcentaje = porcentajeAntiguedad(anios);
        double bono       = salarioMinimo * porcentaje;
        double ganado     = salarioBruto + bono;
        double afp        = aporteAfp(ganado);
        double neto       = ganado - afp;
        double impuesto   = rcIva(neto);

        return new Liquidacion(
                salarioBruto,
                bono,
                porcentaje,
                ganado,
                afp,
                ganado > topeAfp(),
                impuesto,
                impuesto == 0.0,
                neto - impuesto);
    }
}
