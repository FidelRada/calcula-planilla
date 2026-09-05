package bo.diplomado.calcula.service;

import bo.diplomado.calcula.model.Liquidacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cubre las veinte ramas de PlanillaService: los seis escalones, el
 * tope de AFP, la exencion de RC-IVA, las dos validaciones de liquidar
 * y las cuatro decisiones del aguinaldo.
 *
 * Los valores esperados son los mismos que usa scripts/smoke-test.sh,
 * asi que la prueba unitaria y la de humo comprueban lo mismo desde los
 * dos lados del despliegue.
 */
class PlanillaServiceTest {

    private PlanillaService servicio;

    @BeforeEach
    void inicializar() {
        // Mismos valores que application.properties.
        servicio = new PlanillaService(2750, 0.1271, 60, 0.13, 2, 3);
    }

    @Nested
    @DisplayName("Escalones del bono de antiguedad")
    class Escalones {

        @ParameterizedTest(name = "{0} anios -> {1}")
        @CsvSource({
            "0,  0.00",
            "1,  0.00",
            "2,  0.05",
            "4,  0.05",
            "5,  0.11",   // borde inferior del escalon de 11 %
            "7,  0.11",
            "8,  0.18",
            "10, 0.18",
            "11, 0.26",
            "14, 0.26",
            "15, 0.34",
            "30, 0.34"
        })
        void devuelveElPorcentajeDelEscalon(int anios, double esperado) {
            assertEquals(esperado, servicio.porcentajeAntiguedad(anios), 1e-9);
        }

        @Test
        @DisplayName("un valor negativo cae al 0 % defensivo")
        void negativoCaeAlCero() {
            // porcentajeAntiguedad() es publico y no valida: liquidar() ya
            // rechaza los negativos antes de llegar aqui. Esta prueba cubre
            // la rama defensiva que de otro modo quedaria sin ejercitar.
            assertEquals(0.00, servicio.porcentajeAntiguedad(-1), 1e-9);
        }

        @Test
        @DisplayName("el borde de 5 anios no cae al escalon anterior")
        void bordeDeCincoAnios() {
            // Si alguien mueve el corte de `anios >= 5` a `anios >= 6`,
            // esta prueba es la que se pone roja.
            assertEquals(0.11, servicio.porcentajeAntiguedad(5), 1e-9);
            assertEquals(0.05, servicio.porcentajeAntiguedad(4), 1e-9);
        }
    }

    @Nested
    @DisplayName("Aporte a la AFP")
    class Afp {

        @Test
        @DisplayName("sin tope se aplica la tasa completa")
        void sinTope() {
            assertEquals(8802.50 * 0.1271, servicio.aporteAfp(8802.50), 1e-9);
        }

        @Test
        @DisplayName("por encima del tope se cotiza solo hasta el tope")
        void conTope() {
            double tope = 2750 * 60;
            assertEquals(tope * 0.1271, servicio.aporteAfp(tope + 50_000), 1e-9);
        }
    }

    @Nested
    @DisplayName("RC-IVA")
    class RcIva {

        @Test
        @DisplayName("por debajo de los minimos exentos no se paga")
        void exento() {
            assertEquals(0.0, servicio.rcIva(5500), 1e-9);
            assertEquals(0.0, servicio.rcIva(1000), 1e-9);
        }

        @Test
        @DisplayName("solo se grava el excedente sobre el exento")
        void sobreElExcedente() {
            assertEquals(500 * 0.13, servicio.rcIva(6000), 1e-9);
        }
    }

    @Nested
    @DisplayName("Liquidacion completa")
    class Completa {

        @ParameterizedTest(name = "salario {0}, {1} anios -> liquido {2}")
        @CsvSource({
            " 2750,  0,  2400.48",
            " 3000,  2,  2738.72",
            " 8500,  4,  7274.52",
            " 8500,  5,  7399.82",
            "20000, 15, 16613.52"
        })
        void calculaElLiquidoPagable(double salario, int anios, double liquido) {
            assertEquals(liquido, servicio.liquidar(salario, anios).liquidoPagable(), 1e-9);
        }

