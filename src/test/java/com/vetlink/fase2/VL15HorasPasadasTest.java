package com.vetlink.fase2;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.dto.OfertarCitaForm;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.service.CitaService;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** VL-15: no se ofertan ni reservan cupos en horas que ya pasaron. */
class VL15HorasPasadasTest extends PruebaWebBase {

    @Autowired CitaService citaService;

    /** Una hora de hoy que ya pasó (minuto exacto, como en el formulario). */
    private static LocalTime horaPasadaHoy() {
        LocalTime ahora = LocalTime.now();
        Assumptions.assumeTrue(ahora.isAfter(LocalTime.of(0, 2)), "Justo después de medianoche no hay horas pasadas hoy");
        return ahora.minusMinutes(1).withSecond(0).withNano(0);
    }

    private OfertarCitaForm oferta(Usuario vet, LocalDate fecha, LocalTime hora) {
        OfertarCitaForm f = new OfertarCitaForm();
        f.setIdVeterinario(vet.getIdUsuario());
        f.setIdServicio(servicioPorDefecto().getIdServicio());
        f.setFecha(fecha);
        f.setHora(hora);
        return f;
    }

    private AgendarCitaForm reserva(Usuario vet, Veterinaria clinica, Integer idMascota, LocalDate fecha, LocalTime hora) {
        AgendarCitaForm f = new AgendarCitaForm();
        f.setIdMascota(idMascota);
        f.setIdVeterinaria(clinica.getIdVeterinaria());
        f.setIdVeterinario(vet.getIdUsuario());
        f.setIdServicio(servicioPorDefecto().getIdServicio());
        f.setFecha(fecha);
        f.setHora(hora);
        return f;
    }

    @Test
    void ofertarCupoHoyEnHoraPasadaDaError() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        ofrecerServicio(clinica, servicioPorDefecto());

        assertThatThrownBy(() -> citaService.ofertarCupo(oferta(vet, LocalDate.now(), horaPasadaHoy()), clinica.getIdVeterinaria()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se permiten citas en horas pasadas.");
    }

    @Test
    void reservarCupoDeHoyCuyaHoraPasoDaError() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        ofrecerServicio(clinica, servicioPorDefecto());
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        LocalTime pasada = horaPasadaHoy();
        crearCita(null, vet, clinica, LocalDate.now(), pasada, EstadoCita.DISPONIBLE);

        assertThatThrownBy(() -> citaService.agendar(
                reserva(vet, clinica, mascota.getIdMascota(), LocalDate.now(), pasada), duenio.getIdUsuario()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se permiten citas en horas pasadas.");

        // Tampoco se ofrece en la lista de cupos de hoy.
        mockMvc.perform(get("/citas/horarios-disponibles").with(como(duenio))
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("idVeterinario", vet.getIdUsuario().toString())
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("fecha", LocalDate.now().toString()))
                .andExpect(jsonPath("$.ocupadas", not(hasItem(pasada.toString().substring(0, 5)))));
    }

    @Test
    void cupoDeMananaFunciona() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        ofrecerServicio(clinica, servicioPorDefecto());
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        LocalDate manana = LocalDate.now().plusDays(1);

        var cupo = citaService.ofertarCupo(oferta(vet, manana, LocalTime.of(8, 0)), clinica.getIdVeterinaria());
        var cita = citaService.agendar(reserva(vet, clinica, mascota.getIdMascota(), manana, LocalTime.of(8, 0)),
                duenio.getIdUsuario());

        assertThat(cita.getIdCita()).isEqualTo(cupo.getIdCita());
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.AGENDADA);
    }
}
