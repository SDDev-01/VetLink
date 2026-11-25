package com.vetlink.cita.service;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.model.TransicionCita.Actor;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.comun.config.ReglasNegocio;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.ArchivoHistorial;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.service.ArchivoClinicoService;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.notificacion.NotificacionService;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.vacuna.model.CarnetVacuna;
import com.vetlink.vacuna.repository.CarnetVacunaRepository;
import com.vetlink.vacuna.service.CarnetVacunaService;
import com.vetlink.veterinaria.model.Servicio;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Atención clínica de una cita, en tres pasos:
 * <ol>
 *   <li>{@link #iniciar}: el veterinario asignado inicia la cita CONFIRMADA dentro de la
 *       ventana [hora programada, hora + margen] según el reloj del servidor → EN_ATENCION.</li>
 *   <li>Registro: historia clínica ({@link #registrarHistorial}), vacuna ({@link #registrarVacuna})
 *       y adjuntos ({@link #adjuntar}); solo mientras la cita está EN_ATENCION.</li>
 *   <li>{@link #finalizar}: exige el registro obligatorio del servicio → ATENDIDA.</li>
 * </ol>
 * No existe otra forma de marcar una cita como atendida.
 */
@Service
public class AtencionService {

    /** Estado del botón «Atender cita» para la vista. */
    public record VentanaAtencion(boolean habilitado, String motivo, LocalDateTime abre, LocalDateTime cierra) {
    }

    private final CitaRepository citas;
    private final HistorialClinicoService historiales;
    private final CarnetVacunaService carnets;
    private final CarnetVacunaRepository carnetRepo;
    private final ArchivoClinicoService archivos;
    private final CitaEstadoService estados;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final ReglasNegocio reglas;
    private final Clock reloj;

    public AtencionService(CitaRepository citas, HistorialClinicoService historiales, CarnetVacunaService carnets,
                           CarnetVacunaRepository carnetRepo, ArchivoClinicoService archivos, CitaEstadoService estados,
                           UsuarioRepository usuarios, NotificacionService notificaciones, AuditoriaService auditoria,
                           ReglasNegocio reglas, Clock reloj) {
        this.citas = citas;
        this.historiales = historiales;
        this.carnets = carnets;
        this.carnetRepo = carnetRepo;
        this.archivos = archivos;
        this.estados = estados;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.auditoria = auditoria;
        this.reglas = reglas;
        this.reloj = reloj;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    /** Calcula si el botón «Atender cita» debe estar habilitado y por qué. */
    public VentanaAtencion ventana(Cita cita, Integer idVeterinario) {
        LocalDateTime abre = cita.getInicio();
        LocalDateTime cierra = abre.plusMinutes(reglas.margenInicioAtencionMin());
        if (!cita.getVeterinario().getIdUsuario().equals(idVeterinario)) {
            return new VentanaAtencion(false, "Esta cita está asignada a otro veterinario.", abre, cierra);
        }
        if (cita.getEstado() == EstadoCita.EN_ATENCION) {
            return new VentanaAtencion(false, "La atención ya está en curso.", abre, cierra);
        }
        if (cita.getEstado() != EstadoCita.CONFIRMADA) {
            return new VentanaAtencion(false, "Solo se pueden atender citas confirmadas (estado actual: "
                    + cita.getEstado().getEtiqueta() + ").", abre, cierra);
        }
        LocalDateTime ahora = ahora();
        if (ahora.isBefore(abre)) {
            return new VentanaAtencion(false, "Disponible desde la hora programada.", abre, cierra);
        }
        if (ahora.isAfter(cierra)) {
            return new VentanaAtencion(false, "Se superó el margen de " + reglas.margenInicioAtencionMin()
                    + " minutos para iniciar la atención.", abre, cierra);
        }
        return new VentanaAtencion(true, null, abre, cierra);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita iniciar(Integer idCita, Integer idVeterinario) {
        Cita cita = citas.bloquear(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        if (!cita.getVeterinario().getIdUsuario().equals(idVeterinario)) {
            throw new AccessDeniedException("Esta cita no está asignada a ti");
        }
        VentanaAtencion v = ventana(cita, idVeterinario);
        if (!v.habilitado()) {
            throw new IllegalStateException(v.motivo());
        }
        boolean otraEnCurso = citas.findByVeterinario_IdUsuarioAndEstado(idVeterinario, EstadoCita.EN_ATENCION)
                .stream().anyMatch(c -> !c.getIdCita().equals(idCita));
        if (otraEnCurso) {
            throw new IllegalStateException("Ya tienes una atención en curso. Finalízala antes de iniciar otra.");
        }
        Usuario vet = usuarios.findById(idVeterinario).orElseThrow();
        estados.cambiar(cita, EstadoCita.EN_ATENCION, Actor.VETERINARIO, vet, "Atención iniciada");
        cita.setInicioAtencion(ahora());
        citas.save(cita);
        auditoria.registrar(idVeterinario, "CITA_INICIAR_ATENCION", "cita", idCita, null);
        return cita;
    }

    /** Cita EN ATENCIÓN del veterinario asignado, o excepción. */
    public Cita enAtencion(Integer idCita, Integer idVeterinario) {
        Cita cita = citas.findById(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        exigirEnAtencion(cita, idVeterinario);
        return cita;
    }

    private void exigirEnAtencion(Cita cita, Integer idVeterinario) {
        if (!cita.getVeterinario().getIdUsuario().equals(idVeterinario)) {
            throw new AccessDeniedException("Esta cita no está asignada a ti");
        }
        if (cita.getEstado() != EstadoCita.EN_ATENCION) {
            throw new IllegalStateException("Los registros clínicos solo se pueden crear durante una cita en atención.");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HistorialClinico registrarHistorial(Integer idCita, HistorialClinicoForm form, Integer idVeterinario) {
        Cita cita = citas.bloquear(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        exigirEnAtencion(cita, idVeterinario);
        if (cita.getServicio().getTipoRegistro() == Servicio.TipoRegistro.NINGUNO) {
            throw new IllegalStateException("Este servicio no requiere historia clínica.");
        }
        HistorialClinico h = historiales.crearParaCita(form, cita, idVeterinario);
        auditoria.registrar(idVeterinario, "HISTORIAL_CREAR", "historial_clinico", h.getNumeroHistorial(),
                "cita=" + idCita);
        return h;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CarnetVacuna registrarVacuna(Integer idCita, Integer idVacuna, Integer idLote, String dosis,
                                        LocalDate proximaDosis, String observaciones, Integer idVeterinario) {
        Cita cita = citas.bloquear(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        exigirEnAtencion(cita, idVeterinario);
        if (cita.getServicio().getTipoRegistro() != Servicio.TipoRegistro.VACUNACION) {
            throw new IllegalStateException("Las vacunas se registran durante una cita de vacunación.");
        }
        if (dosis == null || dosis.isBlank()) throw new IllegalArgumentException("La dosis es obligatoria");
        if (proximaDosis == null) throw new IllegalArgumentException("La fecha de la próxima dosis es obligatoria");
        CarnetVacuna c = carnets.registrar(cita.getMascota().getIdMascota(), idVacuna, idLote, dosis.trim(),
                ahora().toLocalDate(), proximaDosis, observaciones, idVeterinario);
        c.setCita(cita);
        c.setVeterinaria(cita.getVeterinaria());
        carnetRepo.save(c);
        auditoria.registrar(idVeterinario, "VACUNA_REGISTRAR", "carnet_vacuna", c.getIdCarnet(), "cita=" + idCita);
        return c;
    }

    @Transactional
    public ArchivoHistorial adjuntar(Integer idCita, MultipartFile archivo, Integer idVeterinario) {
        Cita cita = enAtencion(idCita, idVeterinario);
        HistorialClinico h = historiales.buscarPorCita(cita.getIdCita())
                .orElseThrow(() -> new IllegalStateException("Guarda primero el registro clínico para adjuntar archivos."));
        ArchivoHistorial a = archivos.guardar(h, archivo, usuarios.findById(idVeterinario).orElseThrow());
        auditoria.registrar(idVeterinario, "ADJUNTO_SUBIR", "archivo_historial", a.getIdArchivo(), "historial=" + h.getNumeroHistorial());
        return a;
    }

    /** Lo que falta para poder finalizar (vacío = se puede finalizar). */
    public List<String> pendientesParaFinalizar(Cita cita) {
        List<String> faltan = new ArrayList<>();
        if (cita.getEstado() != EstadoCita.EN_ATENCION) {
            faltan.add("La cita no está en atención.");
            return faltan;
        }
        switch (cita.getServicio().getTipoRegistro()) {
            case CLINICO -> {
                if (historiales.buscarPorCita(cita.getIdCita()).isEmpty()) faltan.add("Guardar el registro clínico.");
            }
            case VACUNACION -> {
                if (carnetRepo.findByCita_IdCita(cita.getIdCita()).isEmpty()) faltan.add("Registrar la vacuna aplicada.");
            }
            case NINGUNO -> {
            }
        }
        return faltan;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita finalizar(Integer idCita, Integer idVeterinario) {
        Cita cita = citas.bloquear(idCita).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        exigirEnAtencion(cita, idVeterinario);
        List<String> faltan = pendientesParaFinalizar(cita);
        if (!faltan.isEmpty()) {
            throw new IllegalStateException("No se puede finalizar: " + String.join(" ", faltan));
        }
        Usuario vet = usuarios.findById(idVeterinario).orElseThrow();
        estados.cambiar(cita, EstadoCita.ATENDIDA, Actor.VETERINARIO, vet, "Atención finalizada");
        cita.setFinAtencion(ahora());
        citas.save(cita);
        auditoria.registrar(idVeterinario, "CITA_FINALIZAR_ATENCION", "cita", idCita, null);
        notificaciones.notificar(cita.getMascota().getTutor().getIdUsuario(), "CITA_ATENDIDA", "Atención finalizada",
                cita.getMascota().getNombre() + " fue atendido(a) en " + cita.getVeterinaria().getNombre()
                        + ". El registro ya está en su historial clínico.",
                "/historial/mias", "cita-" + idCita + "-atendida");
        return cita;
    }
}
