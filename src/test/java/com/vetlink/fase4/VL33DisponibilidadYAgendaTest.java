package com.vetlink.fase4;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.model.SolicitudDisponibilidad;
import com.vetlink.disponibilidad.model.TipoAusencia;
import com.vetlink.disponibilidad.repository.HorarioVeterinarioRepository;
import com.vetlink.disponibilidad.repository.VeterinarioServicioRepository;
import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService.Franja;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.veterinaria.model.Servicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Disponibilidad con aprobación, generación automática e idempotente de cupos y API del calendario. */
class VL33DisponibilidadYAgendaTest extends PruebaWebBase {

    @Autowired DisponibilidadService disponibilidad;
    @Autowired SolicitudDisponibilidadService solicitudes;
    @Autowired HorarioVeterinarioRepository horarios;
    @Autowired VeterinarioServicioRepository vetServicios;

    private List<Cita> cuposDe(Integer idVet, LocalDate fecha) {
        return citaRepository.findByVeterinario_IdUsuarioAndFecha(idVet, fecha).stream()
                .filter(c -> c.getEstado() == EstadoCita.DISPONIBLE).toList();
    }

    @Test
    void generacionRespetaHorarioServiciosCitasYEsIdempotente() {
        var clinica = crearClinica();
        Servicio consulta = servicioPorDefecto();
        Servicio control = crearServicio("Control " + siguiente());
        ofrecerServicio(clinica, consulta);
        ofrecerServicio(clinica, control);
        var vet = crearVeterinario(clinica);
        autorizarServicio(vet, consulta);
        autorizarServicio(vet, control);
        horarioAprobado(vet, LocalTime.of(8, 0), LocalTime.of(10, 0));
        LocalDate dia = LocalDate.now().plusDays(3);
        // Una cita ya reservada de 9:00 a 9:30 debe respetarse.
        crearCita(crearMascota(crearDuenio()), vet, clinica, dia, LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        sincronizar();

        int creados = disponibilidad.generarParaVeterinario(vet.getIdUsuario(), dia, dia);
        sincronizar();
        List<Cita> cupos = cuposDe(vet.getIdUsuario(), dia);
        assertThat(creados).isEqualTo(3).isEqualTo(cupos.size());
        assertThat(cupos).extracting(Cita::getHora)
                .containsExactlyInAnyOrder(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 30));
        assertThat(cupos).allMatch(c -> Boolean.TRUE.equals(c.getGeneradaAuto()));
        // Reparto entre los dos servicios autorizados.
        assertThat(cupos.stream().map(c -> c.getServicio().getIdServicio()).distinct().count()).isEqualTo(2);

        // Idempotencia: volver a generar no crea nada.
        assertThat(disponibilidad.generarParaVeterinario(vet.getIdUsuario(), dia, dia)).isZero();
        sincronizar();
        assertThat(cuposDe(vet.getIdUsuario(), dia)).hasSize(3);
    }

