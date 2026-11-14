package com.vetlink.fase2;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-17: la próxima cita sugerida es opcional y no puede ser pasada. */
class VL17ProximaCitaOpcionalTest extends PruebaWebBase {

    @Test
    void historiaSinProximaCitaSeGuarda() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        crearCita(mascota, vet, clinica);
        long antes = historialRepository.count();

        mockMvc.perform(post("/historial/nuevo").with(csrf()).with(como(vet))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("motivoConsulta", "Vómito")
                        .param("diagnostico", "Gastritis leve"))
                .andExpect(redirectedUrl("/historial/paciente/" + mascota.getIdMascota() + "?creado"));

        assertThat(historialRepository.count()).isEqualTo(antes + 1);
        sincronizar();
        mockMvc.perform(get("/historial/mascota/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(content().string(containsString("Sin control programado")));
    }

    @Test
    void proximaCitaPasadaDaErrorDeValidacion() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);
        long antes = historialRepository.count();

        mockMvc.perform(post("/historial/nuevo").with(csrf()).with(como(vet))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("motivoConsulta", "Control")
                        .param("diagnostico", "Sano")
                        .param("proximaCita", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "proximaCita"))
                .andExpect(content().string(containsString("La próxima cita no puede ser una fecha pasada")));

        assertThat(historialRepository.count()).isEqualTo(antes);
    }

    @Test
    void historiaGuardadaSinProximaCitaEnLaEntidad() {
        var clinica = crearClinica();
        var h = crearHistorial(crearMascota(crearDuenio()), crearVeterinario(clinica), LocalDateTime.now());
        h.setProximaCita(null);
        sincronizar();
        assertThat(historialRepository.findById(h.getNumeroHistorial()).orElseThrow().getProximaCita()).isNull();
    }
}
