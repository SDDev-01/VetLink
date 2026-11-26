package com.vetlink.disponibilidad.service;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.comun.config.ReglasNegocio;
import com.vetlink.disponibilidad.model.AusenciaVeterinario;
import com.vetlink.disponibilidad.model.EstadoAprobacion;
import com.vetlink.disponibilidad.model.HorarioVeterinario;
import com.vetlink.disponibilidad.repository.AusenciaVeterinarioRepository;
import com.vetlink.disponibilidad.repository.HorarioVeterinarioRepository;
import com.vetlink.disponibilidad.repository.VeterinarioServicioRepository;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.model.HorarioVeterinaria;
import com.vetlink.veterinaria.model.ServicioVeterinaria;
import com.vetlink.veterinaria.repository.HorarioVeterinariaRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Disponibilidad real de los veterinarios y generación automática de cupos.
 *
 * <p>Un veterinario puede atender en [inicio, fin) si: su usuario está activo, el intervalo
 * cae dentro de una franja APROBADA de su horario (recortada al horario de la clínica, si la
 * clínica lo tiene definido) y no hay una ausencia APROBADA que lo cruce.</p>
 *
 * <p>La generación es idempotente: un cupo candidato solo se crea si no se cruza con ningún
 * cupo DISPONIBLE ni con ninguna cita que ocupe horario. Ejecutarla dos veces no crea nada nuevo.</p>
 */
@Service
public class DisponibilidadService {

    public record Intervalo(LocalDateTime inicio, LocalDateTime fin) {
        public boolean contiene(LocalDateTime desde, LocalDateTime hasta) {
            return !desde.isBefore(inicio) && !hasta.isAfter(fin);
        }

        public boolean seCruza(LocalDateTime desde, LocalDateTime hasta) {
            return inicio.isBefore(hasta) && desde.isBefore(fin);
        }
    }

    public record ResultadoGeneracion(int cuposCreados, int veterinarios, LocalDate desde, LocalDate hasta) {
    }

    private static final Set<EstadoCita> BLOQUEAN_GENERACION;

    static {
        Set<EstadoCita> s = EnumSet.copyOf(EstadoCita.OCUPAN_HORARIO);
        s.add(EstadoCita.DISPONIBLE);
        BLOQUEAN_GENERACION = Collections.unmodifiableSet(s);
    }

    private final HorarioVeterinarioRepository horarios;
    private final AusenciaVeterinarioRepository ausencias;
    private final VeterinarioServicioRepository vetServicios;
    private final HorarioVeterinariaRepository horariosClinica;
    private final ServicioVeterinariaRepository serviciosClinica;
    private final VeterinarioRepository veterinarios;
    private final CitaRepository citas;
    private final ReglasNegocio reglas;
    private final Clock reloj;

