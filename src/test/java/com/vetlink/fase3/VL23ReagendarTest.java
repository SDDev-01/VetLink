package com.vetlink.fase3;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-23: reagendar = mover la cita a otro cupo DISPONIBLE, liberando el horario original. */
class VL23ReagendarTest extends PruebaWebBase {

    private Cita recargar(Cita c) {
        return citaRepository.findById(c.getIdCita()).orElseThrow();
    }

    private org.springframework.test.web.servlet.ResultActions reagendar(Usuario recepcion, Cita cita, Cita cupo) throws Exception {
        return mockMvc.perform(post("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(csrf()).with(como(recepcion))
                .param("idCupoNuevo", cupo.getIdCita().toString()));
    }

    @Test
    void reagendarAUnCupoFuturoMueveLaCitaYLiberaElHorarioViejo() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        LocalDate fecha = LocalDate.now().plusDays(2);
        var cita = crearCita(mascota, vet, clinica, fecha, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        var cupo = crearCita(null, vet, clinica, fecha.plusDays(1), LocalTime.of(15, 0), EstadoCita.DISPONIBLE);
        sincronizar();

        mockMvc.perform(get("/reportes/citas").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/citas/recepcion/" + cita.getIdCita() + "/reagendar")));
        mockMvc.perform(get("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"" + cupo.getIdCita() + "\"")));

        reagendar(recepcion, cita, cupo).andExpect(redirectedUrl("/reportes/citas?reagendada"));

        sincronizar();
        Cita nueva = recargar(cupo);
        Cita vieja = recargar(cita);
        assertThat(nueva.getEstado()).isEqualTo(EstadoCita.AGENDADA);
        assertThat(nueva.getMascota().getIdMascota()).isEqualTo(mascota.getIdMascota());
        assertThat(vieja.getEstado()).isEqualTo(EstadoCita.DISPONIBLE);
        assertThat(vieja.getMascota()).isNull();
    }

    @Test
    void reagendarAUnCupoPasadoDaError() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var cita = crearCita(crearMascota(crearDuenio()), vet, clinica);
        var pasado = crearCita(null, vet, clinica, LocalDate.now().minusDays(1), LocalTime.of(9, 0), EstadoCita.DISPONIBLE);

        reagendar(recepcion, cita, pasado)
                .andExpect(redirectedUrl("/citas/recepcion/" + cita.getIdCita() + "/reagendar"))
                .andExpect(flash().attribute("errorGeneral", "No se permiten citas en horas pasadas."));

        sincronizar();
        assertThat(recargar(cita).getEstado()).isEqualTo(EstadoCita.AGENDADA);
        assertThat(recargar(pasado).getEstado()).isEqualTo(EstadoCita.DISPONIBLE);
    }

    @Test
    void reagendarAUnCupoDeOtraClinicaDaError() throws Exception {
        var clinica = crearClinica();
        var otra = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var cita = crearCita(crearMascota(crearDuenio()), crearVeterinario(clinica), clinica);
        var ajeno = crearCita(null, crearVeterinario(otra), otra, LocalDate.now().plusDays(3), LocalTime.of(9, 0),
                EstadoCita.DISPONIBLE);

        reagendar(recepcion, cita, ajeno)
                .andExpect(flash().attribute("errorGeneral", "El cupo elegido no pertenece a tu veterinaria"));

        sincronizar();
        assertThat(recargar(ajeno).getMascota()).isNull();
    }

    @Test
    void reagendarCuandoLaMascotaYaTieneOtraCitaAEsaHoraDaError() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet1 = crearVeterinario(clinica);
        var vet2 = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        LocalDate fecha = LocalDate.now().plusDays(4);
        var cita = crearCita(mascota, vet1, clinica, fecha, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        crearCita(mascota, vet1, clinica, fecha, LocalTime.of(11, 0), EstadoCita.AGENDADA);
        var cupo = crearCita(null, vet2, clinica, fecha, LocalTime.of(11, 0), EstadoCita.DISPONIBLE);

        reagendar(recepcion, cita, cupo)
                .andExpect(flash().attribute("errorGeneral", "La mascota ya tiene otra cita a esa fecha y hora"));
    }

    @Test
    void laListaSoloOfreceCuposFuturosDelMismoServicio() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var cita = crearCita(crearMascota(crearDuenio()), vet, clinica);
        var otroServicio = crearCita(null, vet, clinica, LocalDate.now().plusDays(5), LocalTime.of(9, 0), EstadoCita.DISPONIBLE);
        otroServicio.setServicio(crearServicio("Peluquería"));
        var pasado = crearCita(null, vet, clinica, LocalDate.now().minusDays(2), LocalTime.of(9, 0), EstadoCita.DISPONIBLE);
        sincronizar();

        mockMvc.perform(get("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(como(recepcion)))
                .andExpect(content().string(not(containsString("value=\"" + otroServicio.getIdCita() + "\""))))
                .andExpect(content().string(not(containsString("value=\"" + pasado.getIdCita() + "\""))))
                .andExpect(content().string(containsString("No hay cupos disponibles")));
    }
}
