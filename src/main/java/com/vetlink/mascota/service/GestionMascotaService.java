package com.vetlink.mascota.service;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.cita.service.CitaService;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.model.MascotaEvento;
import com.vetlink.mascota.model.SituacionMascota;
import com.vetlink.mascota.model.TransferenciaMascota;
import com.vetlink.mascota.repository.MascotaEventoRepository;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.mascota.repository.TransferenciaMascotaRepository;
import com.vetlink.notificacion.NotificacionService;
import com.vetlink.pasaporte.model.Pasaporte;
import com.vetlink.pasaporte.service.PasaporteService;
import com.vetlink.usuario.model.Tutor;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.TutorRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Ciclo de vida de la mascota sin borrado: transferencia de propiedad (con aceptación del
 * nuevo tutor), pérdida, hallazgo y fallecimiento. Todo queda en mascota_evento y auditoría.
 * El historial clínico, las vacunas y las citas pasadas siguen ligados a la mascota.
 */
@Service
public class GestionMascotaService {

    private static final Logger log = LoggerFactory.getLogger(GestionMascotaService.class);

    private final MascotaRepository mascotas;
    private final TransferenciaMascotaRepository transferencias;
    private final MascotaEventoRepository eventos;
    private final TutorRepository tutores;
    private final UsuarioRepository usuarios;
    private final CitaRepository citas;
    private final CitaService citaService;
    private final PasaporteService pasaportes;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final Clock reloj;

