package com.vetlink.veterinaria.service;

import com.vetlink.cita.model.Cita;
import com.vetlink.comun.web.FormatoVista;
import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.emergencia.service.EmergenciaDisponibilidadService;
import com.vetlink.resena.service.ResenaService;
import com.vetlink.ubicacion.model.UbicacionVeterinaria;
import com.vetlink.veterinaria.model.HorarioVeterinaria;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Búsqueda del directorio: solo devuelve veterinarias que cumplen todos los filtros.
 * En modo emergencia solo aparecen clínicas con servicio de urgencias y al menos un
 * veterinario disponible ahora mismo (ver EmergenciaDisponibilidadService), ordenadas por cercanía.
 */
@Service
@Transactional(readOnly = true)
public class DirectorioService {

    public record Filtros(Double lat, Double lng, Integer idCiudad, Integer idLocalidad, Integer idServicio,
                          LocalDate fecha, LocalTime hora, boolean emergencia, String q) {
        public boolean tieneUbicacion() {
            return lat != null && lng != null && Double.isFinite(lat) && Double.isFinite(lng)
                    && Math.abs(lat) <= 90 && Math.abs(lng) <= 180;
        }
    }

    public record Tarjeta(Integer id, String nombre, String logoUrl, String direccion, Double distanciaKm,
                          List<String> servicios, String horarioHoy, String disponibilidad, boolean disponible,
                          List<String> telefonos, List<String> correos, String paginaWeb, String rating,
                          Double lat, Double lng, boolean urgencias, int veterinariosDisponibles) {}

    private final VeterinariaRepository veterinarias;
    private final ServicioVeterinariaRepository serviciosClinica;
    private final VeterinariaInfoService info;
    private final ResenaService resenas;
    private final DisponibilidadService disponibilidad;
    private final EmergenciaDisponibilidadService emergencias;

    public DirectorioService(VeterinariaRepository veterinarias, ServicioVeterinariaRepository serviciosClinica,
                             VeterinariaInfoService info, ResenaService resenas,
                             DisponibilidadService disponibilidad, EmergenciaDisponibilidadService emergencias) {
        this.veterinarias = veterinarias;
        this.serviciosClinica = serviciosClinica;
        this.info = info;
        this.resenas = resenas;
        this.disponibilidad = disponibilidad;
        this.emergencias = emergencias;
    }

