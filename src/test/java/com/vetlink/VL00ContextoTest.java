package com.vetlink;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-00: el contexto arranca con H2 y la portada responde. */
class VL00ContextoTest extends PruebaWebBase {

    @Test
    void portadaRespondeOk() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void ayudantesCreanDatosValidos() throws Exception {
        var clinica = crearClinica();
        var duenio = crearDuenio();
        var vet = crearVeterinario(clinica);
        crearRecepcionista(clinica, true);
        crearCita(crearMascota(duenio), vet, clinica);
        sincronizar();
        mockMvc.perform(get("/dashboard").with(como(duenio))).andExpect(status().isOk());
    }
}