    public GestionMascotaService(MascotaRepository mascotas, TransferenciaMascotaRepository transferencias,
                                 MascotaEventoRepository eventos, TutorRepository tutores, UsuarioRepository usuarios,
                                 CitaRepository citas, CitaService citaService, PasaporteService pasaportes,
                                 NotificacionService notificaciones, AuditoriaService auditoria, Clock reloj) {
        this.mascotas = mascotas;
        this.transferencias = transferencias;
        this.eventos = eventos;
        this.tutores = tutores;
        this.usuarios = usuarios;
        this.citas = citas;
        this.citaService = citaService;
        this.pasaportes = pasaportes;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    // ------------------------------------------------------------------
    // Transferencias
    // ------------------------------------------------------------------

    @Transactional
    public TransferenciaMascota solicitarTransferencia(Integer idMascota, Integer idTutorOrigen, String destino,
                                                       String mensaje) {
        Mascota m = mascotas.bloquear(idMascota).orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        exigirDuenio(m, idTutorOrigen);
        if (m.getEstado() == null || m.getEstado() != 1 || m.getSituacionSegura() == SituacionMascota.FALLECIDA) {
            throw new IllegalStateException("Esta mascota no se puede transferir en su estado actual.");
        }
        Tutor tDestino = tutores.buscarPorDocumentoOCorreo(destino)
                .orElseThrow(() -> new IllegalArgumentException("No hay un tutor registrado con ese documento o correo."));
        Usuario uDestino = tDestino.getUsuario();
        if (uDestino.getIdUsuario().equals(idTutorOrigen)) {
            throw new IllegalArgumentException("No puedes transferirte una mascota a ti mismo.");
        }
        if (!uDestino.isActivo()) {
            throw new IllegalArgumentException("La cuenta del tutor de destino no está activa.");
        }
        if (transferencias.findFirstByMascota_IdMascotaAndEstado(idMascota, TransferenciaMascota.Estado.PENDIENTE).isPresent()) {
            throw new IllegalStateException("Ya hay una transferencia pendiente para esta mascota.");
        }
        if (tieneCitasVigentes(idMascota)) {
            throw new IllegalStateException("La mascota tiene citas pendientes, confirmadas o en atención. "
                    + "Cancélalas o espera a que terminen antes de transferirla.");
        }
        TransferenciaMascota t = new TransferenciaMascota();
        t.setMascota(m);
        t.setTutorOrigen(m.getTutor());
        t.setTutorDestino(uDestino);
        t.setMensaje(mensaje == null || mensaje.isBlank() ? null : mensaje.trim().substring(0, Math.min(300, mensaje.trim().length())));
        t.setCreadaEn(ahora());
        try {
            t = transferencias.saveAndFlush(t);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Ya hay una transferencia pendiente para esta mascota.");
        }
        evento(m, "TRANSFERENCIA_SOLICITADA", "Solicitada a " + nombre(uDestino), null, null, m.getTutor());
        auditoria.registrar(idTutorOrigen, "MASCOTA_TRANSFERENCIA_SOLICITAR", "mascota", idMascota,
                "transferencia=" + t.getIdTransferencia() + " destino=" + uDestino.getIdUsuario());
        notificaciones.notificar(uDestino.getIdUsuario(), "TRANSFERENCIA_SOLICITADA", "Te quieren transferir una mascota",
                nombre(m.getTutor()) + " quiere transferirte a " + m.getNombre() + ". Revisa la solicitud.",
                "/mascotas/transferencias", "transferencia-" + t.getIdTransferencia());
        return t;
    }

    @Transactional
    public TransferenciaMascota aceptar(Integer idTransferencia, Integer idTutorDestino) {
        TransferenciaMascota t = pendienteDe(idTransferencia);
        if (!t.getTutorDestino().getIdUsuario().equals(idTutorDestino)) {
            throw new AccessDeniedException("Esta solicitud no está dirigida a ti");
        }
        Mascota m = mascotas.bloquear(t.getMascota().getIdMascota()).orElseThrow();
        if (!m.getTutor().getIdUsuario().equals(t.getTutorOrigen().getIdUsuario())) {
            t.setEstado(TransferenciaMascota.Estado.CANCELADA);
            t.setResueltaEn(ahora());
            throw new IllegalStateException("La mascota ya no pertenece a quien la ofreció; la solicitud se anuló.");
        }
        if (tieneCitasVigentes(m.getIdMascota())) {
            throw new IllegalStateException("La mascota tiene citas vigentes; la transferencia no se puede completar todavía.");
        }
        revocarPasaportes(m.getIdMascota());
        Usuario anterior = m.getTutor();
        Usuario nuevo = usuarios.findById(idTutorDestino).orElseThrow();
        m.setTutor(nuevo);
        mascotas.save(m);
        t.setEstado(TransferenciaMascota.Estado.ACEPTADA);
        t.setResueltaEn(ahora());
        transferencias.save(t);
        evento(m, "TRANSFERIDA", "De " + nombre(anterior) + " a " + nombre(nuevo),
                String.valueOf(anterior.getIdUsuario()), String.valueOf(nuevo.getIdUsuario()), nuevo);
        auditoria.registrar(idTutorDestino, "MASCOTA_TRANSFERENCIA_ACEPTAR", "mascota", m.getIdMascota(),
                "transferencia=" + idTransferencia + " origen=" + anterior.getIdUsuario());
        notificaciones.notificar(anterior.getIdUsuario(), "TRANSFERENCIA_ACEPTADA", "Transferencia aceptada",
                nombre(nuevo) + " aceptó la transferencia de " + m.getNombre() + ".", "/mascotas/transferencias",
                "transferencia-" + idTransferencia + "-resuelta");
        return t;
    }

    @Transactional
    public TransferenciaMascota rechazar(Integer idTransferencia, Integer idTutorDestino) {
        TransferenciaMascota t = pendienteDe(idTransferencia);
        if (!t.getTutorDestino().getIdUsuario().equals(idTutorDestino)) {
            throw new AccessDeniedException("Esta solicitud no está dirigida a ti");
        }
        t.setEstado(TransferenciaMascota.Estado.RECHAZADA);
        t.setResueltaEn(ahora());
        transferencias.save(t);
        evento(t.getMascota(), "TRANSFERENCIA_RECHAZADA", "Rechazada por " + nombre(t.getTutorDestino()), null, null,
                t.getTutorDestino());
        auditoria.registrar(idTutorDestino, "MASCOTA_TRANSFERENCIA_RECHAZAR", "mascota", t.getMascota().getIdMascota(),
                "transferencia=" + idTransferencia);
        notificaciones.notificar(t.getTutorOrigen().getIdUsuario(), "TRANSFERENCIA_RECHAZADA", "Transferencia rechazada",
                nombre(t.getTutorDestino()) + " rechazó la transferencia de " + t.getMascota().getNombre()
                        + ". La mascota sigue contigo.", "/mascotas/transferencias",
                "transferencia-" + idTransferencia + "-resuelta");
        return t;
    }

    @Transactional
    public TransferenciaMascota cancelar(Integer idTransferencia, Integer idTutorOrigen) {
        TransferenciaMascota t = pendienteDe(idTransferencia);
        if (!t.getTutorOrigen().getIdUsuario().equals(idTutorOrigen)) {
            throw new AccessDeniedException("Solo quien solicitó la transferencia puede cancelarla");
        }
        t.setEstado(TransferenciaMascota.Estado.CANCELADA);
        t.setResueltaEn(ahora());
        transferencias.save(t);
        auditoria.registrar(idTutorOrigen, "MASCOTA_TRANSFERENCIA_CANCELAR", "mascota", t.getMascota().getIdMascota(),
                "transferencia=" + idTransferencia);
        notificaciones.notificar(t.getTutorDestino().getIdUsuario(), "TRANSFERENCIA_CANCELADA", "Transferencia cancelada",
                nombre(t.getTutorOrigen()) + " canceló la transferencia de " + t.getMascota().getNombre() + ".",
                "/mascotas/transferencias", "transferencia-" + idTransferencia);
        return t;
    }

    private TransferenciaMascota pendienteDe(Integer id) {
        TransferenciaMascota t = transferencias.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (t.getEstado() != TransferenciaMascota.Estado.PENDIENTE) {
            throw new IllegalStateException("Esta solicitud ya fue resuelta (" + t.getEstado().getEtiqueta() + ").");
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Situación de la mascota
    // ------------------------------------------------------------------

    @Transactional
    public Mascota reportarPerdida(Integer idMascota, Integer idTutor, String detalle) {
        Mascota m = mascotaDelDuenio(idMascota, idTutor);
        if (m.getSituacionSegura() != SituacionMascota.ACTIVA) {
            throw new IllegalStateException("Solo se puede reportar como perdida una mascota activa.");
        }
        return cambiarSituacion(m, SituacionMascota.PERDIDA, "MASCOTA_PERDIDA", detalle, idTutor);
    }

    @Transactional
    public Mascota reportarEncontrada(Integer idMascota, Integer idTutor, String detalle) {
        Mascota m = mascotaDelDuenio(idMascota, idTutor);
        if (m.getSituacionSegura() != SituacionMascota.PERDIDA) {
            throw new IllegalStateException("La mascota no está reportada como perdida.");
        }
        return cambiarSituacion(m, SituacionMascota.ACTIVA, "MASCOTA_ENCONTRADA", detalle, idTutor);
    }

    /** Irreversible: cancela sus citas futuras vigentes y revoca los enlaces compartidos. */
    @Transactional
    public Mascota reportarFallecimiento(Integer idMascota, Integer idTutor, String detalle) {
        Mascota m = mascotaDelDuenio(idMascota, idTutor);
        if (m.getSituacionSegura() == SituacionMascota.FALLECIDA) {
            throw new IllegalStateException("El fallecimiento ya fue reportado.");
        }
        if (citas.findByMascota_IdMascotaOrderByFechaDescHoraDesc(idMascota).stream()
                .anyMatch(c -> c.getEstado() == EstadoCita.EN_ATENCION)) {
            throw new IllegalStateException("La mascota tiene una atención en curso.");
        }
        for (Cita c : citas.proximasDeMascota(idMascota, EstadoCita.RESERVAS_VIGENTES)) {
            if (c.getInicio().isAfter(ahora())) {
                citaService.cancelar(c.getIdCita(), idTutor);
            }
        }
        transferencias.findFirstByMascota_IdMascotaAndEstado(idMascota, TransferenciaMascota.Estado.PENDIENTE)
                .ifPresent(t -> {
                    t.setEstado(TransferenciaMascota.Estado.CANCELADA);
                    t.setResueltaEn(ahora());
                });
        try {
            revocarPasaportes(idMascota);
        } catch (IllegalStateException e) {
            log.warn("No se pudieron revocar los pasaportes de la mascota {}: {}", idMascota, e.getMessage());
        }
        return cambiarSituacion(m, SituacionMascota.FALLECIDA, "MASCOTA_FALLECIDA", detalle, idTutor);
    }

    private Mascota cambiarSituacion(Mascota m, SituacionMascota nueva, String tipo, String detalle, Integer idUsuario) {
        SituacionMascota anterior = m.getSituacionSegura();
        m.setSituacion(nueva);
        mascotas.save(m);
        Usuario u = usuarios.findById(idUsuario).orElse(null);
        evento(m, tipo, detalle, anterior.name(), nueva.name(), u);
        auditoria.registrar(idUsuario, tipo, "mascota", m.getIdMascota(), anterior + " -> " + nueva);
        return m;
    }

    // ------------------------------------------------------------------
    // Peso (recepción)
    // ------------------------------------------------------------------

    @Transactional
    public Mascota actualizarPeso(Integer idMascota, Double peso, Usuario usuario) {
        if (peso == null || !(peso > 0) || peso > 1500 || peso.isNaN()) {
            throw new IllegalArgumentException("Ingresa un peso mayor que cero");
        }
        Mascota m = mascotas.bloquear(idMascota).orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        m.exigirEditable();
        Double anterior = m.getPeso();
        double redondeado = Math.round(peso * 100.0) / 100.0;
        m.setPeso(redondeado);
        mascotas.save(m);
        evento(m, "PESO_ACTUALIZADO", "Peso actualizado por recepción",
                anterior == null ? null : String.valueOf(anterior), String.valueOf(redondeado), usuario);
        auditoria.registrar(usuario.getIdUsuario(), "MASCOTA_PESO", "mascota", idMascota, anterior + " -> " + redondeado);
        return m;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<TransferenciaMascota> recibidasPendientes(Integer idTutor) {
        return transferencias.findByTutorDestino_IdUsuarioAndEstadoOrderByCreadaEnDesc(idTutor,
                TransferenciaMascota.Estado.PENDIENTE);
    }

    public List<TransferenciaMascota> enviadas(Integer idTutor) {
        return transferencias.findByTutorOrigen_IdUsuarioOrderByCreadaEnDesc(idTutor);
    }

    public List<TransferenciaMascota> recibidas(Integer idTutor) {
        return transferencias.findByTutorDestino_IdUsuarioOrderByCreadaEnDesc(idTutor);
    }

    public java.util.Optional<TransferenciaMascota> pendienteDeMascota(Integer idMascota) {
        return transferencias.findFirstByMascota_IdMascotaAndEstado(idMascota, TransferenciaMascota.Estado.PENDIENTE);
    }

    public List<MascotaEvento> eventos(Integer idMascota) {
        return eventos.findByMascota_IdMascotaOrderByFechaDesc(idMascota);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private boolean tieneCitasVigentes(Integer idMascota) {
        return citas.existsByMascota_IdMascotaAndEstadoIn(idMascota,
                EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, EstadoCita.EN_ATENCION));
    }

    /** Los enlaces compartidos por el dueño anterior dejan de funcionar al cambiar de dueño. */
    private void revocarPasaportes(Integer idMascota) {
        try {
            for (Pasaporte p : pasaportes.listar(idMascota)) {
                if (p.activo()) pasaportes.revocar(p.token());
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudieron revocar los enlaces compartidos del pasaporte de salud. "
                    + "Inténtalo de nuevo en unos minutos.", e);
        }
    }

    private Mascota mascotaDelDuenio(Integer idMascota, Integer idTutor) {
        Mascota m = mascotas.bloquear(idMascota).orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        exigirDuenio(m, idTutor);
        return m;
    }

    private static void exigirDuenio(Mascota m, Integer idTutor) {
        if (!com.vetlink.security.AccesoPacienteService.esDuenioVigente(m, idTutor)) {
            throw new AccessDeniedException("No tienes permiso sobre esta mascota");
        }
    }

    private void evento(Mascota m, String tipo, String detalle, String antes, String despues, Usuario u) {
        MascotaEvento e = new MascotaEvento();
        e.setMascota(m);
        e.setTipo(tipo);
        e.setDetalle(detalle == null || detalle.isBlank() ? null : detalle.trim().substring(0, Math.min(500, detalle.trim().length())));
        e.setValorAnterior(antes);
        e.setValorNuevo(despues);
        e.setUsuario(u);
        e.setFecha(ahora());
        eventos.save(e);
    }

    private static String nombre(Usuario u) {
        return u.getNombre() + " " + u.getApellido();
    }
}
