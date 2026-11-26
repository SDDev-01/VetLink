package com.vetlink.fase2;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V10 (antes VL-09): la clínica que completa el registro queda aprobada automáticamente
 * (activa, con fecha de aprobación) y aparece en el directorio. Una clínica suspendida
 * (estado 0) sigue fuera del directorio y su recepción ve el aviso.
 */
class VL09ClinicaPendienteTest extends PruebaWebBase {

    @Test
    void clinicaNuevaQuedaAprobadaAutomaticamenteYApareceEnDirectorio() throws Exception {
        var recepcion = registrarClinicaNueva("Clinica Aprobada Zeta");
        var clinica = veterinariaRepository.findById(recepcion.getRecepcionista().getVeterinaria().getIdVeterinaria()).orElseThrow();

        assertThat(clinica.getEstado()).isEqualTo((byte) 1);
        assertThat(clinica.getAprobadaEn()).isNotNull();
        assertThat(clinica.getAprobacionAutomatica()).isTrue();
        // Recepcionista principal, sin permisos de administración del sistema (no existe ese rol).
        assertThat(recepcion.getRecepcionista().getEsPrincipal()).isTrue();
        mockMvc.perform(get("/directorio").param("buscar", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Clinica Aprobada Zeta")));
    }

    @Test
    void clinicaSuspendidaNoApareceEnDirectorioYSuRecepcionVeElAviso() throws Exception {
        var recepcion = registrarClinicaNueva("Clinica Suspendida Zeta");
        var clinica = veterinariaRepository.findById(recepcion.getRecepcionista().getVeterinaria().getIdVeterinaria()).orElseThrow();
        clinica.setEstado((byte) 0); // suspensión administrativa
        veterinariaRepository.save(clinica);
        sincronizar();

        mockMvc.perform(get("/directorio").param("buscar", "1"))
                .andExpect(content().string(not(containsString("Clinica Suspendida Zeta"))));
        mockMvc.perform(get("/directorio/" + clinica.getIdVeterinaria()))
                .andExpect(content().string(not(containsString("Clinica Suspendida Zeta"))));
        mockMvc.perform(get("/dashboard").with(como(usuarioRepository.findById(recepcion.getIdUsuario()).orElseThrow())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tu clínica está inactiva")));
    }

    @Test
    void clinicaVerificadaNoMuestraAviso() throws Exception {
        var recepcion = crearRecepcionista(crearClinica(), true);
        sincronizar();

        mockMvc.perform(get("/dashboard").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Tu clínica está inactiva"))));
    }
}
