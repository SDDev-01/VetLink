package com.vetlink.fase2;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** VL-18: solo se atienden citas de hoy, y la atención es una sola transacción. */
class VL18AtenderCitaTest extends PruebaWebBase {

    @Test
    void atenderCitaDeMananaDaErrorYNoCreaHistoria() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var cita = crearCita(mascota, vet, clinica, LocalDate.now().plusDays(1), LocalTime.of(10, 0), EstadoCita.AGENDADA);
        long antes = historialRepository.count();

        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/atender").with(csrf()).with(como(vet))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("motivoConsulta", "Control")
                        .param("diagnostico", "Sano"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solo se pueden atender citas del día de hoy")));

        sincronizar();
        assertThat(historialRepository.count()).isEqualTo(antes);
        assertThat(citaRepository.findById(cita.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.AGENDADA);
    }

    @Test
    void formularioDeCitaFuturaMuestraElDetalleConMensaje() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var cita = crearCita(mascota, vet, clinica, LocalDate.now().plusDays(1), LocalTime.of(10, 0), EstadoCita.AGENDADA);
        sincronizar();

        mockMvc.perform(get("/agenda/" + cita.getIdCita() + "/atender").with(como(vet)))
                .andExpect(status().isOk())
                .andExpect(view().name("citas/detalle-profesional"))
                .andExpect(content().string(containsString("Solo se pueden atender citas del día de hoy")));
    }

    @Test
    void atenderCitaDeHoyCreaHistoriaYMarcaAtendida() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var cita = crearCita(mascota, vet, clinica, LocalDate.now(), LocalTime.of(10, 0), EstadoCita.AGENDADA);
        sincronizar();
        long antes = historialRepository.count();

        mockMvc.perform(get("/agenda/" + cita.getIdCita() + "/atender").with(como(vet)))
                .andExpect(view().name("historial/atender-cita"));
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/atender").with(csrf()).with(como(vet))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("motivoConsulta", "Control anual")
                        .param("diagnostico", "Sano"))
                .andExpect(redirectedUrl("/agenda/" + cita.getIdCita() + "?atendida"));

        sincronizar();
        assertThat(historialRepository.count()).isEqualTo(antes + 1);
        assertThat(citaRepository.findById(cita.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void otroVeterinarioNoPuedeAtender() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var cita = crearCita(mascota, vet, clinica, LocalDate.now(), LocalTime.of(10, 0), EstadoCita.AGENDADA);

        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/atender").with(csrf()).with(como(crearVeterinario(clinica)))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("motivoConsulta", "X").param("diagnostico", "Y"))
                .andExpect(status().isForbidden());
    }
}
