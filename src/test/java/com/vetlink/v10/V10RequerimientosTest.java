package com.vetlink.v10;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.model.RespuestaDuenio;
import com.vetlink.disponibilidad.model.AusenciaVeterinario;
import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.model.TipoAusencia;
import com.vetlink.mascota.model.SituacionMascota;
import com.vetlink.notificacion.NotificacionRepository;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.service.GestionClinicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Requerimientos de la revisión integral V10 (reprogramación, desbloqueo, mascotas fallecidas, registro…). */
class V10RequerimientosTest extends PruebaWebBase {

    @Autowired NotificacionRepository notificaciones;
    @Autowired GestionClinicaService gestionClinica;

    private Cita recargar(Cita c) {
        return citaRepository.findById(c.getIdCita()).orElseThrow();
    }

    /** Cita reprogramada por recepción que espera la respuesta del dueño. */
    private record Escenario(Usuario recepcion, Usuario duenio, Usuario vet, Cita original, Cita propuesta, Cita otroCupo) {}

    private Escenario reprogramadaPorRecepcion() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        LocalDate fecha = LocalDate.now().plusDays(3);
        var cita = crearCita(mascota, vet, clinica, fecha, LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        var cupo = crearCita(null, vet, clinica, fecha.plusDays(1), LocalTime.of(15, 0), EstadoCita.DISPONIBLE);
        var otro = crearCita(null, vet, clinica, fecha.plusDays(2), LocalTime.of(10, 0), EstadoCita.DISPONIBLE);
        sincronizar();
        mockMvc.perform(post("/citas/recepcion/" + cita.getIdCita() + "/reagendar").with(csrf()).with(como(recepcion))
                        .param("idCupoNuevo", cupo.getIdCita().toString()))
                .andExpect(redirectedUrl("/reportes/citas?reagendada"));
        sincronizar();
        return new Escenario(recepcion, duenio, vet, cita, cupo, otro);
    }

    // ------------------------------------------------------------------
    // 9. Recepción: «Citas por reprogramar» solo de su clínica
    // ------------------------------------------------------------------

