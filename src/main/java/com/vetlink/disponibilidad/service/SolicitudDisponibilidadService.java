package com.vetlink.disponibilidad.service;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.cita.model.Cita;
import com.vetlink.comun.config.ReglasNegocio;
import com.vetlink.comun.web.FormatoVista;
import com.vetlink.disponibilidad.model.*;
import com.vetlink.disponibilidad.repository.AusenciaVeterinarioRepository;
import com.vetlink.disponibilidad.repository.HorarioVeterinarioRepository;
import com.vetlink.disponibilidad.repository.SolicitudDisponibilidadRepository;
import com.vetlink.disponibilidad.repository.VeterinarioServicioRepository;
import com.vetlink.notificacion.NotificacionService;
import com.vetlink.usuario.model.Recepcionista;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.veterinaria.model.ServicioVeterinaria;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Solicitudes de disponibilidad: el veterinario propone (horario, ausencias, servicios) y
 * la recepción de su clínica aprueba o rechaza. Al aprobar se actualiza la disponibilidad,
 * se retiran los cupos que ya no caben, se generan los nuevos y se listan las citas afectadas
 * (que nunca se eliminan ni se reprograman en silencio).
 */
@Service
public class SolicitudDisponibilidadService {

    public record Franja(Integer diaSemana, LocalTime inicio, LocalTime fin) {
    }

    public record ResultadoAprobacion(SolicitudDisponibilidad solicitud, int cuposRetirados, int cuposCreados,
                                      List<Cita> citasAfectadas) {
    }

    private final SolicitudDisponibilidadRepository solicitudes;
    private final HorarioVeterinarioRepository horarios;
    private final AusenciaVeterinarioRepository ausencias;
    private final VeterinarioServicioRepository vetServicios;
    private final ServicioRepository servicios;
    private final ServicioVeterinariaRepository serviciosClinica;
    private final UsuarioRepository usuarios;
    private final RecepcionistaRepository recepcionistas;
    private final DisponibilidadService disponibilidad;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final ReglasNegocio reglas;
    private final Clock reloj;