    public DisponibilidadService(HorarioVeterinarioRepository horarios, AusenciaVeterinarioRepository ausencias,
                                 VeterinarioServicioRepository vetServicios, HorarioVeterinariaRepository horariosClinica,
                                 ServicioVeterinariaRepository serviciosClinica, VeterinarioRepository veterinarios,
                                 CitaRepository citas, ReglasNegocio reglas, Clock reloj) {
        this.horarios = horarios;
        this.ausencias = ausencias;
        this.vetServicios = vetServicios;
        this.horariosClinica = horariosClinica;
        this.serviciosClinica = serviciosClinica;
        this.veterinarios = veterinarios;
        this.citas = citas;
        this.reglas = reglas;
        this.reloj = reloj;
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    // ------------------------------------------------------------------
    // Consultas de disponibilidad
    // ------------------------------------------------------------------

    public boolean tieneHorarioAprobado(Integer idVeterinario) {
        return horarios.existsByVeterinario_IdUsuarioAndEstado(idVeterinario, EstadoAprobacion.APROBADO);
    }

    public boolean autorizadoPara(Integer idVeterinario, Integer idServicio) {
        return vetServicios.existsByVeterinario_IdUsuarioAndServicio_IdServicioAndEstado(
                idVeterinario, idServicio, EstadoAprobacion.APROBADO);
    }

    /** ¿Tiene algún servicio autorizado? (los veterinarios sin ninguno se tratan como registros antiguos). */
    public boolean tieneServiciosAutorizados(Integer idVeterinario) {
        return !vetServicios.findByVeterinario_IdUsuarioAndEstado(idVeterinario, EstadoAprobacion.APROBADO).isEmpty();
    }

    public Set<Integer> serviciosAutorizados(Integer idVeterinario) {
        return vetServicios.findByVeterinario_IdUsuarioAndEstado(idVeterinario, EstadoAprobacion.APROBADO).stream()
                .map(vs -> vs.getServicio().getIdServicio()).collect(Collectors.toSet());
    }

    /** Duración del servicio en la clínica (minutos), o la duración por defecto. */
    public int duracionDe(Integer idVeterinaria, Integer idServicio) {
        return serviciosClinica.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .filter(sv -> sv.getServicio().getIdServicio().equals(idServicio))
                .map(ServicioVeterinaria::getDuracionAprox)
                .filter(d -> d != null && d > 0)
                .findFirst()
                .orElse(reglas.duracionServicioPorDefectoMin());
    }

    /** Veterinarios activos de la clínica autorizados para el servicio. */
    public List<Usuario> veterinariosCompatibles(Integer idVeterinaria, Integer idServicio) {
        return veterinarios.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .map(Veterinario::getUsuario)
                .filter(Usuario::isActivo)
                .filter(u -> autorizadoPara(u.getIdUsuario(), idServicio))
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Franjas en las que el veterinario trabaja esa fecha, ya sin ausencias aprobadas. */
    public List<Intervalo> franjasLaborales(Usuario veterinario, LocalDate fecha) {
        Integer idVet = veterinario.getIdUsuario();
        int dia = fecha.getDayOfWeek().getValue();
        List<Intervalo> base = horarios
                .findByVeterinario_IdUsuarioAndEstadoOrderByDiaSemanaAscHoraInicioAsc(idVet, EstadoAprobacion.APROBADO)
                .stream()
                .filter(h -> h.getDiaSemana() == dia)
                .map(h -> new Intervalo(fecha.atTime(h.getHoraInicio()), fecha.atTime(h.getHoraFin())))
                .toList();
        if (base.isEmpty()) return List.of();

        Integer idClinica = veterinario.getVeterinario() != null && veterinario.getVeterinario().getVeterinaria() != null
                ? veterinario.getVeterinario().getVeterinaria().getIdVeterinaria() : null;
        List<Intervalo> franjas = idClinica == null ? base : recortarAHorarioClinica(base, idClinica, fecha);
        List<AusenciaVeterinario> aus = ausencias.enIntervalo(idVet, EstadoAprobacion.APROBADO,
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        for (AusenciaVeterinario a : aus) {
            franjas = restar(franjas, new Intervalo(a.getInicio(), a.getFin()));
        }
        return franjas;
    }

    /**
     * ¿Puede atender en [inicio, fin)? Si el veterinario no tiene ningún horario aprobado
     * (registros anteriores a V8) solo se exige estar activo y sin ausencias, salvo que
     * {@code exigirHorario} sea true.
     */
    public boolean disponibleEn(Usuario veterinario, LocalDateTime inicio, LocalDateTime fin, boolean exigirHorario) {
        if (veterinario == null || !veterinario.isActivo()) return false;
        Integer idVet = veterinario.getIdUsuario();
        if (!ausencias.enIntervalo(idVet, EstadoAprobacion.APROBADO, inicio, fin).isEmpty()) return false;
        if (!tieneHorarioAprobado(idVet)) return !exigirHorario;
        if (!inicio.toLocalDate().equals(fin.minusNanos(1).toLocalDate())) return false;
        return franjasLaborales(veterinario, inicio.toLocalDate()).stream().anyMatch(f -> f.contiene(inicio, fin));
    }

    /** ¿Está ocupado ahora mismo? (en atención o con una cita vigente en curso). */
    public boolean ocupadoAhora(Integer idVeterinario) {
        LocalDateTime ahora = ahora();
        if (!citas.findByVeterinario_IdUsuarioAndEstado(idVeterinario, EstadoCita.EN_ATENCION).isEmpty()) return true;
        return citas.deVeterinarioEnFechaConEstados(idVeterinario, ahora.toLocalDate(),
                        EnumSet.of(EstadoCita.CONFIRMADA))
                .stream().anyMatch(c -> c.seSolapaCon(ahora, ahora.plusSeconds(1)));
    }

    /**
     * Cupos que de verdad se pueden reservar: futuros (con la anticipación mínima), de un
     * veterinario activo y sin ausencias, que no se crucen con otra cita del veterinario ni,
     * si se indica, con otra cita de la mascota.
     */
    @Transactional(readOnly = true)
    public List<Cita> cuposReservables(Integer idVeterinaria, Integer idServicio, Integer idVeterinario,
                                      LocalDate desde, LocalDate hasta, Integer idMascota) {
        return cuposReservables(idVeterinaria, idServicio, idVeterinario, desde, hasta, idMascota, null);
    }

    /**
     * Igual que {@link #cuposReservables(Integer, Integer, Integer, LocalDate, LocalDate, Integer)}, pero sin
     * contar como ocupada la cita {@code ignorarCita} (la que se está reprogramando: se libera al moverla).
     */
    public List<Cita> cuposReservables(Integer idVeterinaria, Integer idServicio, Integer idVeterinario,
                                      LocalDate desde, LocalDate hasta, Integer idMascota, Integer ignorarCita) {
        return buscarCupos(idVeterinaria, idServicio, idVeterinario, desde, hasta, idMascota, ignorarCita, Integer.MAX_VALUE);
    }

    /** El primer cupo reservable (en orden de fecha y hora) o vacío; se detiene al encontrarlo. */
    @Transactional(readOnly = true)
    public Optional<Cita> primerCupoReservable(Integer idVeterinaria, Integer idServicio, LocalDate desde, LocalDate hasta) {
        return buscarCupos(idVeterinaria, idServicio, null, desde, hasta, null, null, 1).stream().findFirst();
    }

    /**
     * Rendimiento: las ausencias aprobadas de cada veterinario se cargan UNA vez para todo el
     * rango (antes se consultaban por cada cupo candidato) y las citas que ocupan horario se
     * cargan una vez por veterinario y día. Las reglas son las mismas.
     */
    private List<Cita> buscarCupos(Integer idVeterinaria, Integer idServicio, Integer idVeterinario,
                                   LocalDate desde, LocalDate hasta, Integer idMascota, Integer ignorarCita, int maximo) {
        LocalDateTime limite = ahora().plusMinutes(reglas.anticipacionMinimaReservaMin());
        LocalDate inicio = desde.isBefore(limite.toLocalDate()) ? limite.toLocalDate() : desde;
        if (hasta.isBefore(inicio)) return List.of();
        List<Cita> candidatos = citas.cuposEntre(idVeterinaria, idServicio, idVeterinario, inicio, hasta);

        Map<String, List<Cita>> ocupadasVet = new HashMap<>();
        Map<LocalDate, List<Cita>> ocupadasMascota = new HashMap<>();
        Map<Integer, Boolean> autorizado = new HashMap<>();
        Map<Integer, List<AusenciaVeterinario>> ausenciasVet = new HashMap<>();
        LocalDateTime rangoIni = inicio.atStartOfDay();
        LocalDateTime rangoFin = hasta.plusDays(1).atStartOfDay();
        List<Cita> r = new ArrayList<>();
        for (Cita c : candidatos) {
            if (r.size() >= maximo) break;
            if (c.getInicio().isBefore(limite)) continue;
            Usuario vet = c.getVeterinario();
            if (!vet.isActivo()) continue;
            boolean aut = autorizado.computeIfAbsent(vet.getIdUsuario(),
                    id -> !tieneServiciosAutorizados(id) || autorizadoPara(id, idServicio));
            if (!aut) continue;
            List<AusenciaVeterinario> aus = ausenciasVet.computeIfAbsent(vet.getIdUsuario(),
                    id -> ausencias.enIntervalo(id, EstadoAprobacion.APROBADO, rangoIni, rangoFin));
            if (aus.stream().anyMatch(a -> a.cubre(c.getInicio(), c.getFin()))) continue;
            List<Cita> ov = ocupadasVet.computeIfAbsent(vet.getIdUsuario() + "|" + c.getFecha(),
                    k -> citas.deVeterinarioEnFechaConEstados(vet.getIdUsuario(), c.getFecha(), EstadoCita.OCUPAN_HORARIO));
            if (ov.stream().anyMatch(o -> !o.getIdCita().equals(ignorarCita) && o.seSolapaCon(c.getInicio(), c.getFin()))) continue;
            if (idMascota != null) {
                List<Cita> om = ocupadasMascota.computeIfAbsent(c.getFecha(),
                        f -> citas.deMascotaEnFechaConEstados(idMascota, f, EstadoCita.OCUPAN_HORARIO));
                if (om.stream().anyMatch(o -> !o.getIdCita().equals(ignorarCita) && o.seSolapaCon(c.getInicio(), c.getFin()))) continue;
            }
            r.add(c);
        }
        return r;
    }

    // ------------------------------------------------------------------
    // Generación automática de cupos
    // ------------------------------------------------------------------

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoGeneracion generarAgendaClinica(Integer idVeterinaria, LocalDate desde, LocalDate hasta) {
        int total = 0;
        int vets = 0;
        for (Veterinario v : veterinarios.findByVeterinaria_IdVeterinaria(idVeterinaria)) {
            if (v.getUsuario() == null || !v.getUsuario().isActivo()) continue;
            total += generarParaVeterinario(v.getIdUsuario(), desde, hasta);
            vets++;
        }
        return new ResultadoGeneracion(total, vets, desde, hasta);
    }

    /** Horizonte por defecto: hoy … hoy + horizonteAgendaDias. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultadoGeneracion generarAgendaClinica(Integer idVeterinaria) {
        LocalDate hoy = ahora().toLocalDate();
        return generarAgendaClinica(idVeterinaria, hoy, hoy.plusDays(reglas.horizonteAgendaDias()));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int generarParaVeterinario(Integer idVeterinario, LocalDate desde, LocalDate hasta) {
        Veterinario v = veterinarios.bloquear(idVeterinario)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
        Usuario vet = v.getUsuario();
        if (v.getVeterinaria() == null || !vet.isActivo()) return 0;
        Integer idClinica = v.getVeterinaria().getIdVeterinaria();

        Set<Integer> autorizados = serviciosAutorizados(idVeterinario);
        List<ServicioVeterinaria> ofrecidos = serviciosClinica.findByVeterinaria_IdVeterinaria(idClinica).stream()
                .filter(sv -> autorizados.contains(sv.getServicio().getIdServicio()))
                .filter(sv -> !sv.getServicio().isEmergencia())
                .sorted(Comparator.comparing(sv -> sv.getServicio().getIdServicio()))
                .toList();
        if (ofrecidos.isEmpty()) return 0;

        Map<LocalDate, List<Cita>> existentes = citas
                .deVeterinarioEntreConEstados(idVeterinario, desde, hasta, BLOQUEAN_GENERACION).stream()
                .collect(Collectors.groupingBy(Cita::getFecha, HashMap::new, Collectors.toCollection(ArrayList::new)));

        LocalDateTime limite = ahora().plusMinutes(reglas.anticipacionMinimaReservaMin());
        int creados = 0;
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            List<Cita> delDia = existentes.computeIfAbsent(fecha, f -> new ArrayList<>());
            int n = ofrecidos.size();
            // Rotación por día: cada servicio arranca primero en días distintos (reparto equilibrado).
            int idx = fecha.getDayOfYear() % n;
            for (Intervalo franja : franjasLaborales(vet, fecha)) {
                LocalDateTime t = franja.inicio();
                while (t.isBefore(franja.fin())) {
                    ServicioVeterinaria elegido = null;
                    int dur = 0;
                    for (int k = 0; k < n; k++) {
                        ServicioVeterinaria sv = ofrecidos.get((idx + k) % n);
                        int d = duracionValida(sv);
                        if (!t.plusMinutes(d).isAfter(franja.fin())) {
                            elegido = sv;
                            dur = d;
                            idx = (idx + k) % n;
                            break;
                        }
                    }
                    if (elegido == null) break;
                    LocalDateTime fin = t.plusMinutes(dur);
                    final LocalDateTime desdeT = t;
                    Optional<Cita> choque = delDia.stream().filter(c -> c.seSolapaCon(desdeT, fin))
                            .max(Comparator.comparing(Cita::getFin));
                    if (choque.isPresent()) {
                        t = choque.get().getFin();
                        continue;
                    }
                    if (!t.isBefore(limite)) {
                        Cita cupo = new Cita();
                        cupo.setFecha(fecha);
                        cupo.setHora(t.toLocalTime());
                        cupo.setDuracionMin(dur);
                        cupo.setEstado(EstadoCita.DISPONIBLE);
                        cupo.setVeterinario(vet);
                        cupo.setVeterinaria(v.getVeterinaria());
                        cupo.setServicio(elegido.getServicio());
                        cupo.setGeneradaAuto(true);
                        cupo.setCreadaEn(ahora());
                        delDia.add(citas.save(cupo));
                        creados++;
                    }
                    idx = (idx + 1) % n;
                    t = fin;
                }
            }
        }
        return creados;
    }

    /**
     * Tras aprobar un cambio de disponibilidad: retira los cupos DISPONIBLES futuros que ya no
     * caben en la disponibilidad o cuyo servicio dejó de estar autorizado. Los generados
     * automáticamente se eliminan (son derivables); los creados a mano quedan CANCELADOS con
     * una nota, para no perder su rastro. Nunca toca citas reservadas.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int retirarCuposIncompatibles(Integer idVeterinario) {
        Veterinario v = veterinarios.bloquear(idVeterinario).orElseThrow();
        LocalDateTime ahora = ahora();
        Set<Integer> autorizados = serviciosAutorizados(idVeterinario);
        boolean exigirHorario = tieneHorarioAprobado(idVeterinario);
        int retirados = 0;
        for (Cita c : citas.deVeterinarioEntreConEstados(idVeterinario, ahora.toLocalDate(),
                ahora.toLocalDate().plusYears(1), EnumSet.of(EstadoCita.DISPONIBLE))) {
            if (c.getInicio().isBefore(ahora)) continue;
            boolean servicioOk = autorizados.isEmpty() || autorizados.contains(c.getServicio().getIdServicio());
            if (servicioOk && disponibleEn(v.getUsuario(), c.getInicio(), c.getFin(), exigirHorario)) continue;
            if (Boolean.TRUE.equals(c.getGeneradaAuto())) {
                citas.delete(c);
            } else {
                c.setEstado(EstadoCita.CANCELADA);
                c.setObservacionesAdmin("Cupo retirado por cambio aprobado en la disponibilidad del veterinario.");
            }
            retirados++;
        }
        return retirados;
    }

    /**
     * Citas reservadas que quedan fuera de la disponibilidad aprobada del veterinario, solo en
     * su clínica actual (un veterinario que cambió de clínica puede conservar citas en otra:
     * esas las gestiona la clínica dueña de la cita, ver {@link #citasAfectadasDeClinica}).
     */
    @Transactional(readOnly = true)
    public List<Cita> citasAfectadas(Integer idVeterinario) {
        Veterinario v = veterinarios.findById(idVeterinario).orElseThrow();
        Integer clinica = v.getVeterinaria() == null ? null : v.getVeterinaria().getIdVeterinaria();
        LocalDateTime ahora = ahora();
        Set<Integer> autorizados = serviciosAutorizados(idVeterinario);
        boolean exigirHorario = tieneHorarioAprobado(idVeterinario);
        return citas.deVeterinarioEntreConEstados(idVeterinario, ahora.toLocalDate(), ahora.toLocalDate().plusYears(1),
                        EstadoCita.RESERVAS_VIGENTES).stream()
                .filter(c -> clinica != null && c.getVeterinaria().getIdVeterinaria().equals(clinica))
                .filter(c -> c.getInicio().isAfter(ahora))
                .filter(c -> !(autorizados.isEmpty() || autorizados.contains(c.getServicio().getIdServicio()))
                        || !disponibleEn(v.getUsuario(), c.getInicio(), c.getFin(), exigirHorario))
                .sorted(Comparator.comparing(Cita::getInicio))
                .toList();
    }

    /**
     * Citas futuras (pendientes o confirmadas) de la clínica que necesitan reprogramarse: su
     * veterinario ya no pertenece a la clínica o está inactivo, ya no está autorizado para el
     * servicio, o el horario quedó fuera de su disponibilidad (horario, ausencia o bloqueo).
     * Solo devuelve citas de esa clínica: la recepción nunca ve citas de otra veterinaria.
     */
    @Transactional(readOnly = true)
    public List<Cita> citasAfectadasDeClinica(Integer idClinica) {
        LocalDateTime ahora = ahora();
        Map<Integer, Set<Integer>> autorizadosPorVet = new HashMap<>();
        Map<Integer, Boolean> horarioPorVet = new HashMap<>();
        return citas.findByVeterinaria_IdVeterinariaAndEstadoInAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(
                        idClinica, EstadoCita.RESERVAS_VIGENTES, ahora.toLocalDate()).stream()
                .filter(c -> c.getMascota() != null && c.getInicio().isAfter(ahora))
                .filter(c -> {
                    Usuario vet = c.getVeterinario();
                    if (vet.getVeterinario() == null || vet.getVeterinario().getVeterinaria() == null
                            || !vet.getVeterinario().getVeterinaria().getIdVeterinaria().equals(idClinica)
                            || !vet.isActivo()) {
                        return true;
                    }
                    Set<Integer> aut = autorizadosPorVet.computeIfAbsent(vet.getIdUsuario(), this::serviciosAutorizados);
                    if (!aut.isEmpty() && !aut.contains(c.getServicio().getIdServicio())) return true;
                    boolean exigir = horarioPorVet.computeIfAbsent(vet.getIdUsuario(), this::tieneHorarioAprobado);
                    return !disponibleEn(vet, c.getInicio(), c.getFin(), exigir);
                })
                .toList();
    }

    /** Hasta 5 cupos alternativos con otros veterinarios compatibles (mismo servicio, mismo día o la semana siguiente). */
    @Transactional(readOnly = true)
    public List<Cita> alternativas(Cita afectada) {
        Integer idVet = afectada.getVeterinario().getIdUsuario();
        return cuposReservables(afectada.getVeterinaria().getIdVeterinaria(), afectada.getServicio().getIdServicio(),
                null, afectada.getFecha(), afectada.getFecha().plusDays(7),
                afectada.getMascota() == null ? null : afectada.getMascota().getIdMascota())
                .stream()
                .filter(c -> !c.getVeterinario().getIdUsuario().equals(idVet))
                .limit(5)
                .toList();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private int duracionValida(ServicioVeterinaria sv) {
        Integer d = sv.getDuracionAprox();
        return d == null || d <= 0 ? reglas.duracionServicioPorDefectoMin() : d;
    }

    private List<Intervalo> recortarAHorarioClinica(List<Intervalo> base, Integer idClinica, LocalDate fecha) {
        List<HorarioVeterinaria> todos = horariosClinica.findByVeterinaria_IdVeterinaria(idClinica);
        if (todos.isEmpty()) return base; // la clínica no definió horario: rige el del veterinario
        DayOfWeek dia = fecha.getDayOfWeek();
        List<Intervalo> clinica = todos.stream()
                .filter(h -> h.getEstado() != null && h.getEstado() == 1)
                .filter(h -> h.getHoraApertura().isBefore(h.getHoraCierre()))
                .filter(h -> esDia(h, dia))
                .map(h -> new Intervalo(fecha.atTime(h.getHoraApertura()), fecha.atTime(h.getHoraCierre())))
                .toList();
        List<Intervalo> r = new ArrayList<>();
        for (Intervalo b : base) {
            for (Intervalo c : clinica) {
                LocalDateTime i = b.inicio().isAfter(c.inicio()) ? b.inicio() : c.inicio();
                LocalDateTime f = b.fin().isBefore(c.fin()) ? b.fin() : c.fin();
                if (i.isBefore(f)) r.add(new Intervalo(i, f));
            }
        }
        r.sort(Comparator.comparing(Intervalo::inicio));
        return r;
    }

    private static boolean esDia(HorarioVeterinaria h, DayOfWeek dia) {
        if (h.getDiaSemana() == null) return false;
        String nombre = Normalizer.normalize(String.valueOf(h.getDiaSemana().getDiaSemana()), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
        String esperado = switch (dia) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sabado";
            case SUNDAY -> "domingo";
        };
        return nombre.equals(esperado);
    }

    static List<Intervalo> restar(List<Intervalo> franjas, Intervalo hueco) {
        List<Intervalo> r = new ArrayList<>();
        for (Intervalo f : franjas) {
            if (!f.seCruza(hueco.inicio(), hueco.fin())) {
                r.add(f);
                continue;
            }
            if (f.inicio().isBefore(hueco.inicio())) r.add(new Intervalo(f.inicio(), hueco.inicio()));
            if (hueco.fin().isBefore(f.fin())) r.add(new Intervalo(hueco.fin(), f.fin()));
        }
        return r;
    }

    /** Para vistas: franjas aprobadas agrupadas por día. */
    public Map<Integer, List<HorarioVeterinario>> horarioAprobadoPorDia(Integer idVeterinario) {
        return horarios.findByVeterinario_IdUsuarioAndEstadoOrderByDiaSemanaAscHoraInicioAsc(idVeterinario,
                        EstadoAprobacion.APROBADO).stream()
                .collect(Collectors.groupingBy(HorarioVeterinario::getDiaSemana, TreeMap::new, Collectors.toList()));
    }

    public static boolean horaValida(LocalTime inicio, LocalTime fin) {
        return inicio != null && fin != null && inicio.isBefore(fin);
    }
}
