package com.vetlink.fase3;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * La página /403 a la que Spring Security reenvía un acceso denegado.
 * El reenvío conserva el método: un POST denegado llega como POST.
 */
class PaginaAccesoDenegadoTest extends PruebaWebBase {

    @Test
    void unPostDenegadoMuestraLaPagina403YNoUn405() throws Exception {
        mockMvc.perform(post("/403").with(csrf()).with(como(crearDuenio())))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    void unGetDenegadoSigueMostrandoLaPagina403() throws Exception {
        mockMvc.perform(get("/403").with(como(crearDuenio())))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }
}
