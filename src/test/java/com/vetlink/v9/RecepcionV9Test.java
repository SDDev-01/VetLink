package com.vetlink.v9;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.disponibilidad.model.AusenciaVeterinario;
import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.model.TipoAusencia;
import com.vetlink.disponibilidad.repository.HorarioVeterinarioRepository;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.ServicioVeterinariaId;
import com.vetlink.veterinaria.model.Veterinaria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requisitos del rol RECEPCIONISTA (REC-01 a REC-09). */
class RecepcionV9Test extends PruebaWebBase {

    @Autowired HorarioVeterinarioRepository horarios;

    private Cita recargar(Cita c) {
        return citaRepository.findById(c.getIdCita()).orElseThrow();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder agendar(Usuario recepcion, Cita cupo, Integer idMascota) {
        return post("/citas/recepcion/agendar").with(csrf()).with(como(recepcion))
                .param("idCupo", cupo.getIdCita().toString())
                .param("idVeterinario", cupo.getVeterinario().getIdUsuario().toString())
                .param("idServicio", cupo.getServicio().getIdServicio().toString())
                .param("fecha", cupo.getFecha().toString())
                .param("hora", cupo.getHora().toString())
                .param("idMascota", idMascota.toString());
    }

    private void bloqueoAprobado(Usuario vet, LocalDateTime desde, LocalDateTime hasta) {
        var a = new AusenciaVeterinario();
        a.setVeterinario(vet);
        a.setTipo(TipoAusencia.BLOQUEO);
        a.setInicio(desde);
        a.setFin(hasta);
        a.setEstado(EstadoAprobacion.APROBADO);
        a.setCreadoEn(LocalDateTime.now());
        em.persist(a);
    }

    // ------------------------------------------------------------------ REC-02 / REC-04 / REC-05

    @Test
    void agendarRedirigeACitasDeLaClinicaConConfirmacion() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        LocalDate fecha = LocalDate.now().plusDays(3);
        var cupo = crearCita(null, vet, clinica, fecha, LocalTime.of(9, 0), EstadoCita.DISPONIBLE);

        mockMvc.perform(agendar(recepcion, cupo, mascota.getIdMascota()))
                .andExpect(redirectedUrl("/agenda?dia=" + fecha))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        assertThat(recargar(cupo).getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(recargar(cupo).getMascota().getIdMascota()).isEqualTo(mascota.getIdMascota());
    }

    @Test
    void agendarSinElegirHorarioNoGuardaNiRedirige() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, false);
        var mascota = crearMascota(crearDuenio());

