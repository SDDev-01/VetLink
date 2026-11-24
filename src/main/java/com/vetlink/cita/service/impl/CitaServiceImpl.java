package com.vetlink.cita.service.impl;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.dto.FiltroCitaForm;
import com.vetlink.cita.dto.OfertarCitaForm;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.model.TransicionCita;
import com.vetlink.cita.model.TransicionCita.Actor;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.cita.service.CitaEstadoService;
import com.vetlink.cita.service.CitaService;
import com.vetlink.cita.service.ReservaConcurrenteException;
import com.vetlink.comun.config.ReglasNegocio;
import com.vetlink.comun.web.FormatoVista;
import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.notificacion.NotificacionService;
import com.vetlink.pattern.strategy.CompositeValidadorCita;
import com.vetlink.usuario.model.Recepcionista;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reglas de citas. Todas las operaciones que ocupan horario siguen el mismo protocolo:
 * <ol>
 *   <li>Bloqueo pesimista de la fila del cupo/cita, del veterinario y de la mascota
 *       (SELECT ... FOR UPDATE) en ese orden fijo, para evitar interbloqueos.</li>
 *   <li>Validación de solapamiento por duración completa, no solo por hora de inicio.</li>
 *   <li>Cambio de estado a través de {@link CitaEstadoService} (máquina de estados + historial).</li>
 *   <li>El índice único uk_cita_veterinario_ocupado de MySQL es la última barrera: si salta,
 *       se informa como reserva concurrente.</li>
 * </ol>
 */
