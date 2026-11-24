package com.vetlink.fase1;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-01: el registro público no permite unirse a una clínica existente. */
class VL01RegistroRolTest extends PruebaWebBase {

    private MockHttpServletRequestBuilder registro(String rol) {
        int n = siguiente();
        return post("/registro").with(csrf())
                .param("numeroDocumento", "REG" + n)
                .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                .param("nombre", "Nuevo")
                .param("apellido", "Usuario")
                .param("correo", "nuevo" + n + "@pruebas.vetlink")
                .param("telefono", "3001234567")
                .param("password", "Clave12345")
                .param("confirmarPassword", "Clave12345")
                .param("rol", rol);
    }

    @Test
    void recepcionistaEnClinicaExistenteEsRechazado() throws Exception {
        var clinica = crearClinica();
        long antes = recepcionistaRepository.count();

        mockMvc.perform(registro("RECEPCIONISTA")
                        .param("turno", "MAÑANA")
                        .param("idVeterinariaRecepcion", clinica.getIdVeterinaria().toString()))
                .andExpect(status().isForbidden());

        assertThat(recepcionistaRepository.count()).isEqualTo(antes);
    }

    @Test
    void veterinarioEnClinicaExistenteEsRechazado() throws Exception {
        var clinica = crearClinica();
        long antes = veterinarioRepository.count();

        mockMvc.perform(registro("VETERINARIO")
                        .param("licencia", "LIC-123")
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString()))
                .andExpect(status().isForbidden());

        assertThat(veterinarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void registroDeDuenioSigueFuncionando() throws Exception {
        long antes = tutorRepository.count();

        mockMvc.perform(registro("TUTOR").param("direccion", "Calle 10 # 20-30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        assertThat(tutorRepository.count()).isEqualTo(antes + 1);
    }
}
