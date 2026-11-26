package com.vetlink.veterinaria.service;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.comun.validacion.Textos;
import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService;
import com.vetlink.disponibilidad.service.SolicitudDisponibilidadService.Franja;
import com.vetlink.usuario.model.Recepcionista;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.veterinaria.model.CorreoVeterinaria;
import com.vetlink.veterinaria.model.Servicio;
import com.vetlink.veterinaria.model.ServicioVeterinaria;
import com.vetlink.veterinaria.model.ServicioVeterinariaId;
import com.vetlink.veterinaria.model.TelefonoVeterinaria;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.CorreoVeterinariaRepository;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.repository.TelefonoVeterinariaRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * REC-09: gestión de «Mi veterinaria» por la recepción. Todas las operaciones se limitan a
 * la clínica de quien las hace (el id de clínica nunca viene del navegador) y los cambios de
 * horario y servicios se reflejan en el agendamiento (se retiran o regeneran los cupos libres;
 * las citas ya reservadas y el historial no se tocan).
 */
@Service
public class GestionClinicaService {

    public static final Set<String> TURNOS = Set.of("MAÑANA", "TARDE", "NOCHE", "COMPLETO");
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final VeterinariaRepository veterinarias;
    private final UsuarioRepository usuarios;
    private final RecepcionistaRepository recepcionistas;
    private final ServicioRepository servicios;
    private final ServicioVeterinariaRepository serviciosClinica;
    private final TelefonoVeterinariaRepository telefonos;
    private final CorreoVeterinariaRepository correos;
    private final CitaRepository citas;
    private final SolicitudDisponibilidadService solicitudes;
    private final DisponibilidadService disponibilidad;
    private final AuditoriaService auditoria;
    private final CatalogoServiciosService catalogoServicios;
    private final Clock reloj;