@Service
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final VeterinariaRepository veterinariaRepository;
    private final ServicioVeterinariaRepository servicioVeterinariaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final RecepcionistaRepository recepcionistaRepository;
    private final CompositeValidadorCita validador;
    private final CitaEstadoService estados;
    private final DisponibilidadService disponibilidad;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final ReglasNegocio reglas;
    private final Clock reloj;

    public CitaServiceImpl(CitaRepository citaRepository, MascotaRepository mascotaRepository,
                           UsuarioRepository usuarioRepository, ServicioRepository servicioRepository,
                           VeterinariaRepository veterinariaRepository,
                           ServicioVeterinariaRepository servicioVeterinariaRepository,
                           VeterinarioRepository veterinarioRepository, RecepcionistaRepository recepcionistaRepository,
                           CompositeValidadorCita validador, CitaEstadoService estados,
                           DisponibilidadService disponibilidad, NotificacionService notificaciones,
                           AuditoriaService auditoria, ReglasNegocio reglas, Clock reloj) {
        this.citaRepository = citaRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.veterinariaRepository = veterinariaRepository;
        this.servicioVeterinariaRepository = servicioVeterinariaRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.recepcionistaRepository = recepcionistaRepository;
        this.validador = validador;
        this.estados = estados;
        this.disponibilidad = disponibilidad;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.reglas = reglas;
        this.reloj = reloj;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Override
    public List<Cita> listarPorMascota(Integer idMascota) {
        return citaRepository.findByMascota_IdMascotaOrderByFechaDescHoraDesc(idMascota);
    }

    @Override
    public List<Cita> listarPorTutor(Integer idTutor) {
        return citaRepository.findByMascota_Tutor_IdUsuarioOrderByFechaDescHoraDesc(idTutor);
    }

    @Override
    public List<Cita> buscarConFiltros(FiltroCitaForm filtro) {
        return citaRepository.buscarConFiltros(filtro.getIdVeterinaria(), filtro.getEstado(), filtro.getDesde(),
                filtro.getHasta(), filtro.getIdVeterinario());
    }

    // ------------------------------------------------------------------
    // Reservas
    // ------------------------------------------------------------------

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita agendar(AgendarCitaForm form, Integer idTutor) {
        return traducirConcurrencia(() -> {
            Mascota mascota = mascotaRepository.findById(form.getIdMascota())
                    .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
            if (!mascota.getTutor().getIdUsuario().equals(idTutor)) {
                throw new SecurityException("Esta mascota no te pertenece");
            }
            exigirAgendable(mascota);
            Usuario tutor = usuarioRepository.findById(idTutor).orElseThrow();
            Cita cita = reservarDesdeFormulario(form, mascota, Actor.TUTOR, tutor);
            notificaciones.notificar(idTutor, "CITA_RESERVADA", "Reserva enviada",
                    "Tu solicitud para " + mascota.getNombre() + " el " + describir(cita)
                            + " está pendiente de confirmación por la clínica.",
                    "/citas/" + cita.getIdCita(), "cita-" + cita.getIdCita() + "-reservada");
            notificaciones.notificarVarios(recepcionistasDe(cita.getVeterinaria().getIdVeterinaria()),
                    "CITA_PENDIENTE", "Nueva reserva por confirmar",
                    mascota.getNombre() + " · " + cita.getServicio().getNombreServicio() + " · " + describir(cita),
                    "/agenda/" + cita.getIdCita(), "cita-" + cita.getIdCita() + "-pendiente");
            return cita;
        });
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita agendarComoRecepcionista(AgendarCitaForm form, Integer idVeterinariaRecepcionista) {
        return traducirConcurrencia(() -> {
            if (!idVeterinariaRecepcionista.equals(form.getIdVeterinaria())) {
                throw new SecurityException("Solo puedes agendar citas en tu propia veterinaria");
            }
            Mascota mascota = mascotaRepository.findById(form.getIdMascota())
                    .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
            exigirAgendable(mascota);
            Cita cita = reservarDesdeFormulario(form, mascota, Actor.RECEPCIONISTA, null);
            notificaciones.notificar(mascota.getTutor().getIdUsuario(), "CITA_CONFIRMADA", "Cita agendada",
                    "La clínica agendó una cita para " + mascota.getNombre() + " el " + describir(cita) + ".",
                    "/citas/" + cita.getIdCita(), "cita-" + cita.getIdCita() + "-confirmada");
            return cita;
        });
    }

    private Cita reservarDesdeFormulario(AgendarCitaForm form, Mascota mascota, Actor actor, Usuario usuario) {
        validador.validarTodo(form);
        Veterinaria clinica = veterinariaRepository.findById(form.getIdVeterinaria())
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
        if (!clinica.isActiva()) throw new IllegalArgumentException("Veterinaria no encontrada");
        Usuario veterinario = usuarioRepository.findById(form.getIdVeterinario())
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        if (veterinario.getVeterinario() == null || veterinario.getVeterinario().getVeterinaria() == null
                || !veterinario.getVeterinario().getVeterinaria().getIdVeterinaria().equals(clinica.getIdVeterinaria())) {
            throw new IllegalArgumentException("El veterinario no pertenece a la veterinaria seleccionada");
        }
        exigirServicioDeClinica(form.getIdServicio(), clinica.getIdVeterinaria());

        Integer idCupo = form.getIdCupo();
        if (idCupo == null) {
            // El dueño reserva un cupo publicado; nunca crea horarios arbitrarios.
            idCupo = citaRepository.findByVeterinario_IdUsuarioAndServicio_IdServicioAndFechaAndHoraAndEstado(
                            veterinario.getIdUsuario(), form.getIdServicio(), form.getFecha(), form.getHora(),
                            EstadoCita.DISPONIBLE)
                    .map(Cita::getIdCita)
                    .orElseThrow(() -> new IllegalStateException(
                            "Ese horario ya no está disponible. Elige otro cupo del calendario."));
        }
        Cita cupo = citaRepository.bloquear(idCupo)
                .orElseThrow(() -> new IllegalStateException("Ese horario ya no está disponible."));
        if (!cupo.getVeterinario().getIdUsuario().equals(veterinario.getIdUsuario())
                || !cupo.getServicio().getIdServicio().equals(form.getIdServicio())
                || !cupo.getVeterinaria().getIdVeterinaria().equals(clinica.getIdVeterinaria())
                || !cupo.getFecha().equals(form.getFecha()) || !cupo.getHora().equals(form.getHora())) {
            throw new IllegalArgumentException("El cupo elegido no coincide con los datos de la cita");
        }
        return reservarCupoBloqueado(cupo, mascota, actor, usuario, form.getMotivo(), null);
    }

    /**
     * Reserva un cupo ya bloqueado (FOR UPDATE). Valida estado, anticipación y solapamientos
     * del veterinario y de la mascota con la duración completa.
     */
    private Cita reservarCupoBloqueado(Cita cupo, Mascota mascota, Actor actor, Usuario usuario, String motivo,
                                       Cita ignorarDeMascota) {
        if (cupo.getEstado() != EstadoCita.DISPONIBLE) {
            throw new ReservaConcurrenteException();
        }
        LocalDateTime ahora = ahora();
        if (cupo.getInicio().isBefore(ahora)) {
            throw new IllegalStateException("No se permiten citas en horas pasadas.");
        }
        if (cupo.getInicio().isBefore(ahora.plusMinutes(reglas.anticipacionMinimaReservaMin()))) {
            throw new IllegalStateException("Las citas se reservan con al menos "
                    + reglas.anticipacionMinimaReservaMin() + " minutos de anticipación.");
        }
        veterinarioRepository.bloquear(cupo.getVeterinario().getIdUsuario());
        mascotaRepository.bloquear(mascota.getIdMascota());
        validarSolapamientos(cupo, mascota, ignorarDeMascota);
        if (!disponibilidad.disponibleEn(cupo.getVeterinario(), cupo.getInicio(), cupo.getFin(), false)) {
            throw new IllegalStateException("El veterinario no está disponible en ese horario.");
        }

        EstadoCita nuevo = actor == Actor.TUTOR ? EstadoCita.PENDIENTE : EstadoCita.CONFIRMADA;
        estados.cambiar(cupo, nuevo, actor, usuario,
                actor == Actor.TUTOR ? "Reserva solicitada por el dueño" : "Cita agendada por recepción");
        cupo.setMascota(mascota);
        cupo.setReservadaEn(ahora);
        cupo.setMotivo(limpiar(motivo, 500));
        cupo.setExpiraEn(nuevo == EstadoCita.PENDIENTE ? calcularExpiracion(cupo, ahora) : null);
        Cita guardada = citaRepository.saveAndFlush(cupo);
        auditoria.registrar(usuario == null ? null : usuario.getIdUsuario(), "CITA_RESERVAR", "cita",
                guardada.getIdCita(), "estado=" + nuevo + " mascota=" + mascota.getIdMascota());
        return guardada;
    }

    private LocalDateTime calcularExpiracion(Cita cita, LocalDateTime ahora) {
        LocalDateTime porPlazo = ahora.plusHours(reglas.expiracionPendienteHoras());
        LocalDateTime antesDelInicio = cita.getInicio().minusMinutes(reglas.confirmacionAntesDelInicioMin());
        LocalDateTime exp = porPlazo.isBefore(antesDelInicio) ? porPlazo : antesDelInicio;
        return exp.isAfter(ahora) ? exp : cita.getInicio();
    }

    private void validarSolapamientos(Cita cupo, Mascota mascota, Cita ignorarDeMascota) {
        LocalDateTime inicio = cupo.getInicio();
        LocalDateTime fin = cupo.getFin();
        boolean choqueVet = citaRepository.deVeterinarioEnFechaConEstados(cupo.getVeterinario().getIdUsuario(),
                        cupo.getFecha(), EstadoCita.OCUPAN_HORARIO).stream()
                .anyMatch(c -> !c.getIdCita().equals(cupo.getIdCita()) && c.seSolapaCon(inicio, fin));
        if (choqueVet) {
            throw new ReservaConcurrenteException();
        }
        boolean choqueMascota = citaRepository.deMascotaEnFechaConEstados(mascota.getIdMascota(), cupo.getFecha(),
                        EstadoCita.OCUPAN_HORARIO).stream()
                .anyMatch(c -> !c.getIdCita().equals(cupo.getIdCita())
                        && (ignorarDeMascota == null || !c.getIdCita().equals(ignorarDeMascota.getIdCita()))
                        && c.seSolapaCon(inicio, fin));
        if (choqueMascota) {
            throw new IllegalStateException("La mascota ya tiene otra cita que se cruza con ese horario.");
        }
    }

    // ------------------------------------------------------------------
    // Cancelar, confirmar, rechazar, no asistió
    // ------------------------------------------------------------------

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelar(Integer idCita, Integer idTutor) {
        Cita cita = citaRepository.bloquear(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        if (cita.getMascota() == null || !cita.getMascota().getTutor().getIdUsuario().equals(idTutor)) {
            throw new SecurityException("No tienes permiso sobre esta cita");
        }
        Usuario tutor = usuarioRepository.findById(idTutor).orElseThrow();
        cancelarInterno(cita, Actor.TUTOR, tutor);
        notificaciones.notificarVarios(recepcionistasDe(cita.getVeterinaria().getIdVeterinaria()), "CITA_CANCELADA",
                "Cita cancelada por el dueño", cita.getMascota().getNombre() + " · " + describir(cita),
                "/agenda/" + cita.getIdCita(), "cita-" + cita.getIdCita() + "-cancelada");
        notificaciones.notificar(cita.getVeterinario().getIdUsuario(), "CITA_CANCELADA", "Cita cancelada",
                cita.getMascota().getNombre() + " · " + describir(cita), "/agenda/" + cita.getIdCita(),
                "cita-" + cita.getIdCita() + "-cancelada");
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelarComoRecepcionista(Integer idCita, Integer idVeterinariaRecepcionista) {
        Cita cita = citaRepository.bloquear(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        verificarCitaDeSuVeterinaria(cita, idVeterinariaRecepcionista);
        cancelarInterno(cita, Actor.RECEPCIONISTA, null);
        if (cita.getMascota() != null) {
            notificaciones.notificar(cita.getMascota().getTutor().getIdUsuario(), "CITA_CANCELADA",
                    "La clínica canceló tu cita", cita.getMascota().getNombre() + " · " + describir(cita),
                    "/citas/" + cita.getIdCita(), "cita-" + cita.getIdCita() + "-cancelada");
        }
        notificaciones.notificar(cita.getVeterinario().getIdUsuario(), "CITA_CANCELADA", "Cita cancelada",
                (cita.getMascota() == null ? "Cupo" : cita.getMascota().getNombre()) + " · " + describir(cita),
                "/agenda/" + cita.getIdCita(), "cita-" + cita.getIdCita() + "-cancelada");
    }

    private void cancelarInterno(Cita cita, Actor actor, Usuario usuario) {
        if (!EstadoCita.RESERVAS_VIGENTES.contains(cita.getEstado())) {
            throw new IllegalStateException("Solo se pueden cancelar citas pendientes o confirmadas");
        }
        if (cita.getInicio().isBefore(ahora())) {
            throw new IllegalStateException("No se pueden cancelar citas pasadas");
        }
        estados.cambiar(cita, EstadoCita.CANCELADA, actor, usuario,
                actor == Actor.TUTOR ? "Cancelada por el dueño" : "Cancelada por recepción");
        cita.setExpiraEn(null);
        citaRepository.save(cita);
        auditoria.registrar(usuario == null ? null : usuario.getIdUsuario(), "CITA_CANCELAR", "cita", cita.getIdCita(),
                "actor=" + actor);
        liberarHorario(cita);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita confirmar(Integer idCita, Integer idVeterinariaRecepcionista, Usuario recepcionista) {
        Cita cita = citaRepository.bloquear(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        verificarCitaDeSuVeterinaria(cita, idVeterinariaRecepcionista);
        if (cita.getInicio().isBefore(ahora())) {
            throw new IllegalStateException("La hora de la cita ya pasó; no se puede confirmar.");
        }
        estados.cambiar(cita, EstadoCita.CONFIRMADA, Actor.RECEPCIONISTA, recepcionista, "Confirmada por recepción");
        cita.setExpiraEn(null);
        citaRepository.save(cita);
        auditoria.registrar(idDe(recepcionista), "CITA_CONFIRMAR", "cita", idCita, null);
        notificaciones.notificar(cita.getMascota().getTutor().getIdUsuario(), "CITA_CONFIRMADA", "Cita confirmada",
                "La clínica confirmó la cita de " + cita.getMascota().getNombre() + " el " + describir(cita) + ".",
                "/citas/" + idCita, "cita-" + idCita + "-confirmada");
        notificaciones.notificar(cita.getVeterinario().getIdUsuario(), "CITA_CONFIRMADA", "Nueva cita en tu agenda",
                cita.getMascota().getNombre() + " · " + cita.getServicio().getNombreServicio() + " · " + describir(cita),
                "/agenda/" + idCita, "cita-" + idCita + "-confirmada");
        return cita;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita rechazar(Integer idCita, Integer idVeterinariaRecepcionista, Usuario recepcionista, String motivo) {
        Cita cita = citaRepository.bloquear(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        verificarCitaDeSuVeterinaria(cita, idVeterinariaRecepcionista);
        String texto = limpiar(motivo, 300);
        estados.cambiar(cita, EstadoCita.RECHAZADA, Actor.RECEPCIONISTA, recepcionista,
                texto == null ? "Rechazada por recepción" : "Rechazada: " + texto);
        cita.setExpiraEn(null);
        citaRepository.save(cita);
        auditoria.registrar(idDe(recepcionista), "CITA_RECHAZAR", "cita", idCita, texto);
        notificaciones.notificar(cita.getMascota().getTutor().getIdUsuario(), "CITA_RECHAZADA",
                "Solicitud de cita rechazada",
                "La clínica no pudo aceptar la cita de " + cita.getMascota().getNombre() + " el " + describir(cita)
                        + (texto == null ? "." : ": " + texto),
                "/citas/" + idCita, "cita-" + idCita + "-rechazada");
        liberarHorario(cita);
        return cita;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita marcarNoAsistio(Integer idCita, Usuario actorUsuario) {
        Cita cita = citaRepository.bloquear(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        Actor actor;
        if (actorUsuario.getRecepcionista() != null) {
            verificarCitaDeSuVeterinaria(cita, actorUsuario.getRecepcionista().getVeterinaria().getIdVeterinaria());
            actor = Actor.RECEPCIONISTA;
        } else if (actorUsuario.getVeterinario() != null
                && cita.getVeterinario().getIdUsuario().equals(actorUsuario.getIdUsuario())) {
            actor = Actor.VETERINARIO;
        } else {
            throw new AccessDeniedException("No tienes permiso sobre esta cita");
        }
        if (!ahora().isAfter(cita.getInicio().plusMinutes(reglas.margenInicioAtencionMin()))) {
            throw new IllegalStateException("Solo se puede marcar inasistencia cuando termina el margen de inicio ("
                    + reglas.margenInicioAtencionMin() + " min).");
        }
        estados.cambiar(cita, EstadoCita.NO_ASISTIO, actor, actorUsuario, "La mascota no asistió");
        citaRepository.save(cita);
        auditoria.registrar(actorUsuario.getIdUsuario(), "CITA_NO_ASISTIO", "cita", idCita, null);
        return cita;
    }

    @Override
    @Transactional
    public void solicitarReprogramacion(Integer idCita, Integer idTutor, String motivo) {
        Cita cita = citaRepository.findById(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        if (cita.getMascota() == null || !cita.getMascota().getTutor().getIdUsuario().equals(idTutor)) {
            throw new SecurityException("No tienes permiso sobre esta cita");
        }
        if (!EstadoCita.RESERVAS_VIGENTES.contains(cita.getEstado()) || cita.getInicio().isBefore(ahora())) {
            throw new IllegalStateException("Solo se puede pedir reprogramación de citas futuras pendientes o confirmadas");
        }
        String texto = limpiar(motivo, 300);
        String nota = "[" + new FormatoVista().fechaHora(ahora()) + "] El dueño solicitó reprogramar"
                + (texto == null ? "" : ": " + texto);
        cita.setObservacionesAdmin(cita.getObservacionesAdmin() == null ? nota : cita.getObservacionesAdmin() + "\n" + nota);
        citaRepository.save(cita);
        auditoria.registrar(idTutor, "CITA_SOLICITAR_REPROGRAMACION", "cita", idCita, texto);
        notificaciones.notificarVarios(recepcionistasDe(cita.getVeterinaria().getIdVeterinaria()),
                "CITA_REPROGRAMACION_SOLICITADA", "Solicitud de reprogramación",
                cita.getMascota().getNombre() + " · " + describir(cita) + (texto == null ? "" : " · " + texto),
                "/agenda/" + idCita, "cita-" + idCita + "-solicitud-reprogramacion");
    }

    // ------------------------------------------------------------------
    // Reprogramación (conserva la cita original como REPROGRAMADA)
    // ------------------------------------------------------------------

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita reagendarComoRecepcionista(Integer idCita, Integer idCupoNuevo, Integer idVeterinariaRecepcionista) {
        return traducirConcurrencia(() -> {
            if (idCita.equals(idCupoNuevo)) throw new IllegalArgumentException("Elige un cupo distinto");
            Cita original = citaRepository.bloquear(idCita)
                    .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
            verificarCitaDeSuVeterinaria(original, idVeterinariaRecepcionista);
            if (!EstadoCita.RESERVAS_VIGENTES.contains(original.getEstado()) || original.getMascota() == null) {
                throw new IllegalStateException("Solo se pueden reprogramar citas pendientes o confirmadas");
            }
            Cita cupo = citaRepository.bloquear(idCupoNuevo)
                    .orElseThrow(() -> new IllegalArgumentException("Cupo no encontrado"));
            if (!cupo.getVeterinaria().getIdVeterinaria().equals(idVeterinariaRecepcionista)) {
                throw new IllegalArgumentException("El cupo elegido no pertenece a tu veterinaria");
            }
            if (cupo.getEstado() != EstadoCita.DISPONIBLE) {
                throw new IllegalStateException("El cupo elegido ya no está disponible");
            }
            Mascota mascota = original.getMascota();
            Cita nueva = reservarCupoBloqueado(cupo, mascota, Actor.RECEPCIONISTA, null, original.getMotivo(), original);
            nueva.setCitaOrigen(original);
            nueva.setObservaciones(original.getObservaciones());
            nueva.setIndicacionesPrevias(original.getIndicacionesPrevias());
            citaRepository.save(nueva);

            estados.cambiar(original, EstadoCita.REPROGRAMADA, Actor.RECEPCIONISTA, null,
                    "Reprogramada a la cita #" + nueva.getIdCita());
            original.setExpiraEn(null);
            citaRepository.save(original);
            auditoria.registrar(null, "CITA_REPROGRAMAR", "cita", idCita, "nueva=" + nueva.getIdCita());
            liberarHorario(original);

            notificaciones.notificar(mascota.getTutor().getIdUsuario(), "CITA_REPROGRAMADA", "Cita reprogramada",
                    "La cita de " + mascota.getNombre() + " pasó del " + describir(original) + " al " + describir(nueva) + ".",
                    "/citas/" + nueva.getIdCita(), "cita-" + idCita + "-reprogramada");
            notificaciones.notificarVarios(List.of(original.getVeterinario().getIdUsuario(),
                            nueva.getVeterinario().getIdUsuario()), "CITA_REPROGRAMADA", "Cita reprogramada",
                    mascota.getNombre() + ": " + describir(original) + " → " + describir(nueva),
                    "/agenda/" + nueva.getIdCita(), "cita-" + idCita + "-reprogramada");
            return nueva;
        });
    }

    @Override
    public List<Cita> cuposParaReagendar(Integer idCita, Integer idVeterinariaRecepcionista) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        verificarCitaDeSuVeterinaria(cita, idVeterinariaRecepcionista);
        LocalDate hoy = ahora().toLocalDate();
        return disponibilidad.cuposReservables(idVeterinariaRecepcionista, cita.getServicio().getIdServicio(), null,
                        hoy, hoy.plusDays(Math.max(reglas.horizonteAgendaDias(), 30)), null).stream()
                .filter(c -> !c.getIdCita().equals(idCita))
                .toList();
    }

    // ------------------------------------------------------------------
    // Cupo manual (complementa la generación automática)
    // ------------------------------------------------------------------

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita ofertarCupo(OfertarCitaForm form, Integer idVeterinariaRecepcionista) {
        LocalDateTime inicio = LocalDateTime.of(form.getFecha(), form.getHora());
        if (inicio.isBefore(ahora())) {
            throw new IllegalStateException("No se permiten citas en horas pasadas.");
        }
        Usuario veterinario = usuarioRepository.findById(form.getIdVeterinario())
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        Servicio servicio = servicioRepository.findById(form.getIdServicio())
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
        Veterinaria veterinaria = veterinariaRepository.findById(idVeterinariaRecepcionista)
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
        if (veterinario.getVeterinario() == null || veterinario.getVeterinario().getVeterinaria() == null
                || !idVeterinariaRecepcionista.equals(veterinario.getVeterinario().getVeterinaria().getIdVeterinaria())) {
            throw new SecurityException("El veterinario no pertenece a tu veterinaria");
        }
        exigirServicioDeClinica(form.getIdServicio(), idVeterinariaRecepcionista);
        if (servicio.isEmergencia()) {
            throw new IllegalArgumentException("Las urgencias no se ofrecen como cupos; se gestionan en Emergencias.");
        }
        if (!disponibilidad.autorizadoPara(veterinario.getIdUsuario(), servicio.getIdServicio())) {
            throw new IllegalArgumentException("El veterinario no está autorizado para atender ese servicio");
        }
        veterinarioRepository.bloquear(veterinario.getIdUsuario());
        int duracion = disponibilidad.duracionDe(idVeterinariaRecepcionista, servicio.getIdServicio());
        LocalDateTime fin = inicio.plusMinutes(duracion);
        if (!disponibilidad.disponibleEn(veterinario, inicio, fin, false)) {
            throw new IllegalStateException("El veterinario no está disponible en ese horario (horario o ausencias)");
        }
        EnumSet<EstadoCita> bloquean = EnumSet.copyOf(EstadoCita.OCUPAN_HORARIO);
        bloquean.add(EstadoCita.DISPONIBLE);
        boolean choque = citaRepository.deVeterinarioEnFechaConEstados(veterinario.getIdUsuario(), form.getFecha(), bloquean)
                .stream().anyMatch(c -> c.seSolapaCon(inicio, fin));
        if (choque) {
            throw new IllegalStateException("El veterinario ya tiene un cupo o cita que se cruza con ese horario");
        }
        Cita cupo = new Cita();
        cupo.setFecha(form.getFecha());
        cupo.setHora(form.getHora());
        cupo.setDuracionMin(duracion);
        cupo.setEstado(EstadoCita.DISPONIBLE);
        cupo.setVeterinario(veterinario);
        cupo.setServicio(servicio);
        cupo.setVeterinaria(veterinaria);
        cupo.setCreadaEn(ahora());
        return citaRepository.save(cupo);
    }

    // ------------------------------------------------------------------
    // Liberación de cupos
    // ------------------------------------------------------------------

    /**
     * Devuelve el horario a la oferta como un cupo nuevo si: falta al menos la anticipación
     * mínima de liberación, el veterinario sigue disponible y autorizado para el servicio,
     * y no existe ya otro cupo o cita que se cruce. La cita cerrada se conserva como historia.
     */
    private void liberarHorario(Cita cita) {
        LocalDateTime ahora = ahora();
        if (cita.getInicio().isBefore(ahora.plusMinutes(reglas.anticipacionMinimaLiberacionMin()))) return;
        Usuario vet = cita.getVeterinario();
        Integer idServicio = cita.getServicio().getIdServicio();
        if (disponibilidad.tieneServiciosAutorizados(vet.getIdUsuario())
                && !disponibilidad.autorizadoPara(vet.getIdUsuario(), idServicio)) return;
        if (!disponibilidad.disponibleEn(vet, cita.getInicio(), cita.getFin(), false)) return;
        EnumSet<EstadoCita> bloquean = EnumSet.copyOf(EstadoCita.OCUPAN_HORARIO);
        bloquean.add(EstadoCita.DISPONIBLE);
        boolean ocupado = citaRepository.deVeterinarioEnFechaConEstados(vet.getIdUsuario(), cita.getFecha(), bloquean)
                .stream().anyMatch(c -> !c.getIdCita().equals(cita.getIdCita()) && c.seSolapaCon(cita.getInicio(), cita.getFin()));
        if (ocupado) return;
        Cita cupo = new Cita();
        cupo.setVeterinario(vet);
        cupo.setServicio(cita.getServicio());
        cupo.setVeterinaria(cita.getVeterinaria());
        cupo.setFecha(cita.getFecha());
        cupo.setHora(cita.getHora());
        cupo.setDuracionMin(cita.getDuracionMin());
        cupo.setEstado(EstadoCita.DISPONIBLE);
        cupo.setGeneradaAuto(Boolean.TRUE.equals(cita.getGeneradaAuto()));
        cupo.setCreadaEn(ahora);
        citaRepository.save(cupo);
    }

    // ------------------------------------------------------------------
    // Tareas del sistema
    // ------------------------------------------------------------------

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int expirarPendientes() {
        LocalDateTime ahora = ahora();
        int n = 0;
        for (Cita c : citaRepository.pendientesVencidas(ahora, ahora.toLocalDate(), ahora.toLocalTime())) {
            Cita cita = citaRepository.bloquear(c.getIdCita()).orElse(null);
            if (cita == null || cita.getEstado() != EstadoCita.PENDIENTE) continue;
            estados.cambiar(cita, EstadoCita.RECHAZADA, Actor.SISTEMA, null, "La reserva expiró sin confirmación");
            cita.setExpiraEn(null);
            citaRepository.save(cita);
            if (cita.getMascota() != null) {
                notificaciones.notificar(cita.getMascota().getTutor().getIdUsuario(), "CITA_RECHAZADA",
                        "Reserva expirada", "La clínica no confirmó a tiempo la cita de "
                                + cita.getMascota().getNombre() + " del " + describir(cita) + ". Puedes elegir otro horario.",
                        "/citas/" + cita.getIdCita(), "cita-" + cita.getIdCita() + "-rechazada");
            }
            liberarHorario(cita);
            n++;
        }
        return n;
    }

    @Override
    @Transactional
    public int enviarRecordatorios() {
        LocalDateTime ahora = ahora();
        LocalDateTime limite = ahora.plusHours(reglas.recordatorioHorasAntes());
        int n = 0;
        for (Cita c : citaRepository.confirmadasEntreFechas(ahora.toLocalDate(), limite.toLocalDate())) {
            if (c.getMascota() == null || c.getInicio().isBefore(ahora) || c.getInicio().isAfter(limite)) continue;
            if (notificaciones.notificarUnaVez(c.getMascota().getTutor().getIdUsuario(), "CITA_RECORDATORIO",
                    "Recordatorio de cita", c.getMascota().getNombre() + " tiene cita el " + describir(c) + " en "
                            + c.getVeterinaria().getNombre() + ".",
                    "/citas/" + c.getIdCita(), "cita-" + c.getIdCita() + "-recordatorio")) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private <T> T traducirConcurrencia(Supplier<T> op) {
        try {
            return op.get();
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException
                 | PessimisticLockingFailureException e) {
            throw new ReservaConcurrenteException(e);
        }
    }

    private static void exigirAgendable(Mascota mascota) {
        if (!mascota.isAgendable()) {
            throw new IllegalStateException("La mascota no está activa; no se le pueden agendar citas.");
        }
    }

    private void exigirServicioDeClinica(Integer idServicio, Integer idVeterinaria) {
        if (servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .noneMatch(sv -> sv.getServicio().getIdServicio().equals(idServicio))) {
            throw new IllegalArgumentException("El servicio no está disponible en la veterinaria seleccionada");
        }
    }

    private void verificarCitaDeSuVeterinaria(Cita cita, Integer idVeterinariaRecepcionista) {
        if (!cita.getVeterinaria().getIdVeterinaria().equals(idVeterinariaRecepcionista)) {
            throw new SecurityException("Esta cita no pertenece a tu veterinaria");
        }
    }

    private List<Integer> recepcionistasDe(Integer idVeterinaria) {
        return recepcionistaRepository.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .map(Recepcionista::getUsuario)
                .filter(Usuario::isActivo)
                .map(Usuario::getIdUsuario)
                .toList();
    }

    private static String describir(Cita c) {
        FormatoVista f = new FormatoVista();
        return f.fechaCorta(c.getFecha()) + " a las " + f.hora(c.getHora());
    }

    private static Integer idDe(Usuario u) {
        return u == null ? null : u.getIdUsuario();
    }

    private static String limpiar(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /** Usado por las vistas para decidir qué acciones mostrar. */
    public static boolean permitida(Cita c, EstadoCita destino, Actor actor) {
        return TransicionCita.permitida(c.getEstado(), destino, actor);
    }
}