    public List<Tarjeta> buscar(Filtros f) {
        LocalDateTime ahora = disponibilidad.ahora();
        FormatoVista fmt = new FormatoVista();
        List<Veterinaria> base = (f.q() == null || f.q().isBlank())
                ? veterinarias.findByEstado((byte) 1)
                : veterinarias.findByNombreContainingIgnoreCaseAndEstado(f.q().trim(), (byte) 1);
        List<Tarjeta> r = new ArrayList<>();
        for (Veterinaria v : base) {
            Integer id = v.getIdVeterinaria();
            var svs = serviciosClinica.findByVeterinaria_IdVeterinaria(id);
            if (f.idServicio() != null && svs.stream().noneMatch(sv -> sv.getServicio().getIdServicio().equals(f.idServicio()))) {
                continue;
            }
            Optional<UbicacionVeterinaria> ub = info.ubicacion(id);
            if (f.idLocalidad() != null && ub.map(u -> u.getBarrio() == null || u.getBarrio().getLocalidad() == null
                    || !u.getBarrio().getLocalidad().getIdLocalidad().equals(f.idLocalidad())).orElse(true)) {
                continue;
            }
            if (f.idCiudad() != null && ub.map(u -> u.getBarrio() == null || u.getBarrio().getLocalidad() == null
                    || u.getBarrio().getLocalidad().getCiudad() == null
                    || !u.getBarrio().getLocalidad().getCiudad().getIdCiudad().equals(f.idCiudad())).orElse(true)) {
                continue;
            }
            boolean urgencias = emergencias.ofreceEmergencias(id);
            int vetsDisponibles = urgencias ? emergencias.disponiblesAhora(id).size() : 0;
            if (f.emergencia() && (!urgencias || vetsDisponibles == 0)) continue;

            String dispTexto;
            boolean disponible;
            if (f.emergencia()) {
                dispTexto = vetsDisponibles + (vetsDisponibles == 1 ? " veterinario disponible ahora" : " veterinarios disponibles ahora");
                disponible = true;
            } else {
                Optional<Cita> cupo = proximoCupo(id, f, ahora);
                if ((f.fecha() != null) && cupo.isEmpty()) continue;
                disponible = cupo.isPresent();
                dispTexto = cupo.map(c -> "Próximo cupo: " + fmt.fechaCorta(c.getFecha()) + ", " + fmt.hora(c.getHora()))
                        .orElse("Sin cupos publicados por ahora");
            }
            Double lat = ub.map(UbicacionVeterinaria::getLatitud).orElse(null);
            Double lng = ub.map(UbicacionVeterinaria::getLongitud).orElse(null);
            Double dist = f.tieneUbicacion() && lat != null && lng != null ? distanciaKm(f.lat(), f.lng(), lat, lng) : null;
            Double promedio = resenas.promedioDeVeterinaria(id);
            List<HorarioVeterinaria> hoy = info.horarioDelDia(id, ahora.getDayOfWeek());
            String horarioHoy = hoy.isEmpty() ? "Hoy: cerrado o sin horario publicado"
                    : "Hoy: " + hoy.stream().map(h -> fmt.hora(h.getHoraApertura()) + " – " + fmt.hora(h.getHoraCierre()))
                    .collect(Collectors.joining(" · "));
            r.add(new Tarjeta(id, v.getNombre(), VeterinariaInfoService.logoUrl(v), info.direccion(id), dist,
                    svs.stream().map(sv -> sv.getServicio().getNombreServicio()).sorted().toList(), horarioHoy, dispTexto,
                    disponible, info.telefonos(id), info.correos(id), v.getPaginaWeb(),
                    promedio != null ? String.format(Locale.ROOT, "%.1f", promedio) : "Nuevo", lat, lng, urgencias, vetsDisponibles));
        }
        Comparator<Tarjeta> orden = Comparator.comparing(Tarjeta::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder()));
        if (!f.emergencia()) orden = orden.thenComparing(t -> !t.disponible());
        r.sort(orden.thenComparing(Tarjeta::nombre, String.CASE_INSENSITIVE_ORDER));
        return r;
    }

    /** Cupo reservable que cumple servicio/fecha/hora (±60 min) si se indicaron. */
    private Optional<Cita> proximoCupo(Integer idVeterinaria, Filtros f, LocalDateTime ahora) {
        LocalDate desde = f.fecha() != null ? f.fecha() : ahora.toLocalDate();
        LocalDate hasta = f.fecha() != null ? f.fecha() : ahora.toLocalDate().plusDays(30);
        List<Integer> servicios = f.idServicio() != null ? List.of(f.idServicio())
                : serviciosClinica.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .filter(sv -> !sv.getServicio().isEmergencia())
                .map(sv -> sv.getServicio().getIdServicio()).toList();
        Cita mejor = null;
        for (Integer s : servicios) {
            for (Cita c : disponibilidad.cuposReservables(idVeterinaria, s, null, desde, hasta, null)) {
                if (f.hora() != null && f.fecha() != null) {
                    long dif = Math.abs(java.time.Duration.between(f.hora(), c.getHora()).toMinutes());
                    if (dif > 60) continue;
                }
                if (mejor == null || c.getInicio().isBefore(mejor.getInicio())) mejor = c;
                break;
            }
        }
        return Optional.ofNullable(mejor);
    }

    /** Distancia en km entre dos coordenadas (fórmula del haversine). */
    public static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return Math.round(r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10.0) / 10.0;
    }
}
