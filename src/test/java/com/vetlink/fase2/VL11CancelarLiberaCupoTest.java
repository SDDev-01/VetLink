package com.vetlink.fase2;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/** VL-11: cancelar una cita futura devuelve la hora como cupo DISPONIBLE. */
class VL11CancelarLiberaCupoTest extends PruebaWebBase {

    private List<Cita> cuposDisponibles(Cita base) {
        return citaRepository.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCita.DISPONIBLE
                        && c.getVeterinario().getIdUsuario().equals(base.getVeterinario().getIdUsuario())
                        && c.getFecha().equals(base.getFecha())
                        && c.getHora().equals(base.getHora()))
                .toList();
    }

    @Test
    void ofertarReservarYCancelarDejaUnCupoNuevo() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        ofrecerServicio(clinica, servicioPorDefecto());
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        LocalDate fecha = LocalDate.now().plusDays(3);
        var cupo = crearCita(null, vet, clinica, fecha, LocalTime.of(11, 0), EstadoCita.DISPONIBLE);
        sincronizar();

        // Reservar (dueño)
        mockMvc.perform(post("/citas/agendar").with(csrf()).with(como(duenio))
                        .param("idMascota", mascota.getIdMascota().toString())
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("idVeterinario", vet.getIdUsuario().toString())
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("fecha", fecha.toString())
                        .param("hora", "11:00"))
                .andExpect(redirectedUrl("/citas/mascota/" + mascota.getIdMascota() + "?agendada"));

        // Cancelar (dueño)
        mockMvc.perform(post("/citas/" + cupo.getIdCita() + "/cancelar").with(csrf()).with(como(duenio))
                        .param("idMascota", mascota.getIdMascota().toString()))
                .andExpect(redirectedUrl("/citas/mascota/" + mascota.getIdMascota() + "?cancelada"));

        sincronizar();
        Cita original = citaRepository.findById(cupo.getIdCita()).orElseThrow();
        assertThat(original.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(original.getMascota().getIdMascota()).isEqualTo(mascota.getIdMascota());

        List<Cita> nuevos = cuposDisponibles(original);
        assertThat(nuevos).hasSize(1);
        Cita nuevo = nuevos.get(0);
        assertThat(nuevo.getIdCita()).isNotEqualTo(original.getIdCita());
        assertThat(nuevo.getMascota()).isNull();
        assertThat(nuevo.getServicio().getIdServicio()).isEqualTo(original.getServicio().getIdServicio());
        assertThat(nuevo.getVeterinaria().getIdVeterinaria()).isEqualTo(clinica.getIdVeterinaria());
    }

    @Test
    void noDuplicaSiYaExisteUnCupoDisponibleIdentico() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        LocalDate fecha = LocalDate.now().plusDays(4);
        var agendada = crearCita(mascota, vet, clinica, fecha, LocalTime.of(15, 0), EstadoCita.AGENDADA);
        crearCita(null, vet, clinica, fecha, LocalTime.of(15, 0), EstadoCita.DISPONIBLE);
        sincronizar();

        mockMvc.perform(post("/citas/" + agendada.getIdCita() + "/cancelar").with(csrf()).with(como(duenio))
                        .param("idMascota", mascota.getIdMascota().toString()))
                .andExpect(redirectedUrl("/citas/mascota/" + mascota.getIdMascota() + "?cancelada"));

        sincronizar();
        assertThat(cuposDisponibles(citaRepository.findById(agendada.getIdCita()).orElseThrow())).hasSize(1);
    }
}
