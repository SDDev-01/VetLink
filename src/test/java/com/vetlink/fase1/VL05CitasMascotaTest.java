package com.vetlink.fase1;

import com.vetlink.support.PruebaWebBase;
import com.vetlink.cita.model.EstadoCita;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-05: las citas de una mascota solo las ve y agenda su dueño. */
class VL05CitasMascotaTest extends PruebaWebBase {

    @Test
    void veterinarioNoVeCitasDeMascota() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);

        mockMvc.perform(get("/citas/mascota/" + mascota.getIdMascota()).with(como(vet)))
                .andExpect(status().isForbidden());
    }

    @Test
    void recepcionistaNoAbreAgendarDeMascota() throws Exception {
        var clinica = crearClinica();
        var mascota = crearMascota(crearDuenio());

        mockMvc.perform(get("/citas/agendar/" + mascota.getIdMascota()).with(como(crearRecepcionista(clinica, false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void otroDuenioNoVeCitasDeMascotaAjena() throws Exception {
        var mascota = crearMascota(crearDuenio());

        mockMvc.perform(get("/citas/mascota/" + mascota.getIdMascota()).with(como(crearDuenio())))
                .andExpect(status().isForbidden());
    }

    @Test
    void duenioVeCitasDeSuMascota() throws Exception {
        var clinica = crearClinica();
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        crearCita(mascota, crearVeterinario(clinica), clinica);
        sincronizar();

        mockMvc.perform(get("/citas/mascota/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/citas/agendar/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/citas/agendar").with(como(duenio)))
                .andExpect(status().isOk());
    }

    @Test
    void duenioSigueAgendandoPorPost() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        ofrecerServicio(clinica, servicioPorDefecto());
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        LocalDate manana = LocalDate.now().plusDays(1);
        var cupo = crearCita(null, vet, clinica, manana, LocalTime.of(9, 0), EstadoCita.DISPONIBLE);
        sincronizar();

        mockMvc.perform(post("/citas/agendar").with(csrf()).with(como(duenio))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("idVeterinario", vet.getIdUsuario().toString())
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("fecha", manana.toString())
                        .param("hora", "09:00"))
                .andExpect(redirectedUrl("/citas/mascota/" + mascota.getIdMascota() + "?agendada"));

        sincronizar();
        assertThat(citaRepository.findById(cupo.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.AGENDADA);
    }
}
