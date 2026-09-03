package bo.diplomado.calcula.controller;

import bo.diplomado.calcula.service.PlanillaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanillaController.class)
@Import(PlanillaControllerTest.Config.class)
class PlanillaControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        PlanillaService planillaService() {
            return new PlanillaService(2750, 0.1271, 60, 0.13, 2);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("devuelve el desglose completo")
    void devuelveElDesglose() throws Exception {
        mockMvc.perform(get("/api/planilla/liquidar")
                        .param("salario", "8500")
                        .param("anios", "5"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.bonoAntiguedad").value(302.50))
               .andExpect(jsonPath("$.totalGanado").value(8802.50))
               .andExpect(jsonPath("$.aporteAfp").value(1118.80))
               .andExpect(jsonPath("$.rcIva").value(283.88))
               .andExpect(jsonPath("$.liquidoPagable").value(7399.82));
    }

    @Test
    @DisplayName("una entrada invalida devuelve 400, no 500")
    void entradaInvalida() throws Exception {
        mockMvc.perform(get("/api/planilla/liquidar")
                        .param("salario", "100")
                        .param("anios", "0"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("la antiguedad negativa devuelve 400")
    void antiguedadNegativa() throws Exception {
        mockMvc.perform(get("/api/planilla/liquidar")
                        .param("salario", "8500")
                        .param("anios", "-1"))
               .andExpect(status().isBadRequest());
    }
}