    @Test
    void citasPorReprogramarSoloMuestraCitasDeLaClinicaYSePuedenReprogramar() throws Exception {
        var clinicaA = crearClinica();
        var clinicaB = crearClinica();
        var recepcionA = crearRecepcionista(clinicaA, false);
        var vet = crearVeterinario(clinicaA);
        var mascota = crearMascota(crearDuenio());
        // El veterinario trabajó antes en la clínica B y conserva allí una cita: no debe verse en A.
        var ajena = crearCita(mascota, vet, clinicaB, LocalDate.now().plusDays(4), LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        // Cita de A afectada por un bloqueo de agenda.
        var propia = crearCita(mascota, vet, clinicaA, LocalDate.now().plusDays(5), LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        var bloqueo = new AusenciaVeterinario();
        bloqueo.setVeterinario(vet);
        bloqueo.setTipo(TipoAusencia.BLOQUEO);
        bloqueo.setInicio(propia.getInicio().minusHours(1));
        bloqueo.setFin(propia.getInicio().plusHours(2));
        bloqueo.setEstado(EstadoAprobacion.APROBADO);
        bloqueo.setCreadoEn(LocalDateTime.now());
        em.persist(bloqueo);
        sincronizar();

        mockMvc.perform(get("/recepcion/disponibilidad").with(como(recepcionA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/citas/recepcion/" + propia.getIdCita() + "/reagendar")))
                .andExpect(content().string(not(containsString("/citas/recepcion/" + ajena.getIdCita() + "/reagendar"))))
                .andExpect(content().string(containsString("Agendamiento bloqueado")));
        mockMvc.perform(get("/citas/recepcion/" + propia.getIdCita() + "/reagendar").with(como(recepcionA)))
                .andExpect(status().isOk());
        // La seguridad general se mantiene: la cita de otra clínica sigue sin acceso.
        mockMvc.perform(get("/citas/recepcion/" + ajena.getIdCita() + "/reagendar").with(como(recepcionA)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // 10. Respuesta del dueño a una reprogramación
    // ------------------------------------------------------------------

    @Test
    void alReprogramarLaNuevaCitaEsperaRespuestaYSeNotificaAlDuenioConLosDatos() throws Exception {
        var e = reprogramadaPorRecepcion();
        Cita propuesta = recargar(e.propuesta());
        assertThat(propuesta.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(propuesta.getRespuestaDuenio()).isEqualTo(RespuestaDuenio.PENDIENTE);
        assertThat(recargar(e.original()).getEstado()).isEqualTo(EstadoCita.REPROGRAMADA);

        var avisos = notificaciones.findByIdUsuarioOrderByCreadaEnDesc(e.duenio().getIdUsuario(), PageRequest.of(0, 10));
        assertThat(avisos).anySatisfy(n -> {
            assertThat(n.getTipo()).isEqualTo("CITA_REPROGRAMADA");
            assertThat(n.getMensaje()).contains(propuesta.getMascota().getNombre())
                    .contains("Antes:").contains("Nueva propuesta:").contains("pendiente de tu confirmación");
        });

        mockMvc.perform(get("/citas/" + propuesta.getIdCita()).with(como(e.duenio())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La clínica reprogramó tu cita")))
                .andExpect(content().string(containsString("/reprogramacion/confirmar")));
        mockMvc.perform(get("/citas/mias").with(como(e.duenio())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Requiere tu respuesta")));
    }

    @Test
    void elDuenioConfirmaElNuevoHorario() throws Exception {
        var e = reprogramadaPorRecepcion();
        mockMvc.perform(post("/citas/" + e.propuesta().getIdCita() + "/reprogramacion/confirmar").with(csrf()).with(como(e.duenio())))
                .andExpect(redirectedUrl("/citas/" + e.propuesta().getIdCita()))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        Cita c = recargar(e.propuesta());
        assertThat(c.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(c.getRespuestaDuenio()).isEqualTo(RespuestaDuenio.CONFIRMADA);
        assertThat(c.getRespuestaDuenioEn()).isNotNull();
        // La clínica recibe la respuesta.
        assertThat(notificaciones.findByIdUsuarioAndClaveUnica(e.recepcion().getIdUsuario(),
                "cita-" + c.getIdCita() + "-respuesta-duenio")).isPresent();
        // Confirmar dos veces no se permite.
        mockMvc.perform(post("/citas/" + c.getIdCita() + "/reprogramacion/confirmar").with(csrf()).with(como(e.duenio())))
                .andExpect(flash().attributeExists("errorGeneral"));
    }

    @Test
    void soloElDuenioPuedeResponder() throws Exception {
        var e = reprogramadaPorRecepcion();
        var otroDuenio = crearDuenio();
        sincronizar();
        mockMvc.perform(post("/citas/" + e.propuesta().getIdCita() + "/reprogramacion/confirmar").with(csrf()).with(como(otroDuenio)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/citas/" + e.propuesta().getIdCita() + "/reprogramacion/confirmar").with(csrf()).with(como(e.recepcion())))
                .andExpect(status().isForbidden());
        sincronizar();
        assertThat(recargar(e.propuesta()).getRespuestaDuenio()).isEqualTo(RespuestaDuenio.PENDIENTE);
    }

    @Test
    void elDuenioEligeOtroHorarioDelMismoServicio() throws Exception {
        var e = reprogramadaPorRecepcion();
        mockMvc.perform(get("/citas/" + e.propuesta().getIdCita() + "/reprogramacion/modificar").with(como(e.duenio())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"" + e.otroCupo().getIdCita() + "\"")));

        mockMvc.perform(post("/citas/" + e.propuesta().getIdCita() + "/reprogramacion/modificar").with(csrf()).with(como(e.duenio()))
                        .param("idCupoNuevo", e.otroCupo().getIdCita().toString()))
                .andExpect(redirectedUrl("/citas/" + e.otroCupo().getIdCita()));
        sincronizar();
        Cita nueva = recargar(e.otroCupo());
        Cita propuesta = recargar(e.propuesta());
        assertThat(nueva.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(nueva.getCitaOrigen().getIdCita()).isEqualTo(propuesta.getIdCita());
        assertThat(nueva.getServicio().getIdServicio()).isEqualTo(propuesta.getServicio().getIdServicio());
        assertThat(propuesta.getEstado()).isEqualTo(EstadoCita.REPROGRAMADA);
        assertThat(propuesta.getRespuestaDuenio()).isEqualTo(RespuestaDuenio.MODIFICADA);
    }

    @Test
    void noSePuedeCambiarElServicioNiUsarUnCupoOcupado() throws Exception {
        var e = reprogramadaPorRecepcion();
        var clinica = e.propuesta().getVeterinaria();
        Servicio otroServicio = crearServicio("Servicio Distinto " + siguiente());
        var cupoOtroServicio = crearCita(null, e.vet(), veterinariaRepository.findById(clinica.getIdVeterinaria()).orElseThrow(),
                LocalDate.now().plusDays(8), LocalTime.of(11, 0), EstadoCita.DISPONIBLE);
        cupoOtroServicio.setServicio(otroServicio);
        citaRepository.save(cupoOtroServicio);
        sincronizar();
        mockMvc.perform(post("/citas/" + e.propuesta().getIdCita() + "/reprogramacion/modificar").with(csrf()).with(como(e.duenio()))
                        .param("idCupoNuevo", cupoOtroServicio.getIdCita().toString()))
                .andExpect(flash().attribute("errorGeneral", "Solo puedes elegir horarios del mismo servicio y la misma veterinaria"));
        // La cita original (ya REPROGRAMADA) no es un cupo libre.
        mockMvc.perform(post("/citas/" + e.propuesta().getIdCita() + "/reprogramacion/modificar").with(csrf()).with(como(e.duenio()))
                        .param("idCupoNuevo", e.original().getIdCita().toString()))
                .andExpect(flash().attributeExists("errorGeneral"));
        sincronizar();
        assertThat(recargar(e.propuesta()).getRespuestaDuenio()).isEqualTo(RespuestaDuenio.PENDIENTE);
    }

    @Test
    void elDuenioCancelaLaCitaReprogramadaYSeLiberaElHorario() throws Exception {
        var e = reprogramadaPorRecepcion();
        Cita propuesta = recargar(e.propuesta());
        mockMvc.perform(post("/citas/" + propuesta.getIdCita() + "/cancelar").with(csrf()).with(como(e.duenio()))
                        .param("origen", "detalle"))
                .andExpect(redirectedUrl("/citas/" + propuesta.getIdCita() + "?cancelada"));
        sincronizar();
        Cita c = recargar(propuesta);
        assertThat(c.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(c.getRespuestaDuenio()).isEqualTo(RespuestaDuenio.CANCELADA);
        assertThat(citaRepository.findAll().stream().anyMatch(x -> x.getEstado() == EstadoCita.DISPONIBLE
                && x.getFecha().equals(c.getFecha()) && x.getHora().equals(c.getHora())
                && x.getVeterinario().getIdUsuario().equals(e.vet().getIdUsuario()))).isTrue();
    }

    // ------------------------------------------------------------------
    // 11. Desbloquear agendamiento
    // ------------------------------------------------------------------

    @Test
    void recepcionDesbloqueaLaAgendaYQuedaTrazado() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        sincronizar();
        LocalDateTime ini = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0);
        mockMvc.perform(post("/recepcion/disponibilidad/bloqueo").with(csrf()).with(como(recepcion))
                        .param("idVeterinario", vet.getIdUsuario().toString())
                        .param("inicio", ini.toString()).param("fin", ini.plusHours(8).toString())
                        .param("motivo", "Capacitación"))
                .andExpect(status().isOk());
        sincronizar();
        AusenciaVeterinario b = em.createQuery("select a from AusenciaVeterinario a where a.veterinario.idUsuario = :v",
                AusenciaVeterinario.class).setParameter("v", vet.getIdUsuario()).getSingleResult();

        // Otra clínica no puede desbloquearla.
        var ajena = crearRecepcionista(crearClinica(), false);
        sincronizar();
        mockMvc.perform(post("/recepcion/disponibilidad/bloqueo/" + b.getIdAusencia() + "/desbloquear").with(csrf()).with(como(ajena)))
                .andExpect(status().isForbidden());
        // Un veterinario tampoco (ruta exclusiva de recepción).
        mockMvc.perform(post("/recepcion/disponibilidad/bloqueo/" + b.getIdAusencia() + "/desbloquear").with(csrf()).with(como(vet)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/recepcion/disponibilidad/bloqueo/" + b.getIdAusencia() + "/desbloquear").with(csrf()).with(como(recepcion)))
                .andExpect(redirectedUrl("/recepcion/disponibilidad#equipo"))
                .andExpect(flash().attributeExists("mensajeOk"));
        sincronizar();
        AusenciaVeterinario d = em.find(AusenciaVeterinario.class, b.getIdAusencia());
        assertThat(d.getEstado()).isEqualTo(EstadoAprobacion.ANULADO);
        assertThat(d.getDesbloqueadaPor().getIdUsuario()).isEqualTo(recepcion.getIdUsuario());
        assertThat(d.getDesbloqueadaEn()).isNotNull();
        // Repetir el desbloqueo no hace nada.
        mockMvc.perform(post("/recepcion/disponibilidad/bloqueo/" + b.getIdAusencia() + "/desbloquear").with(csrf()).with(como(recepcion)))
                .andExpect(flash().attribute("errorGeneral", "Ese bloqueo ya no está vigente."));
    }

    // ------------------------------------------------------------------
    // 5. Mascota fallecida en solo lectura
    // ------------------------------------------------------------------

    @Test
    void mascotaFallecidaNoSeEditaNiPorLaApi() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, false);
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        crearCita(mascota, vet, clinica, LocalDate.now().minusDays(3), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        mascota.setSituacion(SituacionMascota.FALLECIDA);
        mascotaRepository.save(mascota);
        sincronizar();
        String nombre = mascota.getNombre();
        Double peso = mascota.getPeso();

        mockMvc.perform(get("/mascotas/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("solo lectura")))
                .andExpect(content().string(not(containsString("/mascotas/" + mascota.getIdMascota() + "/editar"))));
        mockMvc.perform(get("/mascotas/" + mascota.getIdMascota() + "/editar").with(como(duenio)))
                .andExpect(redirectedUrl("/mascotas/" + mascota.getIdMascota()));
        mockMvc.perform(post("/mascotas/" + mascota.getIdMascota() + "/editar").with(csrf()).with(como(duenio))
                        .param("nombre", "Otro Nombre").param("peso", "99")
                        .param("idRaza", mascota.getRaza().getIdRaza().toString()))
                .andExpect(redirectedUrl("/mascotas/" + mascota.getIdMascota()));
        mockMvc.perform(post("/recepcion/mascotas/" + mascota.getIdMascota() + "/peso").with(csrf()).with(como(recepcion))
                        .param("peso", "50"))
                .andExpect(flash().attribute("errorGeneral", com.vetlink.mascota.model.Mascota.MENSAJE_SOLO_LECTURA));
        // El historial sigue disponible.
        mockMvc.perform(get("/historial/mascota/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(status().isOk());
        sincronizar();
        var guardada = mascotaRepository.findById(mascota.getIdMascota()).orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo(nombre);
        assertThat(guardada.getPeso()).isEqualTo(peso);
    }

    // ------------------------------------------------------------------
    // 7. Historial clínico: solo mascotas vigentes del dueño
    // ------------------------------------------------------------------

    @Test
    void historialDelDuenioNoMuestraMascotasDadasDeBajaNiAjenas() throws Exception {
        var duenio = crearDuenio();
        var propia = crearMascota(duenio);
        var deBaja = crearMascota(duenio);
        deBaja.setEstado((byte) 0);
        mascotaRepository.save(deBaja);
        var ajena = crearMascota(crearDuenio());
        sincronizar();
        mockMvc.perform(get("/historial/mias").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(propia.getNombre())))
                .andExpect(content().string(not(containsString(deBaja.getNombre()))))
                .andExpect(content().string(not(containsString(ajena.getNombre()))));

        var sinMascotas = crearDuenio();
        sincronizar();
        mockMvc.perform(get("/historial/mias").with(como(sinMascotas)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aún no tienes mascotas registradas")));
    }

    // ------------------------------------------------------------------
    // 14. Servicios: sin duplicados aunque cambien tildes o mayúsculas
    // ------------------------------------------------------------------

    @Test
    void agregarServicioSinTildeReutilizaElDelCatalogo() {
        var clinica = crearClinica();
        var principal = crearRecepcionista(clinica, true);
        Servicio eco = crearServicio("Ecografía Prueba");
        long antes = servicioRepository.count();
        var sv = gestionClinica.agregarServicio(principal, "ecografia   prueba", null, 50000.0, 30, null);
        assertThat(sv.getServicio().getIdServicio()).isEqualTo(eco.getIdServicio());
        assertThat(servicioRepository.count()).isEqualTo(antes);
        // Del catálogo por id.
        Servicio otro = crearServicio("Radiología Prueba");
        var sv2 = gestionClinica.agregarServicio(principal, otro.getIdServicio(), null, null, 40000.0, 30, null);
        assertThat(sv2.getServicio().getIdServicio()).isEqualTo(otro.getIdServicio());
    }

    // ------------------------------------------------------------------
    // 12 y 15. Registro de veterinarias
    // ------------------------------------------------------------------

    @Test
    void registroDeVeterinariaExigeContraseniaSeguraYNoAceptaNitRepetido() throws Exception {
        var ciudad = new com.vetlink.ubicacion.model.Ciudad();
        ciudad.setNombre("Bogotá");
        ciudad = ciudadRepository.save(ciudad);
        int n = siguiente();
        var peticion = post("/registro").with(csrf())
                .param("numeroDocumento", "SEG" + n)
                .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                .param("nombre", "Ana").param("apellido", "Segura")
                .param("correo", "seg" + n + "@pruebas.vetlink")
                .param("telefono", "3001234567")
                .param("password", "clave1234").param("confirmarPassword", "clave1234")
                .param("rol", "VETERINARIA")
                .param("nuevaVeterinaria.nit", String.format("903%06d", n))
                .param("nuevaVeterinaria.nombre", "Clínica Segura " + letras(n))
                .param("nuevaVeterinaria.telefono", "6011234567")
                .param("nuevaVeterinaria.correo", "seg" + n + "@clinica.vetlink")
                .param("nuevaVeterinaria.direccion", "Calle 1 # 2-3")
                .param("nuevaVeterinaria.idCiudad", ciudad.getIdCiudad().toString())
                .param("nuevaVeterinaria.barrio", "Centro")
                .param("nuevaVeterinaria.servicios[0].idServicio", servicioPorDefecto().getIdServicio().toString())
                .param("nuevaVeterinaria.servicios[0].precio", "1000");
        mockMvc.perform(peticion)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mínimo 8 caracteres")));
        assertThat(usuarioRepository.findByNumeroDocumento("SEG" + n)).isEmpty();

        // NIT ya registrado: se detecta en el servidor y en la verificación inmediata.
        var existente = registrarClinicaNueva("Clinica Nit " + letras(n));
        String nit = veterinariaRepository.findById(existente.getRecepcionista().getVeterinaria().getIdVeterinaria())
                .orElseThrow().getNit();
        mockMvc.perform(get("/api/registro/disponible").param("campo", "nit").param("valor", nit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
        mockMvc.perform(get("/api/registro/disponible").param("campo", "nit").param("valor", "999999999"))
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void registroAceptaServicioPersonalizadoOtrosSinDuplicar() throws Exception {
        var ciudad = new com.vetlink.ubicacion.model.Ciudad();
        ciudad.setNombre("Bogotá");
        ciudad = ciudadRepository.save(ciudad);
        Servicio existente = crearServicio("Acupuntura Prueba");
        long antes = servicioRepository.count();
        int n = siguiente();
        mockMvc.perform(post("/registro").with(csrf())
                        .param("numeroDocumento", "OTR" + n)
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Ana").param("apellido", "Otros")
                        .param("correo", "otr" + n + "@pruebas.vetlink")
                        .param("telefono", "3001234567")
                        .param("password", PASSWORD_SEGURA_PRUEBAS).param("confirmarPassword", PASSWORD_SEGURA_PRUEBAS)
                        .param("rol", "VETERINARIA")
                        .param("nuevaVeterinaria.nit", String.format("904%06d", n))
                        .param("nuevaVeterinaria.nombre", "Clínica Otros " + letras(n))
                        .param("nuevaVeterinaria.telefono", "6011234567")
                        .param("nuevaVeterinaria.correo", "otr" + n + "@clinica.vetlink")
                        .param("nuevaVeterinaria.direccion", "Calle 1 # 2-3")
                        .param("nuevaVeterinaria.idCiudad", ciudad.getIdCiudad().toString())
                        .param("nuevaVeterinaria.barrio", "Centro")
                        // «Otros» con un nombre que ya existe (sin tilde distinta): se reutiliza.
                        .param("nuevaVeterinaria.servicios[0].nombrePersonalizado", "acupuntura prueba")
                        .param("nuevaVeterinaria.servicios[0].precio", "1000")
                        // «Otros» nuevo: se crea en el catálogo.
                        .param("nuevaVeterinaria.servicios[1].nombrePersonalizado", "Masaje Terapéutico " + letras(n))
                        .param("nuevaVeterinaria.servicios[1].precio", "2000"))
                .andExpect(redirectedUrl("/login?registradoVet"));
        sincronizar();
        assertThat(servicioRepository.count()).isEqualTo(antes + 1);
        var clinica = usuarioRepository.findByNumeroDocumento("OTR" + n).orElseThrow().getRecepcionista().getVeterinaria();
        var ofrecidos = servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(clinica.getIdVeterinaria());
        assertThat(ofrecidos).hasSize(2);
        assertThat(ofrecidos).anyMatch(sv -> sv.getServicio().getIdServicio().equals(existente.getIdServicio()));
    }

    // ------------------------------------------------------------------
    // 2. «Mis citas»: pestañas y filtros
    // ------------------------------------------------------------------

    @Test
    void misCitasSeparaActivasEHistorialYFiltraPorMascota() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var m1 = crearMascota(duenio);
        var m2 = crearMascota(duenio);
        var futura = crearCita(m1, vet, clinica, LocalDate.now().plusDays(2), LocalTime.of(9, 0), EstadoCita.CONFIRMADA);
        var pasada = crearCita(m2, vet, clinica, LocalDate.now().minusDays(2), LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        sincronizar();
        mockMvc.perform(get("/citas/mias").with(como(duenio)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/citas/" + futura.getIdCita() + "\"")))
                .andExpect(content().string(not(containsString("href=\"/citas/" + pasada.getIdCita() + "\""))));
        mockMvc.perform(get("/citas/mias").param("tab", "historial").with(como(duenio)))
                .andExpect(content().string(containsString("href=\"/citas/" + pasada.getIdCita() + "\"")))
                .andExpect(content().string(not(containsString("href=\"/citas/" + futura.getIdCita() + "\""))));
        mockMvc.perform(get("/citas/mias").param("tab", "historial").param("idMascota", m1.getIdMascota().toString()).with(como(duenio)))
                .andExpect(content().string(not(containsString("href=\"/citas/" + pasada.getIdCita() + "\""))))
                .andExpect(content().string(containsString("Limpiar filtros")));
    }

    // ------------------------------------------------------------------
    // 8. Novedad: la fecha final no puede ser anterior a la inicial
    // ------------------------------------------------------------------

    @Test
    void novedadConFechaFinalAnteriorSeRechaza() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        sincronizar();
        LocalDateTime ini = LocalDateTime.now().plusDays(3).withHour(23).withMinute(59).withSecond(0).withNano(0);
        mockMvc.perform(post("/disponibilidad/ausencia").with(csrf()).with(como(vet))
                        .param("tipo", "AUSENCIA").param("inicio", ini.toString()).param("fin", ini.minusHours(2).toString()))
                .andExpect(flash().attribute("errorGeneral", "El inicio debe ser anterior al fin"));
        // Mismo día (día de ausencia): se acepta.
        LocalDateTime dia = ini.withHour(0).withMinute(0);
        mockMvc.perform(post("/disponibilidad/ausencia").with(csrf()).with(como(vet))
                        .param("tipo", "AUSENCIA").param("inicio", dia.toString()).param("fin", dia.withHour(23).withMinute(59).toString()))
                .andExpect(flash().attributeExists("mensajeOk"));
    }
}
