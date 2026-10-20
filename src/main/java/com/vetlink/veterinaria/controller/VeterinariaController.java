package com.vetlink.veterinaria.controller;

import com.vetlink.resena.service.ResenaService;
import com.vetlink.ubicacion.repository.UbicacionVeterinariaRepository;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import com.vetlink.veterinaria.repository.ServicioRepository;
import com.vetlink.veterinaria.service.VeterinariaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;

import java.util.List;

@Controller
@RequestMapping({"/directorio", "/veterinarias"})
public class VeterinariaController {

    private final VeterinariaService veterinariaService;
    private final ResenaService resenaService;
    private final UbicacionVeterinariaRepository ubicacionVeterinariaRepository;
    private final ServicioVeterinariaRepository servicioVeterinariaRepository;
    private final ServicioRepository servicioRepository;

    public VeterinariaController(VeterinariaService veterinariaService, ResenaService resenaService,
                                  UbicacionVeterinariaRepository ubicacionVeterinariaRepository,
                                  ServicioVeterinariaRepository servicioVeterinariaRepository,
                                  ServicioRepository servicioRepository) {
        this.veterinariaService = veterinariaService;
        this.resenaService = resenaService;
        this.ubicacionVeterinariaRepository = ubicacionVeterinariaRepository;
        this.servicioVeterinariaRepository = servicioVeterinariaRepository;
        this.servicioRepository = servicioRepository;
    }

    /** Tarjeta de directorio con lo que necesita el mapa/las tarjetas: ubicacion, servicios, calificacion. */
    public record VeterinariaCard(Integer id, String nombre, Double lat, Double lng, String ciudad,
                                   List<String> servicios, String rating) {}

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        List<Veterinaria> veterinarias = (q == null || q.isBlank())
                ? veterinariaService.listarActivas()
                : veterinariaService.buscarPorNombre(q);

        List<VeterinariaCard> tarjetas = veterinarias.stream()
                .map(v -> {
                    var ubicacionOpt = ubicacionVeterinariaRepository.findByVeterinaria_IdVeterinaria(v.getIdVeterinaria());
                    Double lat = ubicacionOpt.map(u -> u.getLatitud()).orElse(null);
                    Double lng = ubicacionOpt.map(u -> u.getLongitud()).orElse(null);
                    String ciudad = ubicacionOpt
                            .map(u -> u.getBarrio())
                            .map(b -> b.getLocalidad())
                            .map(l -> l.getCiudad())
                            .map(c -> c.getNombre())
                            .orElse("");
                    List<String> servicios = servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(v.getIdVeterinaria())
                            .stream().map(sv -> sv.getServicio().getNombreServicio()).toList();
                    Double promedio = resenaService.promedioDeVeterinaria(v.getIdVeterinaria());
                    String rating = promedio != null ? String.format("%.1f", promedio) : "Nuevo";
                    return new VeterinariaCard(v.getIdVeterinaria(), v.getNombre(), lat, lng, ciudad, servicios, rating);
                })
                .toList();

        model.addAttribute("veterinarias", tarjetas);
        model.addAttribute("todosLosServicios", servicioRepository.findAll());
        model.addAttribute("q", q);
        return "veterinarias/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, Model model) {
        model.addAttribute("veterinaria", veterinariaService.buscarPorId(id));
        model.addAttribute("resenas", resenaService.listarPorVeterinaria(id));
        model.addAttribute("promedio", resenaService.promedioDeVeterinaria(id));
        return cargarMensajesDetalle(id, model);
        
    }

    /** Para veterinarios: se evita el directorio general y se muestra únicamente su clínica. */
    @GetMapping("/mi-clinica")
    public String miClinica(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        if (principal.getUsuario().getVeterinario() == null) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes una veterinaria asociada");
        }
        Veterinaria veterinaria = principal.getUsuario().getVeterinario().getVeterinaria();
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "directorio");
        model.addAttribute("navItems", DashboardController.navVeterinario());
        model.addAttribute("veterinaria", veterinaria);
        model.addAttribute("servicios", servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(veterinaria.getIdVeterinaria()));
        return "veterinarias/mi-clinica";
    }

    private String cargarMensajesDetalle(Integer id, Model model) {
        model.addAttribute("servicios", servicioVeterinariaRepository.findByVeterinaria_IdVeterinaria(id));
        return "veterinarias/detalle";
    }
}
