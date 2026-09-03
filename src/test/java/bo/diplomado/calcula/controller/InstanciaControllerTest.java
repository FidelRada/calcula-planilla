package bo.diplomado.calcula.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstanciaController.class)
@TestPropertySource(properties = {
        "app.instance=GREEN",
        "server.port=8081",
        "app.version=1.2.3"
})
class InstanciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("expone la instancia, el puerto y la version del entorno")
    void exponeLaIdentidad() throws Exception {
        mockMvc.perform(get("/api/instance"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.instance").value("GREEN"))
               .andExpect(jsonPath("$.port").value("8081"))
               .andExpect(jsonPath("$.version").value("1.2.3"));
    }
}