    public SolicitudDisponibilidadService(SolicitudDisponibilidadRepository solicitudes, HorarioVeterinarioRepository horarios,
                                          AusenciaVeterinarioRepository ausencias, VeterinarioServicioRepository vetServicios,
                                          ServicioRepository servicios, ServicioVeterinariaRepository serviciosClinica,
                                          UsuarioRepository usuarios, RecepcionistaRepository recepcionistas,
                                          DisponibilidadService disponibilidad, NotificacionService notificaciones,
                                          AuditoriaService auditoria, ReglasNegocio reglas, Clock reloj) {
        this.solicitudes = solicitudes;
        this.horarios = horarios;
        this.ausencias = ausencias;
        this.vetServicios = vetServicios;
        this.servicios = servicios;
        this.serviciosClinica = serviciosClinica;
        this.usuarios = usuarios;
        this.recepcionistas = recepcionistas;
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
    // Veterinario: solicitudes
    // ------------------------------------------------------------------

    @Transactional
    public SolicitudDisponibilidad solicitarHorario(Usuario vet, List<Franja> franjas, String descripcion) {
        exigirVeterinario(vet);
        List<Franja> validas = franjas.stream().filter(Objects::nonNull)
                .filter(f -> f.diaSemana() != null || f.inicio() != null || f.fin() != null).toList();
        if (validas.isEmpty()) throw new IllegalArgumentException("Agrega al menos una franja de horario");
        for (Franja f : validas) {
            if (f.diaSemana() == null || f.diaSemana() < 1 || f.diaSemana() > 7) {
                throw new IllegalArgumentException("Selecciona un día válido en cada franja");
            }
            if (!DisponibilidadService.horaValida(f.inicio(), f.fin())) {
                throw new IllegalArgumentException("En cada franja la hora de inicio debe ser anterior a la de fin");
            }
        }
        Map<Integer, List<Franja>> porDia = validas.stream().collect(Collectors.groupingBy(Franja::diaSemana));
        for (List<Franja> dia : porDia.values()) {
            List<Franja> orden = dia.stream().sorted(Comparator.comparing(Franja::inicio)).toList();
            for (int i = 1; i < orden.size(); i++) {
                if (orden.get(i).inicio().isBefore(orden.get(i - 1).fin())) {
                    throw new IllegalArgumentException("Hay franjas que se cruzan el mismo día");
                }
            }
        }
        SolicitudDisponibilidad s = nueva(vet, TipoSolicitud.HORARIO, descripcion);
        for (Franja f : validas) {
            HorarioVeterinario h = new HorarioVeterinario();
            h.setVeterinario(vet);
            h.setDiaSemana(f.diaSemana());
            h.setHoraInicio(f.inicio());
            h.setHoraFin(f.fin());
            h.setEstado(EstadoAprobacion.PENDIENTE);
            h.setSolicitud(s);
            h.setCreadoEn(ahora());
            horarios.save(h);
        }
        avisarRecepcion(s);
        return s;
    }

    @Transactional
    public SolicitudDisponibilidad solicitarAusencia(Usuario vet, TipoAusencia tipo, LocalDateTime inicio, LocalDateTime fin,
                                                     String motivo) {
        exigirVeterinario(vet);
        if (tipo == null || tipo == TipoAusencia.BLOQUEO) {
            throw new IllegalArgumentException("Selecciona el tipo de novedad");
        }
        validarRango(inicio, fin);
        SolicitudDisponibilidad s = nueva(vet, TipoSolicitud.AUSENCIA,
                tipo.getEtiqueta() + ": " + new FormatoVista().fechaHora(inicio) + " – " + new FormatoVista().fechaHora(fin));
        crearAusencia(vet, tipo, inicio, fin, motivo, EstadoAprobacion.PENDIENTE, s);
        avisarRecepcion(s);
        return s;
    }

    @Transactional
    public SolicitudDisponibilidad solicitarServicios(Usuario vet, Collection<Integer> idsServicio, String descripcion) {
        exigirVeterinario(vet);
        Set<Integer> ids = idsServicio == null ? Set.of() : new LinkedHashSet<>(idsServicio);
        if (ids.isEmpty()) throw new IllegalArgumentException("Selecciona al menos un servicio");
        Integer idClinica = vet.getVeterinario().getVeterinaria().getIdVeterinaria();
        Set<Integer> ofrecidos = serviciosClinica.findByVeterinaria_IdVeterinaria(idClinica).stream()
                .map(sv -> sv.getServicio().getIdServicio()).collect(Collectors.toSet());
        if (!ofrecidos.containsAll(ids)) {
            throw new IllegalArgumentException("Solo puedes solicitar servicios que ofrece tu veterinaria");
        }
        SolicitudDisponibilidad s = nueva(vet, TipoSolicitud.SERVICIOS, descripcion);
        for (Integer id : ids) {
            VeterinarioServicio vs = new VeterinarioServicio();
            vs.setVeterinario(vet);
            vs.setServicio(servicios.findById(id).orElseThrow());
            vs.setEstado(EstadoAprobacion.PENDIENTE);
            vs.setSolicitud(s);
            vs.setCreadoEn(ahora());
            vetServicios.save(vs);
        }
        avisarRecepcion(s);
        return s;
    }

    // ------------------------------------------------------------------
    // Recepción: aprobar, rechazar, bloquear
    // ------------------------------------------------------------------

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoAprobacion aprobar(Integer idSolicitud, Usuario recepcionista, String observacion) {
        SolicitudDisponibilidad s = pendienteDeMiClinica(idSolicitud, recepcionista);
        Integer idVet = s.getVeterinario().getIdUsuario();
        switch (s.getTipo()) {
            case HORARIO -> {
                horarios.findByVeterinario_IdUsuarioAndEstadoOrderByDiaSemanaAscHoraInicioAsc(idVet, EstadoAprobacion.APROBADO)
                        .forEach(h -> h.setEstado(EstadoAprobacion.REEMPLAZADO));
                horarios.findBySolicitud_IdSolicitudOrderByDiaSemanaAscHoraInicioAsc(idSolicitud)
                        .forEach(h -> h.setEstado(EstadoAprobacion.APROBADO));
            }
            case AUSENCIA -> ausencias.findBySolicitud_IdSolicitud(idSolicitud)
                    .forEach(a -> a.setEstado(EstadoAprobacion.APROBADO));
            case SERVICIOS -> {
                vetServicios.findByVeterinario_IdUsuarioAndEstado(idVet, EstadoAprobacion.APROBADO)
                        .forEach(v -> v.setEstado(EstadoAprobacion.REEMPLAZADO));
                vetServicios.findBySolicitud_IdSolicitud(idSolicitud).forEach(v -> v.setEstado(EstadoAprobacion.APROBADO));
            }
        }
        cerrar(s, EstadoAprobacion.APROBADO, recepcionista, observacion);
        horarios.flush();
        ResultadoAprobacion r = aplicarCambios(s, recepcionista);
        notificaciones.notificar(idVet, "DISPONIBILIDAD_APROBADA", "Solicitud aprobada",
                s.getTipo().getEtiqueta() + ": tu solicitud fue aprobada.", "/disponibilidad",
                "solicitud-" + idSolicitud + "-resuelta");
        auditoria.registrar(recepcionista.getIdUsuario(), "DISPONIBILIDAD_APROBAR", "solicitud_disponibilidad", idSolicitud,
                "retirados=" + r.cuposRetirados() + " creados=" + r.cuposCreados() + " afectadas=" + r.citasAfectadas().size());
        return r;
    }

    @Transactional
    public SolicitudDisponibilidad rechazar(Integer idSolicitud, Usuario recepcionista, String observacion) {
        SolicitudDisponibilidad s = pendienteDeMiClinica(idSolicitud, recepcionista);
        switch (s.getTipo()) {
            case HORARIO -> horarios.findBySolicitud_IdSolicitudOrderByDiaSemanaAscHoraInicioAsc(idSolicitud)
                    .forEach(h -> h.setEstado(EstadoAprobacion.RECHAZADO));
            case AUSENCIA -> ausencias.findBySolicitud_IdSolicitud(idSolicitud)
                    .forEach(a -> a.setEstado(EstadoAprobacion.RECHAZADO));
            case SERVICIOS -> vetServicios.findBySolicitud_IdSolicitud(idSolicitud)
                    .forEach(v -> v.setEstado(EstadoAprobacion.RECHAZADO));
        }
        cerrar(s, EstadoAprobacion.RECHAZADO, recepcionista, observacion);
        notificaciones.notificar(s.getVeterinario().getIdUsuario(), "DISPONIBILIDAD_RECHAZADA", "Solicitud rechazada",
                s.getTipo().getEtiqueta() + (observacion == null || observacion.isBlank() ? "" : ": " + observacion.trim()),
                "/disponibilidad", "solicitud-" + idSolicitud + "-resuelta");
        auditoria.registrar(recepcionista.getIdUsuario(), "DISPONIBILIDAD_RECHAZAR", "solicitud_disponibilidad",
                idSolicitud, observacion);
        return s;
    }

    /** Bloqueo de agenda decidido por recepción: queda aprobado de inmediato. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoAprobacion bloquearAgenda(Usuario recepcionista, Integer idVeterinario, LocalDateTime inicio,
                                              LocalDateTime fin, String motivo) {
        Integer clinica = clinicaDe(recepcionista);
        Usuario vet = usuarios.findById(idVeterinario).orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        if (vet.getVeterinario() == null || vet.getVeterinario().getVeterinaria() == null
                || !vet.getVeterinario().getVeterinaria().getIdVeterinaria().equals(clinica)) {
            throw new AccessDeniedException("El veterinario no pertenece a tu veterinaria");
        }
        validarRango(inicio, fin);
        SolicitudDisponibilidad s = nueva(vet, TipoSolicitud.AUSENCIA, "Bloqueo de agenda: "
                + new FormatoVista().fechaHora(inicio) + " – " + new FormatoVista().fechaHora(fin));
        crearAusencia(vet, TipoAusencia.BLOQUEO, inicio, fin, motivo, EstadoAprobacion.APROBADO, s);
        cerrar(s, EstadoAprobacion.APROBADO, recepcionista, motivo);
        ResultadoAprobacion r = aplicarCambios(s, recepcionista);
        notificaciones.notificar(idVeterinario, "DISPONIBILIDAD_BLOQUEO", "Bloqueo en tu agenda", s.getDescripcion(),
                "/disponibilidad", "solicitud-" + s.getIdSolicitud() + "-resuelta");
        auditoria.registrar(recepcionista.getIdUsuario(), "AGENDA_BLOQUEAR", "veterinario", idVeterinario, s.getDescripcion());
        return r;
    }

    /**
     * Levanta un bloqueo de agenda vigente (o futuro) del veterinario. El bloqueo no se borra:
     * queda como ANULADO con quién y cuándo lo desbloqueó. Después se regeneran los cupos, y la
     * generación sigue respetando el horario aprobado, las demás ausencias y las citas ocupadas.
     *
     * @return cantidad de cupos nuevos generados
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int desbloquearAgenda(Integer idAusencia, Usuario recepcionista) {
        Integer clinica = clinicaDe(recepcionista);
        AusenciaVeterinario a = ausencias.findById(idAusencia)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueo no encontrado"));
        Usuario vet = a.getVeterinario();
        if (vet.getVeterinario() == null || vet.getVeterinario().getVeterinaria() == null
                || !vet.getVeterinario().getVeterinaria().getIdVeterinaria().equals(clinica)) {
            throw new AccessDeniedException("El veterinario no pertenece a tu veterinaria");
        }
        if (a.getTipo() != TipoAusencia.BLOQUEO) {
            throw new IllegalStateException("Solo se pueden desbloquear los bloqueos de agenda. Las novedades del veterinario se gestionan en sus solicitudes.");
        }
        LocalDateTime ahora = ahora();
        if (a.getEstado() != EstadoAprobacion.APROBADO || !a.getFin().isAfter(ahora)) {
            throw new IllegalStateException("Ese bloqueo ya no está vigente.");
        }
        a.setEstado(EstadoAprobacion.ANULADO);
        a.setDesbloqueadaPor(recepcionista);
        a.setDesbloqueadaEn(ahora);
        ausencias.saveAndFlush(a);
        LocalDate hoy = ahora.toLocalDate();
        int creados = disponibilidad.generarParaVeterinario(vet.getIdUsuario(), hoy, hoy.plusDays(reglas.horizonteAgendaDias()));
        FormatoVista f = new FormatoVista();
        notificaciones.notificar(vet.getIdUsuario(), "DISPONIBILIDAD_DESBLOQUEO", "Agenda desbloqueada",
                "Recepción levantó el bloqueo de tu agenda (" + f.fechaHora(a.getInicio()) + " – " + f.fechaHora(a.getFin())
                        + "). Ya se pueden agendar citas en tu horario laboral.",
                "/disponibilidad", "ausencia-" + idAusencia + "-desbloqueo");
        auditoria.registrar(recepcionista.getIdUsuario(), "AGENDA_DESBLOQUEAR", "ausencia_veterinario", idAusencia,
                "veterinario=" + vet.getIdUsuario() + " cuposCreados=" + creados);
        return creados;
    }

    /** Bloqueos de agenda aprobados que todavía no terminan (vigentes o futuros). */
    @Transactional(readOnly = true)
    public List<AusenciaVeterinario> bloqueosVigentes(Integer idVet) {
        return ausencias.findByVeterinario_IdUsuarioAndFinAfterOrderByInicioAsc(idVet, ahora()).stream()
                .filter(a -> a.getTipo() == TipoAusencia.BLOQUEO && a.getEstado() == EstadoAprobacion.APROBADO)
                .toList();
    }

    private ResultadoAprobacion aplicarCambios(SolicitudDisponibilidad s, Usuario recepcionista) {
        Integer idVet = s.getVeterinario().getIdUsuario();
        int retirados = disponibilidad.retirarCuposIncompatibles(idVet);
        LocalDate hoy = ahora().toLocalDate();
        int creados = disponibilidad.generarParaVeterinario(idVet, hoy, hoy.plusDays(reglas.horizonteAgendaDias()));
        List<Cita> afectadas = disponibilidad.citasAfectadas(idVet);
        for (Cita c : afectadas) {
            if (c.getMascota() != null) {
                notificaciones.notificar(c.getMascota().getTutor().getIdUsuario(), "CITA_AFECTADA",
                        "Tu cita podría reprogramarse",
                        "Hubo un cambio en la disponibilidad del veterinario para la cita de " + c.getMascota().getNombre()
                                + " (" + new FormatoVista().fechaCorta(c.getFecha()) + " " + new FormatoVista().hora(c.getHora())
                                + "). La clínica te propondrá otro horario; tu cita sigue vigente hasta entonces.",
                        "/citas/" + c.getIdCita(), "cita-" + c.getIdCita() + "-afectada-" + s.getIdSolicitud());
            }
        }
        return new ResultadoAprobacion(s, retirados, creados, afectadas);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<SolicitudDisponibilidad> deVeterinario(Integer idVet) {
        return solicitudes.findByVeterinario_IdUsuarioOrderByCreadaEnDesc(idVet);
    }

    public List<SolicitudDisponibilidad> pendientesDeClinica(Integer idClinica) {
        return solicitudes.findByVeterinaria_IdVeterinariaAndEstadoOrderByCreadaEnAsc(idClinica, EstadoAprobacion.PENDIENTE);
    }

    public List<SolicitudDisponibilidad> historialDeClinica(Integer idClinica) {
        return solicitudes.findByVeterinaria_IdVeterinariaOrderByCreadaEnDesc(idClinica);
    }

    public SolicitudDisponibilidad deMiClinica(Integer idSolicitud, Usuario recepcionista) {
        SolicitudDisponibilidad s = solicitudes.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!s.getVeterinaria().getIdVeterinaria().equals(clinicaDe(recepcionista))) {
            throw new AccessDeniedException("La solicitud no pertenece a tu veterinaria");
        }
        return s;
    }

    public List<HorarioVeterinario> franjasDe(Integer idSolicitud) {
        return horarios.findBySolicitud_IdSolicitudOrderByDiaSemanaAscHoraInicioAsc(idSolicitud);
    }

    public List<AusenciaVeterinario> ausenciasDe(Integer idSolicitud) {
        return ausencias.findBySolicitud_IdSolicitud(idSolicitud);
    }

    public List<VeterinarioServicio> serviciosDe(Integer idSolicitud) {
        return vetServicios.findBySolicitud_IdSolicitud(idSolicitud);
    }

    public List<HorarioVeterinario> horarioAprobado(Integer idVet) {
        return horarios.findByVeterinario_IdUsuarioAndEstadoOrderByDiaSemanaAscHoraInicioAsc(idVet, EstadoAprobacion.APROBADO);
    }

    public List<VeterinarioServicio> serviciosAprobados(Integer idVet) {
        return vetServicios.findByVeterinario_IdUsuarioAndEstado(idVet, EstadoAprobacion.APROBADO);
    }

    public List<AusenciaVeterinario> ausenciasProximas(Integer idVet) {
        return ausencias.findByVeterinario_IdUsuarioAndFinAfterOrderByInicioAsc(idVet, ahora());
    }

    public List<ServicioVeterinaria> serviciosDeClinica(Integer idClinica) {
        return serviciosClinica.findByVeterinaria_IdVeterinaria(idClinica);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private SolicitudDisponibilidad nueva(Usuario vet, TipoSolicitud tipo, String descripcion) {
        SolicitudDisponibilidad s = new SolicitudDisponibilidad();
        s.setVeterinario(vet);
        s.setVeterinaria(vet.getVeterinario().getVeterinaria());
        s.setTipo(tipo);
        s.setEstado(EstadoAprobacion.PENDIENTE);
        s.setDescripcion(descripcion == null || descripcion.isBlank() ? null
                : descripcion.trim().substring(0, Math.min(500, descripcion.trim().length())));
        s.setCreadaEn(ahora());
        return solicitudes.save(s);
    }

    private void crearAusencia(Usuario vet, TipoAusencia tipo, LocalDateTime inicio, LocalDateTime fin, String motivo,
                               EstadoAprobacion estado, SolicitudDisponibilidad s) {
        AusenciaVeterinario a = new AusenciaVeterinario();
        a.setVeterinario(vet);
        a.setTipo(tipo);
        a.setInicio(inicio);
        a.setFin(fin);
        a.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.trim().substring(0, Math.min(500, motivo.trim().length())));
        a.setEstado(estado);
        a.setSolicitud(s);
        a.setCreadoEn(ahora());
        ausencias.save(a);
    }

    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) throw new IllegalArgumentException("Indica el inicio y el fin");
        if (!inicio.isBefore(fin)) throw new IllegalArgumentException("El inicio debe ser anterior al fin");
        if (fin.isBefore(ahora())) throw new IllegalArgumentException("No puedes seleccionar una fecha anterior a la actual");
    }

    private void cerrar(SolicitudDisponibilidad s, EstadoAprobacion estado, Usuario revisor, String observacion) {
        s.setEstado(estado);
        s.setRevisadaPor(revisor);
        s.setRevisadaEn(ahora());
        s.setObservacionRevision(observacion == null || observacion.isBlank() ? null
                : observacion.trim().substring(0, Math.min(500, observacion.trim().length())));
        solicitudes.save(s);
    }

    private SolicitudDisponibilidad pendienteDeMiClinica(Integer idSolicitud, Usuario recepcionista) {
        SolicitudDisponibilidad s = deMiClinica(idSolicitud, recepcionista);
        if (s.getEstado() != EstadoAprobacion.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue resuelta");
        }
        return s;
    }

    private void avisarRecepcion(SolicitudDisponibilidad s) {
        List<Integer> ids = recepcionistas.findByVeterinaria_IdVeterinaria(s.getVeterinaria().getIdVeterinaria()).stream()
                .map(Recepcionista::getUsuario).filter(Usuario::isActivo).map(Usuario::getIdUsuario).toList();
        notificaciones.notificarVarios(ids, "DISPONIBILIDAD_SOLICITUD", "Solicitud de disponibilidad",
                s.getVeterinario().getNombre() + " " + s.getVeterinario().getApellido() + " · " + s.getTipo().getEtiqueta(),
                "/recepcion/disponibilidad", "solicitud-" + s.getIdSolicitud());
        auditoria.registrar(s.getVeterinario().getIdUsuario(), "DISPONIBILIDAD_SOLICITAR", "solicitud_disponibilidad",
                s.getIdSolicitud(), s.getTipo().name());
    }

    private static void exigirVeterinario(Usuario vet) {
        if (vet.getVeterinario() == null || vet.getVeterinario().getVeterinaria() == null) {
            throw new AccessDeniedException("Solo un veterinario con clínica puede registrar su disponibilidad");
        }
    }

    private static Integer clinicaDe(Usuario recepcionista) {
        if (recepcionista.getRecepcionista() == null) throw new AccessDeniedException("Solo recepción aprueba solicitudes");
        return recepcionista.getRecepcionista().getVeterinaria().getIdVeterinaria();
    }
}
