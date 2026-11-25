package com.vetlink.fase2;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.veterinaria.model.Servicio;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VL-18 (nuevo flujo): «Atender cita» solo se habilita desde la hora programada y durante el
 * margen configurado (hora del servidor); la cita pasa a EN_ATENCION, se registra la historia y
 * solo entonces se puede finalizar como ATENDIDA.
 */
class VL18AtenderCitaTest extends PruebaWebBase {

    private Cita citaHoyHaceMinutos(int minutos) {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(minutos).withSecond(0).withNano(0);
        return crearCita(mascota, vet, clinica, inicio.toLocalDate(), inicio.toLocalTime(), EstadoCita.CONFIRMADA);
    }

    @Test
    void noSeIniciaAntesDeLaHoraProgramada() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var cita = crearCita(crearMascota(crearDuenio()), vet, clinica, LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                EstadoCita.CONFIRMADA);

        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/iniciar").with(csrf()).with(como(vet)))
                .andExpect(redirectedUrl("/agenda/" + cita.getIdCita()))
                .andExpect(flash().attribute("errorGeneral", "Disponible desde la hora programada."));
        sincronizar();
        assertThat(citaRepository.findById(cita.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
    }

    @Test
    void elDetalleMuestraElBotonDeshabilitadoConElMotivo() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var cita = crearCita(crearMascota(crearDuenio()), vet, clinica, LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                EstadoCita.CONFIRMADA);

        mockMvc.perform(get("/agenda/" + cita.getIdCita()).with(como(vet)))
                .andExpect(status().isOk())
                .andExpect(view().name("citas/detalle-profesional"))
                .andExpect(content().string(containsString("Atender cita")))
                .andExpect(content().string(containsString("Disponible desde la hora programada.")));
    }

    @Test
    void noSeIniciaFueraDelMargenPermitido() throws Exception {
        Cita cita = citaHoyHaceMinutos(40); // margen por defecto: 15 min
        sincronizar();
        var vet = usuarioRepository.findById(citaRepository.findById(cita.getIdCita()).orElseThrow()
                .getVeterinario().getIdUsuario()).orElseThrow();

        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/iniciar").with(csrf()).with(como(vet)))
                .andExpect(flash().attribute("errorGeneral", containsString("Se superó el margen")));
    }

    @Test
    void flujoCompletoIniciarRegistrarYFinalizar() throws Exception {
        Cita cita = citaHoyHaceMinutos(5);
        Integer idMascota = cita.getMascota().getIdMascota();
        var vet = cita.getVeterinario();
        long antes = historialRepository.count();

        // 1) Sin iniciar no se puede finalizar ni registrar.
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/finalizar").with(csrf()).with(como(vet)))
                .andExpect(flash().attribute("errorGeneral", containsString("solo se pueden crear durante una cita en atención")));

        // 2) Iniciar dentro de la ventana -> EN_ATENCION.
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/iniciar").with(csrf()).with(como(vet)))
                .andExpect(redirectedUrl("/agenda/" + cita.getIdCita() + "/atencion"));
        sincronizar();
        Cita enAtencion = citaRepository.findById(cita.getIdCita()).orElseThrow();
        assertThat(enAtencion.getEstado()).isEqualTo(EstadoCita.EN_ATENCION);
        assertThat(enAtencion.getInicioAtencion()).isNotNull();

        mockMvc.perform(get("/agenda/" + cita.getIdCita() + "/atencion").with(como(vet)))
                .andExpect(view().name("historial/atender-cita"));

        // 3) Finalizar sin registro clínico obligatorio -> rechazado.
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/finalizar").with(csrf()).with(como(vet)))
                .andExpect(flash().attribute("errorGeneral", containsString("Guardar el registro clínico")));

        // 4) Registrar la historia (queda ligada a la cita).
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/historial").with(csrf()).with(como(vet))
                        .param("idMascota", idMascota.toString())
                        .param("motivoConsulta", "Control anual")
                        .param("diagnostico", "Sano")
                        .param("temperatura", "38.5"))
                .andExpect(redirectedUrl("/agenda/" + cita.getIdCita() + "/atencion"));
        assertThat(historialRepository.count()).isEqualTo(antes + 1);

        // 5) Un segundo registro para la misma cita no se duplica.
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/historial").with(csrf()).with(como(vet))
                        .param("idMascota", idMascota.toString())
                        .param("motivoConsulta", "Repetido")
                        .param("diagnostico", "Repetido"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ya tiene un registro clínico")));
        assertThat(historialRepository.count()).isEqualTo(antes + 1);

        // 6) Finalizar -> ATENDIDA con hora de fin.
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/finalizar").with(csrf()).with(como(vet)))
                .andExpect(redirectedUrl("/agenda/" + cita.getIdCita() + "?atendida"));
        sincronizar();
        Cita atendida = citaRepository.findById(cita.getIdCita()).orElseThrow();
        assertThat(atendida.getEstado()).isEqualTo(EstadoCita.ATENDIDA);
        assertThat(atendida.getFinAtencion()).isNotNull();
        assertThat(historialRepository.findByCita_IdCita(cita.getIdCita())).isPresent();
    }

    @Test
    void serviciosSinRegistroObligatorioSeFinalizanSinHistoria() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var peluqueria = crearServicio("Peluquería " + siguiente(), Servicio.TipoRegistro.NINGUNO);
        var cita = citaEnAtencion(crearMascota(crearDuenio()), vet, clinica, peluqueria);

        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/finalizar").with(csrf()).with(como(vet)))
                .andExpect(redirectedUrl("/agenda/" + cita.getIdCita() + "?atendida"));
    }

    @Test
    void otroVeterinarioNoPuedeAtender() throws Exception {
        Cita cita = citaHoyHaceMinutos(2);
        var otro = crearVeterinario(cita.getVeterinaria());

        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/iniciar").with(csrf()).with(como(otro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/historial").with(csrf()).with(como(otro))
                        .param("idMascota", cita.getMascota().getIdMascota().toString())
                        .param("motivoConsulta", "x").param("diagnostico", "y"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recepcionNoPuedeIniciarNiRegistrar() throws Exception {
        Cita cita = citaHoyHaceMinutos(2);
        var recepcion = crearRecepcionista(cita.getVeterinaria(), false);

        mockMvc.perform(post("/agenda/" + cita.getIdCita() + "/iniciar").with(csrf()).with(como(recepcion)))
                .andExpect(status().isForbidden());
    }

    @Test
    void laCreacionLibreDeHistoriasDesdePacientesEstaBloqueada() throws Exception {
        Cita cita = citaHoyHaceMinutos(2);
        long antes = historialRepository.count();
        mockMvc.perform(post("/historial/nuevo").with(csrf()).with(como(cita.getVeterinario()))
                        .param("idMascota", cita.getMascota().getIdMascota().toString())
                        .param("motivoConsulta", "Libre").param("diagnostico", "Libre"))
                .andExpect(status().isForbidden());
        assertThat(historialRepository.count()).isEqualTo(antes);
    }
}
