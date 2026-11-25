package com.vetlink.veterinaria.controller;

import com.vetlink.resena.service.ResenaService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.ubicacion.repository.CiudadRepository;
import com.vetlink.ubicacion.repository.LocalidadRepository;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.service.DirectorioService;
import com.vetlink.veterinaria.service.LogoVeterinariaService;
import com.vetlink.veterinaria.service.VeterinariaInfoService;
import com.vetlink.veterinaria.service.VeterinariaService;
import com.vetlink.emergencia.service.EmergenciaDisponibilidadService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping({"/directorio", "/veterinarias"})
public class VeterinariaController {

    private final VeterinariaService veterinariaService;
    private final ResenaService resenaService;
    private final ServicioVeterinariaRepository servicioVeterinariaRepository;
    private final ServicioRepository servicioRepository;
    private final CiudadRepository ciudades;
    private final LocalidadRepository localidades;
    private final DirectorioService directorio;
    private final VeterinariaInfoService info;
    private final LogoVeterinariaService logos;
    private final EmergenciaDisponibilidadService emergencias;

    public VeterinariaController(VeterinariaService veterinariaService, ResenaService resenaService,
                                 ServicioVeterinariaRepository servicioVeterinariaRepository,
                                 ServicioRepository servicioRepository, CiudadRepository ciudades,
                                 LocalidadRepository localidades, DirectorioService directorio,
                                 VeterinariaInfoService info, LogoVeterinariaService logos,
                                 EmergenciaDisponibilidadService emergencias) {
        this.veterinariaService = veterinariaService;
        this.resenaService = resenaService;
        this.servicioVeterinariaRepository = servicioVeterinariaRepository;
        this.servicioRepository = servicioRepository;
        this.ciudades = ciudades;
        this.localidades = localidades;
        this.directorio = directorio;
        this.info = info;
        this.logos = logos;
        this.emergencias = emergencias;
    }

    /**
     * Sin parámetros de búsqueda muestra primero el formulario de personalización; con
     * {@code buscar=1} (o una consulta q) muestra solo las veterinarias que cumplen los filtros.
     */
    @GetMapping
    public String listar(@RequestParam(required = false) String buscar,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) Double lat,
                         @RequestParam(required = false) Double lng,
                         @RequestParam(required = false) Integer idCiudad,
                         @RequestParam(required = false) Integer idLocalidad,
                         @RequestParam(required = false) Integer idServicio,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
                         @RequestParam(required = false, defaultValue = "false") boolean emergencia,
                         Model model) {
        model.addAttribute("todosLosServicios", servicioRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getNombreServicio().toLowerCase())).toList());
        model.addAttribute("ciudades", ciudades.findAll());
        model.addAttribute("localidades", localidades.findAll().stream()
                .map(l -> Map.of("id", l.getIdLocalidad(), "nombre", l.getNombre(),
                        "ciudad", l.getCiudad() == null ? 0 : l.getCiudad().getIdCiudad()))
                .toList());
        boolean hayBusqueda = buscar != null || (q != null && !q.isBlank());
        model.addAttribute("buscando", hayBusqueda);
        model.addAttribute("q", q);
        model.addAttribute("f", new DirectorioService.Filtros(lat, lng, idCiudad, idLocalidad, idServicio, fecha, hora,
                emergencia, q));
        model.addAttribute("hoy", LocalDate.now());
        if (hayBusqueda) {
            if (fecha != null && fecha.isBefore(LocalDate.now())) {
                model.addAttribute("errorFiltros", "No puedes seleccionar una fecha anterior a la actual");
                model.addAttribute("veterinarias", List.of());
            } else {
                model.addAttribute("veterinarias", directorio.buscar(new DirectorioService.Filtros(lat, lng, idCiudad,
                        idLocalidad, idServicio, fecha, hora, emergencia, q)));
            }
        }
        return "veterinarias/lista";
    }

    @GetMapping("/{id:\\d+}")
    public String detalle(@PathVariable Integer id, @RequestParam(required = false) String volver, Model model) {
        Veterinaria veterinaria = veterinariaService.buscarPorId(id);
        if (!veterinaria.isActiva()) {
            throw new IllegalArgumentException("Veterinaria no encontrada");
        }
        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("logoUrl", VeterinariaInfoService.logoUrl(veterinaria));
        model.addAttribute("direccion", info.direccion(id));
        model.addAttribute("telefonos", info.telefonos(id));
        model.addAttribute("correos", info.correos(id));
        model.addAttribute("horario", info.horarioSemanal(id));
        model.addAttribute("urgencias", emergencias.ofreceEmergencias(id));
        model.addAttribute("vetsDisponibles", emergencias.ofreceEmergencias(id) ? emergencias.disponiblesAhora(id).size() : 0);
        model.addAttribute("resenas", resenaService.listarPorVeterinaria(id));
        model.addAttribute("promedio", resenaService.promedioDeVeterinaria(id));
        model.addAttribute("servicios", servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(id));
        // Solo se acepta volver a una búsqueda del propio directorio (evita redirecciones abiertas).
        model.addAttribute("volver", volver != null && volver.startsWith("/directorio") && !volver.startsWith("//")
                ? volver : "/directorio");
        return "veterinarias/detalle";
    }

    /** Imagen pública de la veterinaria (o la imagen por defecto). */
    @GetMapping("/{id:\\d+}/logo")
    public ResponseEntity<Resource> logo(@PathVariable Integer id) {
        Veterinaria v = veterinariaService.buscarPorId(id);
        return logos.archivo(v)
                .map(p -> ResponseEntity.ok().contentType(LogoVeterinariaService.tipo(p))
                        .header("Cache-Control", "public, max-age=3600")
                        .header("X-Content-Type-Options", "nosniff")
                        .body((Resource) new FileSystemResource(p)))
                .orElseGet(() -> ResponseEntity.status(302)
                        .location(java.net.URI.create("/resource/veterinaria-default.svg")).build());
    }

    /** Veterinarios: su clínica (se conserva por compatibilidad; el menú ahora lleva a «Mi disponibilidad»). */
    @GetMapping("/mi-clinica")
    public String miClinica(@AuthenticationPrincipal VetlinkUserPrincipal principal) {
        if (principal.getUsuario().getVeterinario() == null) {
            throw new AccessDeniedException("No tienes una veterinaria asociada");
        }
        return "redirect:/disponibilidad";
    }
}