    public GestionClinicaService(VeterinariaRepository veterinarias, UsuarioRepository usuarios,
                                 RecepcionistaRepository recepcionistas, ServicioRepository servicios,
                                 ServicioVeterinariaRepository serviciosClinica, TelefonoVeterinariaRepository telefonos,
                                 CorreoVeterinariaRepository correos, CitaRepository citas,
                                 SolicitudDisponibilidadService solicitudes, DisponibilidadService disponibilidad,
                                 AuditoriaService auditoria, CatalogoServiciosService catalogoServicios, Clock reloj) {
        this.veterinarias = veterinarias;
        this.usuarios = usuarios;
        this.recepcionistas = recepcionistas;
        this.servicios = servicios;
        this.serviciosClinica = serviciosClinica;
        this.telefonos = telefonos;
        this.correos = correos;
        this.citas = citas;
        this.solicitudes = solicitudes;
        this.disponibilidad = disponibilidad;
        this.auditoria = auditoria;
        this.catalogoServicios = catalogoServicios;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------
    // Contacto de la clínica
    // ------------------------------------------------------------------

    @Transactional
    public void actualizarContacto(Usuario recepcionista, String telefono, String correo) {
        Veterinaria v = clinicaDe(recepcionista);
        String tel = telefono == null ? "" : telefono.strip();
        String mail = correo == null ? "" : correo.strip();
        if (!tel.isEmpty() && !Textos.esTelefonoValido(tel)) throw new IllegalArgumentException(Textos.MENSAJE_TELEFONO);
        if (!mail.isEmpty() && (mail.length() > 45 || !CORREO.matcher(mail).matches())) {
            throw new IllegalArgumentException("Ingresa un correo válido para la veterinaria (máx. 45 caracteres)");
        }
        if (!tel.isEmpty()) {
            var lista = telefonos.findByVeterinaria_IdVeterinaria(v.getIdVeterinaria());
            TelefonoVeterinaria t = lista.isEmpty() ? new TelefonoVeterinaria() : lista.get(0);
            t.setVeterinaria(v);
            t.setNumeroTelefono(tel);
            telefonos.save(t);
        }
        if (!mail.isEmpty()) {
            var lista = correos.findByVeterinaria_IdVeterinaria(v.getIdVeterinaria());
            CorreoVeterinaria c = lista.isEmpty() ? new CorreoVeterinaria() : lista.get(0);
            c.setVeterinaria(v);
            c.setCorreo(mail.toLowerCase(Locale.ROOT));
            correos.save(c);
        }
        auditoria.registrar(recepcionista.getIdUsuario(), "VETERINARIA_CONTACTO", "veterinaria", v.getIdVeterinaria(), null);
    }

    // ------------------------------------------------------------------
    // Equipo
    // ------------------------------------------------------------------

    /** Horario laboral de un veterinario asignado directamente por la recepción (queda trazado como solicitud aprobada). */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SolicitudDisponibilidadService.ResultadoAprobacion asignarHorarioVeterinario(Usuario recepcionista, Integer idVeterinario,
                                                                                       List<Franja> franjas) {
        Usuario vet = veterinarioDeMiClinica(recepcionista, idVeterinario);
        var s = solicitudes.solicitarHorario(vet, franjas, "Horario asignado por recepción");
        return solicitudes.aprobar(s.getIdSolicitud(), recepcionista, "Asignado desde «Mi veterinaria»");
    }

    /** Servicios que atiende un veterinario, asignados por la recepción. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SolicitudDisponibilidadService.ResultadoAprobacion asignarServiciosVeterinario(Usuario recepcionista, Integer idVeterinario,
                                                                                         Collection<Integer> idsServicio) {
        Usuario vet = veterinarioDeMiClinica(recepcionista, idVeterinario);
        var s = solicitudes.solicitarServicios(vet, idsServicio, "Servicios asignados por recepción");
        return solicitudes.aprobar(s.getIdSolicitud(), recepcionista, "Asignado desde «Mi veterinaria»");
    }

    /** Horario laboral (turno) de otra recepcionista de la misma clínica. */
    @Transactional
    public void cambiarTurnoRecepcionista(Usuario principal, Integer idRecepcionista, String turno) {
        Veterinaria v = clinicaDe(principal);
        Recepcionista r = recepcionistas.findById(idRecepcionista)
                .orElseThrow(() -> new IllegalArgumentException("Recepcionista no encontrada"));
        if (!r.getVeterinaria().getIdVeterinaria().equals(v.getIdVeterinaria())) {
            throw new AccessDeniedException("Esa recepcionista no pertenece a tu veterinaria");
        }
        String t = turno == null ? "" : turno.strip().toUpperCase(Locale.ROOT);
        if (!TURNOS.contains(t)) throw new IllegalArgumentException("Selecciona un turno válido");
        r.setTurno(t);
        recepcionistas.save(r);
        auditoria.registrar(principal.getIdUsuario(), "RECEPCIONISTA_TURNO", "recepcionista", idRecepcionista, t);
    }

    // ------------------------------------------------------------------
    // Servicios
    // ------------------------------------------------------------------

    /**
     * Agrega un servicio al catálogo de la clínica: uno del catálogo general (idServicio) o, con
     * la opción «Otros», uno escrito por la recepción (nombre). Un «Otros» que ya existe en el
     * catálogo (sin importar mayúsculas, espacios ni tildes) se reutiliza: no se duplican servicios.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ServicioVeterinaria agregarServicio(Usuario recepcionista, Integer idServicio, String nombre, String descripcion,
                                               Double precio, Integer duracion, Servicio.TipoRegistro tipoRegistro) {
        Veterinaria v = clinicaDe(recepcionista);
        validarPrecioDuracion(precio, duracion);
        Servicio servicio;
        if (idServicio != null) {
            servicio = servicios.findById(idServicio)
                    .orElseThrow(() -> new IllegalArgumentException("Selecciona un servicio del listado"));
        } else if (nombre != null && !nombre.isBlank()) {
            servicio = catalogoServicios.obtenerOCrear(nombre, descripcion, tipoRegistro);
        } else {
            throw new IllegalArgumentException("Selecciona un servicio del listado o elige «Otros» y escribe su nombre");
        }
        if (serviciosClinica.existsById(new ServicioVeterinariaId(servicio.getIdServicio(), v.getIdVeterinaria()))) {
            throw new IllegalArgumentException("«" + servicio.getNombreServicio() + "» ya está en el catálogo de tu veterinaria");
        }
        ServicioVeterinaria sv = new ServicioVeterinaria();
        sv.setServicio(servicio);
        sv.setVeterinaria(v);
        sv.setPrecio(precio);
        sv.setDuracionAprox(duracion);
        sv = serviciosClinica.saveAndFlush(sv);
        auditoria.registrar(recepcionista.getIdUsuario(), "SERVICIO_AGREGAR", "servicio", servicio.getIdServicio(),
                "veterinaria=" + v.getIdVeterinaria());
        disponibilidad.generarAgendaClinica(v.getIdVeterinaria());
        return sv;
    }

    /** Catálogo general de servicios (opciones del selector). */
    public java.util.List<Servicio> catalogoServicios() {
        return catalogoServicios.catalogo();
    }

    /** Compatibilidad: agregar por nombre (equivale a la opción «Otros»). */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ServicioVeterinaria agregarServicio(Usuario recepcionista, String nombre, String descripcion, Double precio,
                                               Integer duracion, Servicio.TipoRegistro tipoRegistro) {
        return agregarServicio(recepcionista, null, nombre, descripcion, precio, duracion, tipoRegistro);
    }

    /** Cambia precio y duración. Si cambia la duración, los cupos libres futuros se regeneran con la nueva. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void actualizarServicio(Usuario recepcionista, Integer idServicio, Double precio, Integer duracion) {
        Veterinaria v = clinicaDe(recepcionista);
        validarPrecioDuracion(precio, duracion);
        ServicioVeterinaria sv = serviciosClinica.findById(new ServicioVeterinariaId(idServicio, v.getIdVeterinaria()))
                .orElseThrow(() -> new IllegalArgumentException("Ese servicio no está en el catálogo de tu veterinaria"));
        boolean cambiaDuracion = !duracion.equals(sv.getDuracionAprox());
        sv.setPrecio(precio);
        sv.setDuracionAprox(duracion);
        serviciosClinica.saveAndFlush(sv);
        if (cambiaDuracion) {
            retirarCuposLibres(v.getIdVeterinaria(), idServicio, "Cupo retirado: cambió la duración del servicio.");
            disponibilidad.generarAgendaClinica(v.getIdVeterinaria());
        }
        auditoria.registrar(recepcionista.getIdUsuario(), "SERVICIO_EDITAR", "servicio", idServicio,
                "precio=" + precio + " duracion=" + duracion);
    }

    /**
     * Deja de ofrecer un servicio: se quita del catálogo de la clínica y se retiran sus cupos
     * libres futuros. Las citas reservadas y las ya prestadas se conservan.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int retirarServicio(Usuario recepcionista, Integer idServicio) {
        Veterinaria v = clinicaDe(recepcionista);
        ServicioVeterinariaId id = new ServicioVeterinariaId(idServicio, v.getIdVeterinaria());
        ServicioVeterinaria sv = serviciosClinica.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ese servicio no está en el catálogo de tu veterinaria"));
        int retirados = retirarCuposLibres(v.getIdVeterinaria(), idServicio, "Cupo retirado: la clínica dejó de ofrecer el servicio.");
        serviciosClinica.delete(sv);
        auditoria.registrar(recepcionista.getIdUsuario(), "SERVICIO_RETIRAR", "servicio", idServicio,
                "veterinaria=" + v.getIdVeterinaria() + " cuposRetirados=" + retirados);
        return retirados;
    }

    private int retirarCuposLibres(Integer idClinica, Integer idServicio, String nota) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        int n = 0;
        for (Cita c : citas.cuposEntre(idClinica, idServicio, null, hoy, hoy.plusYears(1))) {
            if (c.getInicio().isBefore(ahora)) continue;
            if (Boolean.TRUE.equals(c.getGeneradaAuto())) {
                citas.delete(c);
            } else {
                c.setEstado(com.vetlink.cita.model.EstadoCita.CANCELADA);
                c.setObservacionesAdmin(nota);
            }
            n++;
        }
        citas.flush();
        return n;
    }

    private static void validarPrecioDuracion(Double precio, Integer duracion) {
        if (precio == null || precio < 0 || precio > 100_000_000) {
            throw new IllegalArgumentException("Ingresa un precio válido (0 o mayor)");
        }
        if (duracion == null || duracion < 5 || duracion > 480 || duracion % 5 != 0) {
            throw new IllegalArgumentException("La duración debe estar entre 5 y 480 minutos, en múltiplos de 5");
        }
    }

    // ------------------------------------------------------------------

    private Veterinaria clinicaDe(Usuario recepcionista) {
        if (recepcionista.getRecepcionista() == null) {
            throw new AccessDeniedException("Solo la recepción administra la veterinaria");
        }
        Integer id = recepcionista.getRecepcionista().getVeterinaria().getIdVeterinaria();
        return veterinarias.findById(id).orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
    }

    private Usuario veterinarioDeMiClinica(Usuario recepcionista, Integer idVeterinario) {
        Veterinaria v = clinicaDe(recepcionista);
        Usuario vet = usuarios.findById(idVeterinario).orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        if (vet.getVeterinario() == null || vet.getVeterinario().getVeterinaria() == null
                || !vet.getVeterinario().getVeterinaria().getIdVeterinaria().equals(v.getIdVeterinaria())) {
            throw new AccessDeniedException("Ese veterinario no pertenece a tu veterinaria");
        }
        return vet;
    }
}