        mockMvc.perform(post("/citas/recepcion/agendar").with(csrf()).with(como(recepcion))
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("idMascota", mascota.getIdMascota().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "idCupo"));
    }

    @Test
    void noSeAgendaUnHorarioOcupadoNiUnoBloqueado() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        LocalDate fecha = LocalDate.now().plusDays(4);
        var ocupada = crearCita(crearMascota(crearDuenio()), vet, clinica, fecha, LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        var bloqueado = crearCita(null, vet, clinica, fecha, LocalTime.of(11, 0), EstadoCita.DISPONIBLE);
        bloqueoAprobado(vet, fecha.atTime(10, 30), fecha.atTime(12, 0));
        var mascota = crearMascota(crearDuenio());
        sincronizar();

        mockMvc.perform(agendar(recepcion, ocupada, mascota.getIdMascota()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("errorGeneral"));
        mockMvc.perform(agendar(recepcion, bloqueado, mascota.getIdMascota()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorGeneral", "El veterinario no está disponible en ese horario."));
        sincronizar();
        assertThat(recargar(bloqueado).getEstado()).isEqualTo(EstadoCita.DISPONIBLE);
    }

    @Test
    void grillaMuestraLibresYDeshabilitaOcupadosYBloqueados() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        LocalDate fecha = LocalDate.now().plusDays(5);
        crearCita(null, vet, clinica, fecha, LocalTime.of(8, 0), EstadoCita.DISPONIBLE);
        crearCita(crearMascota(crearDuenio()), vet, clinica, fecha, LocalTime.of(9, 0), EstadoCita.PENDIENTE);
        crearCita(null, vet, clinica, fecha, LocalTime.of(11, 0), EstadoCita.DISPONIBLE);
        bloqueoAprobado(vet, fecha.atTime(10, 30), fecha.atTime(12, 0));
        sincronizar();

        mockMvc.perform(get("/citas/api/disponibilidad/grilla").with(como(recepcion))
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("idServicio", servicioPorDefecto().getIdServicio().toString())
                        .param("idVeterinario", vet.getIdUsuario().toString())
                        .param("fecha", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].hora").value("08:00")).andExpect(jsonPath("$[0].libre").value(true))
                .andExpect(jsonPath("$[1].hora").value("09:00")).andExpect(jsonPath("$[1].libre").value(false))
                .andExpect(jsonPath("$[1].motivo").value("Reserva pendiente")).andExpect(jsonPath("$[1].idCupo").doesNotExist())
                .andExpect(jsonPath("$[2].hora").value("11:00")).andExpect(jsonPath("$[2].libre").value(false))
                .andExpect(jsonPath("$[2].motivo").value("No disponible"));
    }

    @Test
    void formularioDeAgendarNoTieneCampoHoraYTieneBotonDeReporte() throws Exception {
        var recepcion = crearRecepcionista(crearClinica(), false);
        mockMvc.perform(get("/citas/recepcion/agendar").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("type=\"time\""))))
                .andExpect(content().string(containsString("Horarios disponibles")))
                .andExpect(content().string(containsString("Reporte de citas")));
    }

    // ------------------------------------------------------------------ REC-06

    @Test
    void recepcionReprogramaAUnHorarioCercanoDelMismoVeterinarioYLiberaElAnterior() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        LocalDate fecha = LocalDate.now().plusDays(2);
        var cita = crearCita(mascota, vet, clinica, fecha, LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        cita.setDuracionMin(60);
        citaRepository.save(cita);
        // Se cruza con la cita original (9:00-10:00): antes era imposible moverla aquí.
        var cupo = crearCita(null, vet, clinica, fecha, LocalTime.of(9, 30), EstadoCita.DISPONIBLE);
        sincronizar();

        mockMvc.perform(get("/agenda/" + cita.getIdCita()).with(como(recepcion)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/reagendar")));
        mockMvc.perform(get("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(como(recepcion)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("value=\"" + cupo.getIdCita() + "\"")));
        mockMvc.perform(post("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(csrf()).with(como(recepcion))
                        .param("idCupoNuevo", cupo.getIdCita().toString()))
                .andExpect(redirectedUrl("/reportes/citas?reagendada"));
        sincronizar();
        assertThat(recargar(cita).getEstado()).isEqualTo(EstadoCita.REPROGRAMADA);
        assertThat(recargar(cupo).getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(recargar(cupo).getCitaOrigen().getIdCita()).isEqualTo(cita.getIdCita());
    }

    @Test
    void personalQueAbreLaVistaDelDuenioVaALaVistaProfesional() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var cita = crearCita(crearMascota(crearDuenio()), crearVeterinario(clinica), clinica);
        mockMvc.perform(get("/citas/" + cita.getIdCita()).with(como(recepcion)))
                .andExpect(redirectedUrl("/agenda/" + cita.getIdCita()));
    }

    @Test
    void recepcionDeOtraClinicaNoReprograma() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var cita = crearCita(crearMascota(crearDuenio()), vet, clinica);
        var cupo = crearCita(null, vet, clinica, LocalDate.now().plusDays(3), LocalTime.of(15, 0), EstadoCita.DISPONIBLE);
        var ajena = crearRecepcionista(crearClinica(), true);

        mockMvc.perform(get("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(como(ajena)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(csrf()).with(como(ajena))
                .param("idCupoNuevo", cupo.getIdCita().toString()));
        sincronizar();
        assertThat(recargar(cita).getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(recargar(cupo).getEstado()).isEqualTo(EstadoCita.DISPONIBLE);
    }

    // ------------------------------------------------------------------ REC-01 / REC-03 / REC-07 / REC-08

    @Test
    void calendarioYPantallasDeRecepcionUsanLosNuevosIndicadores() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        crearVeterinario(clinica);
        mockMvc.perform(get("/agenda").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("vl-mk-cupo")))
                .andExpect(content().string(containsString("vl-mk-pendiente")))
                .andExpect(content().string(containsString("Cupos disponibles")));
        mockMvc.perform(get("/reportes/citas").with(como(recepcion)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/recepcion/disponibilidad").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("vl-equipo-card")))
                .andExpect(content().string(containsString("data-posterior-a=\"iniB\"")));
    }

    @Test
    void bloqueoDeAgendaRetiraCuposYAfectaLaDisponibilidadReal() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        LocalDate fecha = LocalDate.now().plusDays(6);
        var cupo = crearCita(null, vet, clinica, fecha, LocalTime.of(10, 0), EstadoCita.DISPONIBLE);
        sincronizar();

        mockMvc.perform(post("/recepcion/disponibilidad/bloqueo").with(csrf()).with(como(recepcion))
                        .param("idVeterinario", vet.getIdUsuario().toString())
                        .param("inicio", fecha.atTime(9, 0).toString()).param("fin", fecha.atTime(12, 0).toString())
                        .param("motivo", "Capacitación"))
                .andExpect(status().isOk());
        sincronizar();
        // El cupo libre ya no se ofrece.
        assertThat(citaRepository.findById(cupo.getIdCita()).map(Cita::getEstado).orElse(EstadoCita.CANCELADA))
                .isNotEqualTo(EstadoCita.DISPONIBLE);
    }

    // ------------------------------------------------------------------ REC-09

    @Test
    void recepcionistaNoPrincipalConsultaPeroNoModifica() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        mockMvc.perform(get("/veterinarias/admin").with(como(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vista de consulta")))
                .andExpect(content().string(not(containsString("Agregar servicio"))));
        mockMvc.perform(post("/veterinarias/admin/servicios").with(csrf()).with(como(recepcion))
                        .param("nombre", "Ecografía").param("precio", "80000").param("duracion", "30"))
                .andExpect(status().isForbidden());
    }

    @Test
    void principalAgregaEditaYRetiraServiciosYSeReflejaEnLaAgenda() throws Exception {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        var vet = crearVeterinario(clinica);
        horarioAprobado(vet, LocalTime.of(8, 0), LocalTime.of(10, 0));
        sincronizar();

        mockMvc.perform(post("/veterinarias/admin/servicios").with(csrf()).with(como(principal))
                        .param("nombre", "  ecografía   abdominal ").param("precio", "80000").param("duracion", "60")
                        .param("tipoRegistro", "CLINICO"))
                .andExpect(redirectedUrl("/veterinarias/admin#servicios"))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        var servicio = servicioRepository.findAll().stream()
                .filter(s -> s.getNombreServicio().equals("Ecografía Abdominal")).findFirst().orElseThrow();
        Integer id = servicio.getIdServicio();
        // Un servicio nuevo se ofrece cuando se asigna a los veterinarios que lo atienden.
        mockMvc.perform(post("/veterinarias/admin/veterinarios/" + vet.getIdUsuario() + "/servicios").with(csrf()).with(como(principal))
                        .param("servicios", id.toString()))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        var cuposDe = (java.util.function.Supplier<java.util.List<Cita>>) () -> citaRepository.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCita.DISPONIBLE && c.getServicio().getIdServicio().equals(id)).toList();
        assertThat(cuposDe.get()).isNotEmpty().allMatch(c -> c.getDuracionMin() == 60);

        // Duplicado (mismo nombre con otras mayúsculas) se rechaza.
        mockMvc.perform(post("/veterinarias/admin/servicios").with(csrf()).with(como(principal))
                        .param("nombre", "ECOGRAFÍA ABDOMINAL").param("precio", "1").param("duracion", "30"))
                .andExpect(flash().attributeExists("error"));

        // Cambiar la duración regenera los cupos libres con la nueva.
        mockMvc.perform(post("/veterinarias/admin/servicios/" + id).with(csrf()).with(como(principal))
                        .param("precio", "90000").param("duracion", "30"))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        assertThat(servicioVeterinariaRepository.findById(new ServicioVeterinariaId(id, clinica.getIdVeterinaria()))
                .orElseThrow().getPrecio()).isEqualTo(90000.0);
        assertThat(cuposDe.get()).isNotEmpty().allMatch(c -> c.getDuracionMin() == 30);

        // Una cita reservada de ese servicio se conserva al retirarlo.
        Cita reservada = cuposDe.get().get(0);
        reservada.setMascota(crearMascota(crearDuenio()));
        reservada.setEstado(EstadoCita.CONFIRMADA);
        citaRepository.save(reservada);
        sincronizar();
        mockMvc.perform(post("/veterinarias/admin/servicios/" + id + "/retirar").with(csrf()).with(como(principal)))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        assertThat(servicioVeterinariaRepository.findById(new ServicioVeterinariaId(id, clinica.getIdVeterinaria()))).isEmpty();
        assertThat(cuposDe.get()).isEmpty();
        assertThat(recargar(reservada).getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
    }

    @Test
    void principalModificaElHorarioDeUnVeterinarioYGeneraCupos() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        var principal = crearRecepcionista(clinica, true);
        var vet = crearVeterinario(clinica);
        autorizarServicio(vet, servicioPorDefecto());
        sincronizar();

        mockMvc.perform(post("/veterinarias/admin/veterinarios/" + vet.getIdUsuario() + "/horario").with(csrf()).with(como(principal))
                        .param("dia", "1", "2", "3", "4", "5", "6", "7")
                        .param("inicio", "08:00", "08:00", "08:00", "08:00", "08:00", "08:00", "08:00")
                        .param("fin", "10:00", "10:00", "10:00", "10:00", "10:00", "10:00", "10:00"))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        assertThat(horarios.findAll().stream().filter(h -> h.getVeterinario().getIdUsuario().equals(vet.getIdUsuario())
                && h.getEstado() == EstadoAprobacion.APROBADO)).hasSize(7);
        assertThat(citaRepository.findAll().stream().filter(c -> c.getEstado() == EstadoCita.DISPONIBLE
                && c.getVeterinario().getIdUsuario().equals(vet.getIdUsuario()))).isNotEmpty();
    }

    @Test
    void noSeGestionaPersonalDeOtraClinica() throws Exception {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        Veterinaria otra = crearClinica();
        var vetAjeno = crearVeterinario(otra);
        var recepAjena = crearRecepcionista(otra, false);

        mockMvc.perform(post("/veterinarias/admin/veterinarios/" + vetAjeno.getIdUsuario() + "/horario").with(csrf()).with(como(principal))
                        .param("dia", "1").param("inicio", "08:00").param("fin", "10:00"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/veterinarias/admin/recepcionistas/" + recepAjena.getIdUsuario() + "/turno").with(csrf()).with(como(principal))
                        .param("turno", "NOCHE"))
                .andExpect(status().isForbidden());
        sincronizar();
        assertThat(recepcionistaRepository.findById(recepAjena.getIdUsuario()).orElseThrow().getTurno()).isEqualTo("MAÑANA");
    }

    @Test
    void principalCambiaElTurnoDeUnaRecepcionistaDeSuClinica() throws Exception {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        var otra = crearRecepcionista(clinica, false);
        mockMvc.perform(post("/veterinarias/admin/recepcionistas/" + otra.getIdUsuario() + "/turno").with(csrf()).with(como(principal))
                        .param("turno", "TARDE"))
                .andExpect(flash().attribute("mensajeOk", "Turno actualizado."));
        sincronizar();
        assertThat(recepcionistaRepository.findById(otra.getIdUsuario()).orElseThrow().getTurno()).isEqualTo("TARDE");
    }

    @Test
    void altaDePersonalValidaNombresYTelefono() throws Exception {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        int n = siguiente();

        mockMvc.perform(post("/veterinarias/admin/recepcionistas").with(csrf()).with(como(principal))
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("numeroDocumento", "REC" + n).param("nombre", "Ana3").param("apellido", "Ruiz")
                        .param("correo", "rec" + n + "@pruebas.vetlink").param("telefono", "30ab12345")
                        .param("password", "Clave12345").param("turno", "MAÑANA"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("formEquipo", "nombre", "telefono"));
        sincronizar();
        assertThat(usuarioRepository.findByCorreo("rec" + n + "@pruebas.vetlink")).isEmpty();
    }
}
