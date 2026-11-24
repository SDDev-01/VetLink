package com.vetlink.fase3;

import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-19: la historia clínica no se borra y solo su autor la edita durante 24 horas. */
class VL19HistoriaInmutableTest extends PruebaWebBase {

    @Test
    void eliminarYaNoExiste() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);
        var h = crearHistorial(mascota, vet, LocalDateTime.now().minusMinutes(5));
        long antes = historialRepository.count();

        int estado = mockMvc.perform(post("/historial/" + h.getNumeroHistorial() + "/eliminar").with(csrf()).with(como(vet))
                        .param("idMascota", mascota.getIdMascota().toString()))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isIn(404, 405);
        assertThat(historialRepository.count()).isEqualTo(antes);
    }

    @Test
    void editarHistoriaDeHaceDosDiasEsRechazado() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var h = crearHistorial(mascota, vet, LocalDateTime.now().minusDays(2));

        mockMvc.perform(get("/historial/" + h.getNumeroHistorial() + "/editar").with(como(vet)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/historial/" + h.getNumeroHistorial() + "/editar").with(csrf()).with(como(vet))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("motivoConsulta", "Cambiado").param("diagnostico", "Cambiado"))
                .andExpect(status().isForbidden());

        sincronizar();
        assertThat(historialRepository.findById(h.getNumeroHistorial()).orElseThrow().getDiagnostico()).isEqualTo("Sano");
    }

    @Test
    void registroAntiguoSinFechaNoSeEdita() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var h = crearHistorial(crearMascota(crearDuenio()), vet, null);

        mockMvc.perform(get("/historial/" + h.getNumeroHistorial() + "/editar").with(como(vet)))
                .andExpect(status().isForbidden());
    }

    @Test
    void autorEditaHistoriaDeHaceUnaHora() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);
        var h = crearHistorial(mascota, vet, LocalDateTime.now().minusHours(1));
        sincronizar();

        mockMvc.perform(get("/historial/" + h.getNumeroHistorial() + "/editar").with(como(vet)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/historial/" + h.getNumeroHistorial() + "/editar").with(csrf()).with(como(vet))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("motivoConsulta", "Control").param("diagnostico", "Otitis leve"))
                .andExpect(redirectedUrl("/historial/paciente/" + mascota.getIdMascota() + "?actualizado"));

        sincronizar();
        assertThat(historialRepository.findById(h.getNumeroHistorial()).orElseThrow().getDiagnostico()).isEqualTo("Otitis leve");
    }

    @Test
    void otroVeterinarioNoEdita() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var h = crearHistorial(crearMascota(crearDuenio()), vet, LocalDateTime.now().minusHours(1));

        mockMvc.perform(get("/historial/" + h.getNumeroHistorial() + "/editar").with(como(crearVeterinario(clinica))))
                .andExpect(status().isForbidden());
    }

    @Test
    void botonEditarSoloEnHistoriasEditables() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);
        var reciente = crearHistorial(mascota, vet, LocalDateTime.now().minusHours(1));
        var vieja = crearHistorial(mascota, vet, LocalDateTime.now().minusDays(3));
        sincronizar();

        mockMvc.perform(get("/historial/paciente/" + mascota.getIdMascota()).with(como(vet)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/historial/" + reciente.getNumeroHistorial() + "/editar")))
                .andExpect(content().string(not(containsString("/historial/" + vieja.getNumeroHistorial() + "/editar"))))
                .andExpect(content().string(not(containsString("/eliminar"))));
    }
}