        @Test
        @DisplayName("el desglose cuadra con el liquido")
        void desgloseCoherente() {
            Liquidacion l = servicio.liquidar(8500, 5);

            assertEquals(302.50,  l.bonoAntiguedad(),  1e-9);
            assertEquals(8802.50, l.totalGanado(),     1e-9);
            assertEquals(1118.80, l.aporteAfp(),       1e-9);
            assertEquals(283.88,  l.rcIva(),           1e-9);
            assertEquals(7399.82, l.liquidoPagable(),  1e-9);
            assertFalse(l.topeAfpAplicado());
            assertFalse(l.exentoRcIva());
        }

        @Test
        @DisplayName("marca la exencion cuando no se paga RC-IVA")
        void marcaLaExencion() {
            assertTrue(servicio.liquidar(2750, 0).exentoRcIva());
        }

        @Test
        @DisplayName("marca el tope cuando el ganado lo supera")
        void marcaElTope() {
            assertTrue(servicio.liquidar(200_000, 0).topeAfpAplicado());
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada")
    class Validaciones {

        @Test
        @DisplayName("rechaza antiguedad negativa")
        void antiguedadNegativa() {
            IllegalArgumentException e = assertThrows(
                    IllegalArgumentException.class, () -> servicio.liquidar(8500, -1));
            assertTrue(e.getMessage().contains("antiguedad"));
        }

        @Test
        @DisplayName("rechaza un salario por debajo del minimo nacional")
        void salarioBajoElMinimo() {
            IllegalArgumentException e = assertThrows(
                    IllegalArgumentException.class, () -> servicio.liquidar(2749.99, 0));
            assertTrue(e.getMessage().contains("minimo"));
        }

        @Test
        @DisplayName("el salario minimo exacto es valido")
        void elMinimoExactoEsValido() {
            assertDoesNotThrow(() -> servicio.liquidar(2750, 0));
        }
    }

    @Nested
    @DisplayName("Parametros vigentes")
    class Parametros {

        @Test
        @DisplayName("expone los cinco parametros configurados")
        void exponeLosCinco() {
            var p = servicio.parametros();
            assertEquals(6, p.size());
            assertEquals(2750.0,  p.get("salarioMinimo"));
            assertEquals(0.1271,  p.get("tasaAfp"));
            assertEquals(60.0,    p.get("topeAfpMinimos"));
            assertEquals(0.13,    p.get("tasaRcIva"));
            assertEquals(2.0,     p.get("minimosExentos"));
            assertEquals(3.0,     p.get("mesesMinimosAguinaldo"));
        }
    }

    @Nested
    @DisplayName("Aguinaldo proporcional")
    class AguinaldoProporcional {

        @Test
        @DisplayName("sin derecho por debajo del minimo de meses")
        void sinDerecho() {
            var a = servicio.aguinaldo(8500, 5, 2);
            assertFalse(a.tieneDerecho());
            assertEquals(0.0, a.montoAguinaldo());
            assertEquals(8802.50, a.totalGanado());
        }

        @Test
        @DisplayName("en el borde inferior ya se devenga")
        void bordeInferior() {
            var a = servicio.aguinaldo(8500, 5, 3);
            assertTrue(a.tieneDerecho());
            assertEquals(2200.63, a.montoAguinaldo());
        }

        @Test
        @DisplayName("al ano cumplido se devenga un total ganado completo")
        void anoCompleto() {
            var a = servicio.aguinaldo(8500, 5, 12);
            assertTrue(a.tieneDerecho());
            assertEquals(a.totalGanado(), a.montoAguinaldo());
        }

        @ParameterizedTest(name = "{0} meses -> {1}")
        @CsvSource({
            "3,  2200.63",
            "6,  4401.25",
            "9,  6601.88",
            "11, 8068.96"
        })
        @DisplayName("la parte proporcional")
        void proporcional(int meses, double esperado) {
            assertEquals(esperado, servicio.aguinaldo(8500, 5, meses).montoAguinaldo());
        }

        @Test
        @DisplayName("meses negativos son entrada invalida")
        void mesesNegativos() {
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.aguinaldo(8500, 5, -1));
        }

        @Test
        @DisplayName("mas de doce meses son entrada invalida")
        void mesesExcesivos() {
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.aguinaldo(8500, 5, 13));
        }

        @Test
        @DisplayName("hereda las validaciones de liquidar")
        void heredaValidaciones() {
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.aguinaldo(100, 5, 6));
        }
    }
}
