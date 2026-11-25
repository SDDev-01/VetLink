package com.vetlink.comun.controller;

import com.vetlink.mascota.model.Raza;
import com.vetlink.mascota.repository.RazaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pequeños endpoints JSON de apoyo para selects dependientes en formularios publicos (ej. registro). */
@RestController
@RequestMapping("/api/catalogos")
public class ApiPublicoController {

    private final RazaRepository razaRepository;
    private static final java.text.Collator ORDEN = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("es-CO"));

    public ApiPublicoController(RazaRepository razaRepository) {
        this.razaRepository = razaRepository;
    }

    @GetMapping("/razas/{idEspecie}")
    public List<RazaOption> razasPorEspecie(@PathVariable Integer idEspecie) {
        return razaRepository.findByEspecie_IdEspecie(idEspecie).stream()
                .map(r -> new RazaOption(r.getIdRaza(), r.getNombreRaza()))
                .sorted(java.util.Comparator.comparing(RazaOption::nombre, ORDEN))
                .toList();
    }

    public record RazaOption(Integer id, String nombre) {}
}
