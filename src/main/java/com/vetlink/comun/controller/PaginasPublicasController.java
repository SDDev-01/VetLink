package com.vetlink.comun.controller;

import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.resena.repository.ResenaRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
public class PaginasPublicasController {

    private final VeterinariaRepository veterinariaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final MascotaRepository mascotaRepository;
    private final ResenaRepository resenaRepository;

    public PaginasPublicasController(VeterinariaRepository veterinariaRepository, VeterinarioRepository veterinarioRepository,
                                     MascotaRepository mascotaRepository, ResenaRepository resenaRepository) {
        this.veterinariaRepository = veterinariaRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.mascotaRepository = mascotaRepository;
        this.resenaRepository = resenaRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
        // VL-28: cifras reales de la base en vez de números inventados.
        model.addAttribute("statClinicas", veterinariaRepository.countByEstado((byte) 1));
        model.addAttribute("statVeterinarios", veterinarioRepository.count());
        model.addAttribute("statMascotas", mascotaRepository.countByEstado((byte) 1));
        Double promedio = resenaRepository.promedioGeneral();
        model.addAttribute("statPromedio", promedio == null ? null
                : BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP).doubleValue());
        return "index";
    }

    @GetMapping("/403")
    public String accesoDenegado() {
        return "error/403";
    }
}
