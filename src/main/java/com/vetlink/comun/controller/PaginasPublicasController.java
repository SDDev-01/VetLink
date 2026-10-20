package com.vetlink.comun.controller;

import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginasPublicasController {

    private final VeterinariaRepository veterinariaRepository;

    public PaginasPublicasController(VeterinariaRepository veterinariaRepository) {
        this.veterinariaRepository = veterinariaRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("veterinarias", veterinariaRepository.findByEstado((byte) 1));
        return "index";
    }

    @GetMapping("/403")
    public String accesoDenegado() {
        return "error/403";
    }
}
