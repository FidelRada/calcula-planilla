package bo.diplomado.calcula.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Resultado de una liquidacion.
 *
 * El constructor canonico redondea todos los importes a dos decimales
 * (HALF_UP). Se hace en el borde a proposito: el calculo interno usa
 * double, pero lo que sale por la API es estable, asi que las pruebas y
 * el smoke test pueden comparar valores exactos sin epsilon.
 *
 * Coste declarado: en un sistema de nomina real lo correcto seria
 * BigDecimal en toda la cadena, no solo al final.
 */
public record Liquidacion(
        double salarioBruto,
        double bonoAntiguedad,
        double porcentajeAntiguedad,
        double totalGanado,
        double aporteAfp,
        boolean topeAfpAplicado,
        double rcIva,
        boolean exentoRcIva,
        double liquidoPagable) {

    public Liquidacion {
        salarioBruto        = redondear(salarioBruto);
        bonoAntiguedad      = redondear(bonoAntiguedad);
        totalGanado         = redondear(totalGanado);
        aporteAfp           = redondear(aporteAfp);
        rcIva               = redondear(rcIva);
        liquidoPagable      = redondear(liquidoPagable);
    }

    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
