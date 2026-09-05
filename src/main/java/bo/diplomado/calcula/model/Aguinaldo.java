package bo.diplomado.calcula.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aguinaldo devengado por un trabajador en el ano.
 *
 * Igual que Liquidacion, redondea los importes a dos decimales en el
 * constructor canonico: lo que sale por la API es estable, asi que las
 * pruebas y el smoke test comparan valores exactos sin epsilon.
 */
public record Aguinaldo(
        double totalGanado,
        int mesesTrabajados,
        boolean tieneDerecho,
        double montoAguinaldo) {

    public Aguinaldo {
        totalGanado    = redondear(totalGanado);
        montoAguinaldo = redondear(montoAguinaldo);
    }

    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
