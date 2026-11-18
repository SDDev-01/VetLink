package com.vetlink.fase3;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-26: /uploads/** ya no es público. */
class VL26UploadsPrivadosTest extends PruebaWebBase {

    @Test
    void uploadsSinSesionRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/uploads/historial/2/archivo.xlsx"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