    @Test
    void sinServiciosAutorizadosNoSeGeneranCupos() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        horarioAprobado(vet, LocalTime.of(8, 0), LocalTime.of(12, 0));
        LocalDate dia = LocalDate.now().plusDays(2);
        assertThat(disponibilidad.generarParaVeterinario(vet.getIdUsuario(), dia, dia)).isZero();
    }

    @Test
    void solicitudDeHorarioQuedaPendienteYAlAprobarseGeneraCupos() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        autorizarServicio(vet, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, true);
        sincronizar();
        vet = usuarioRepository.findById(vet.getIdUsuario()).orElseThrow();
        recepcion = usuarioRepository.findById(recepcion.getIdUsuario()).orElseThrow();
        int manana = LocalDate.now().plusDays(1).getDayOfWeek().getValue();

        SolicitudDisponibilidad s = solicitudes.solicitarHorario(vet,
                List.of(new Franja(manana, LocalTime.of(14, 0), LocalTime.of(16, 0))), "Tardes");
        assertThat(s.getEstado()).isEqualTo(EstadoAprobacion.PENDIENTE);
        assertThat(disponibilidad.tieneHorarioAprobado(vet.getIdUsuario())).isFalse();

        var r = solicitudes.aprobar(s.getIdSolicitud(), recepcion, "Ok");
        assertThat(r.solicitud().getEstado()).isEqualTo(EstadoAprobacion.APROBADO);
        assertThat(disponibilidad.tieneHorarioAprobado(vet.getIdUsuario())).isTrue();
        assertThat(r.cuposCreados()).isGreaterThanOrEqualTo(4);
        // Ya resuelta: no se aprueba dos veces.
        final var rec = recepcion;
        assertThatThrownBy(() -> solicitudes.aprobar(s.getIdSolicitud(), rec, null)).hasMessageContaining("ya fue resuelta");
    }

    @Test
    void ausenciaAprobadaRetiraCuposLibresYSenalaCitasAfectadasSinBorrarlas() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        autorizarServicio(vet, servicioPorDefecto());
        horarioAprobado(vet, LocalTime.of(8, 0), LocalTime.of(10, 0));
        var recepcion = crearRecepcionista(clinica, true);
        LocalDate dia = LocalDate.now().plusDays(5);
        var cita = crearCita(crearMascota(crearDuenio()), vet, clinica, dia, LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        disponibilidad.generarParaVeterinario(vet.getIdUsuario(), dia, dia);
        sincronizar();
        assertThat(cuposDe(vet.getIdUsuario(), dia)).isNotEmpty();
        var vetU = usuarioRepository.findById(vet.getIdUsuario()).orElseThrow();
        var recU = usuarioRepository.findById(recepcion.getIdUsuario()).orElseThrow();

        var s = solicitudes.solicitarAusencia(vetU, TipoAusencia.INCAPACIDAD, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay(), "Gripe");
        var r = solicitudes.aprobar(s.getIdSolicitud(), recU, null);
        sincronizar();

        assertThat(cuposDe(vet.getIdUsuario(), dia)).isEmpty();
        assertThat(r.citasAfectadas()).extracting(Cita::getIdCita).containsExactly(cita.getIdCita());
        assertThat(citaRepository.findById(cita.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
    }

    @Test
    void elVeterinarioNoSeAutorizaServiciosASiMismo() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        sincronizar();

        mockMvc.perform(post("/disponibilidad/servicios").with(csrf()).with(como(vet))
                .param("servicios", servicioPorDefecto().getIdServicio().toString()));
        sincronizar();
        assertThat(disponibilidad.autorizadoPara(vet.getIdUsuario(), servicioPorDefecto().getIdServicio())).isFalse();
        var pendiente = solicitudes.deVeterinario(vet.getIdUsuario()).get(0);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoAprobacion.PENDIENTE);
        // Y no puede aprobar su propia solicitud (ruta solo para recepción).
        mockMvc.perform(post("/recepcion/disponibilidad/" + pendiente.getIdSolicitud() + "/aprobar").with(csrf()).with(como(vet)))
                .andExpect(status().isForbidden());
        // Recepción de otra clínica tampoco.
        var ajena = usuarioRepository.findById(crearRecepcionista(crearClinica(), true).getIdUsuario()).orElseThrow();
        assertThatThrownBy(() -> solicitudes.aprobar(pendiente.getIdSolicitud(), ajena, null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void apiDeHorasSoloDevuelveCuposReservables() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        LocalDate dia = LocalDate.now().plusDays(2);
        crearCita(null, vet, clinica, dia, LocalTime.of(9, 0), EstadoCita.DISPONIBLE);
        crearCita(null, vet, clinica, dia, LocalTime.of(10, 0), EstadoCita.DISPONIBLE);
        // Cupo heredado que choca con una cita confirmada: no debe ofrecerse.
        crearCita(crearMascota(crearDuenio()), vet, clinica, dia, LocalTime.of(11, 0), EstadoCita.CONFIRMADA);
        crearCita(null, vet, clinica, dia, LocalTime.of(11, 0), EstadoCita.DISPONIBLE);
        sincronizar();

        mockMvc.perform(get("/citas/api/disponibilidad/horas").with(como(duenio))
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("fecha", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hora").value("09:00"))
                .andExpect(jsonPath("$[0].etiqueta").value("9:00 a. m."));
        mockMvc.perform(get("/citas/api/disponibilidad/dias").with(como(duenio))
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("mes", dia.toString().substring(0, 7)))
                .andExpect(jsonPath("$[?(@.fecha == '" + dia + "')].cupos").value(org.hamcrest.Matchers.contains(2)));
        // Un dueño no puede consultar disponibilidad filtrando por una mascota ajena.
        mockMvc.perform(get("/citas/api/disponibilidad/horas").with(como(duenio))
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("idMascota", crearMascota(crearDuenio()).getIdMascota().toString())
                        .param("fecha", dia.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void veterinariosCompatiblesSoloLosAutorizados() {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var autorizado = crearVeterinario(clinica);
        crearVeterinario(clinica);
        autorizarServicio(autorizado, servicioPorDefecto());
        sincronizar();
        assertThat(disponibilidad.veterinariosCompatibles(clinica.getIdVeterinaria(), servicioPorDefecto().getIdServicio()))
                .extracting(u -> u.getIdUsuario()).containsExactly(autorizado.getIdUsuario());
        assertThat(Set.of(1)).isNotEmpty();
    }
}
