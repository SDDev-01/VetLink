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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-16: no se agenda cita a una mascota eliminada. */
class VL16MascotaInactivaTest extends PruebaWebBase {

    @Test
    void agendarParaMascotaInactivaDaErrorYNoCreaCita() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        ofrecerServicio(clinica, servicioPorDefecto());
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        mascota.setEstado((byte) 0);
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
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La mascota no está activa")));

        sincronizar();
        var despues = citaRepository.findById(cupo.getIdCita()).orElseThrow();
        assertThat(despues.getEstado()).isEqualTo(EstadoCita.DISPONIBLE);
        assertThat(despues.getMascota()).isNull();
    }

    @Test
    void formularioDeAgendarRedirigeConMensaje() throws Exception {
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        mascota.setEstado((byte) 0);
        sincronizar();

        mockMvc.perform(get("/citas/agendar/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(redirectedUrl("/mascotas"))
                .andExpect(flash().attributeExists("errorMascota"));
    }
}
