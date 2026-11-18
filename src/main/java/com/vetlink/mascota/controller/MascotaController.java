package com.vetlink.mascota.controller;

import com.vetlink.cita.service.CitaService;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.mascota.repository.RazaRepository;
import com.vetlink.mascota.service.FotoMascotaService;
import com.vetlink.security.AccesoPacienteService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.vacuna.service.CarnetVacunaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

/** CRUD completo de mascotas para el rol TUTOR. */
@Controller
@RequestMapping("/mascotas")
public class MascotaController {

    private final MascotaService mascotaService;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final CitaService citaService;
    private final HistorialClinicoService historialClinicoService;
    private final CarnetVacunaService carnetVacunaService;
    private final FotoMascotaService fotoMascotaService;
    private final AccesoPacienteService accesoPacienteService;

    public MascotaController(MascotaService mascotaService, EspecieRepository especieRepository, RazaRepository razaRepository,
                              CitaService citaService, HistorialClinicoService historialClinicoService,
                              CarnetVacunaService carnetVacunaService, FotoMascotaService fotoMascotaService,
                              AccesoPacienteService accesoPacienteService) {
        this.mascotaService = mascotaService;
        this.especieRepository = especieRepository;
        this.razaRepository = razaRepository;
        this.citaService = citaService;
        this.historialClinicoService = historialClinicoService;
        this.carnetVacunaService = carnetVacunaService;
        this.fotoMascotaService = fotoMascotaService;
        this.accesoPacienteService = accesoPacienteService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        model.addAttribute("mascotas", mascotaService.listarActivasPorTutor(principal.getUsuario().getIdUsuario()));
        return "mascotas/lista";
    }

    @GetMapping("/nueva")
    public String nuevaForm(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        cargarCatalogos(model);
        model.addAttribute("form", new MascotaForm());
        return "mascotas/formulario";
    }

    @PostMapping("/nueva")
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                         @Valid @ModelAttribute("form") MascotaForm form, BindingResult result,
                         @RequestParam(value = "archivoFoto", required = false) MultipartFile archivoFoto, Model model) {
        validarFoto(archivoFoto, result);
        if (result.hasErrors()) {
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        try {
            var mascota = mascotaService.crear(form, principal.getUsuario().getIdUsuario());
            fotoMascotaService.guardar(mascota, archivoFoto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorGeneral", e.getMessage());
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        return "redirect:/mascotas?creada";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var mascota = mascotaService.buscarPorId(id);
        verificarPropietario(mascota, principal);
        MascotaForm form = new MascotaForm();
        form.setIdMascota(mascota.getIdMascota());
        form.setNombre(mascota.getNombre());
        form.setIdEspecie(mascota.getEspecie().getIdEspecie());
        form.setIdRaza(mascota.getRaza().getIdRaza());
        form.setGenero(mascota.getGenero());
        form.setFechaNacimiento(mascota.getFechaNacimiento());
        form.setPeso(mascota.getPeso());
        form.setFoto(mascota.getFotoUrl()); // solo para la vista previa
        cargarShell(model, principal);
        cargarCatalogos(model);
        model.addAttribute("form", form);
        return "mascotas/formulario";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                              @Valid @ModelAttribute("form") MascotaForm form, BindingResult result,
                              @RequestParam(value = "archivoFoto", required = false) MultipartFile archivoFoto, Model model) {
        form.setIdMascota(id);
        validarFoto(archivoFoto, result);
        if (result.hasErrors()) {
            form.setFoto(mascotaService.buscarPorId(id).getFotoUrl());
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        try {
            var mascota = mascotaService.actualizar(form, principal.getUsuario().getIdUsuario());
            fotoMascotaService.guardar(mascota, archivoFoto);
        } catch (RuntimeException e) {
            model.addAttribute("errorGeneral", e.getMessage());
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "mascotas/formulario";
        }
        return "redirect:/mascotas?actualizada";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        mascotaService.eliminar(id, principal.getUsuario().getIdUsuario());
        return "redirect:/mascotas?eliminada";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(id);
        verificarPropietario(mascota, principal);
        var citas = citaService.listarPorMascota(id);
        model.addAttribute("mascota", mascota);
        model.addAttribute("citas", citas);
        model.addAttribute("proximasCitas", citas.stream()
                .filter(c -> c.getEstado() == com.vetlink.cita.model.EstadoCita.AGENDADA)
                .toList());
        model.addAttribute("historial", historialClinicoService.listarPorMascota(id));
        model.addAttribute("carnet", carnetVacunaService.listarPorMascota(id));
        return "mascotas/detalle";
    }

    /**
     * VL-22: devuelve la foto subida solo a quien tiene acceso a la mascota
     * (su dueño, o el personal que la atiende). Las fotos antiguas que son URL redirigen a esa URL.
     */
    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        var mascota = mascotaService.buscarPorId(id);
        accesoPacienteService.verificar(mascota, principal);
        if (FotoMascotaService.esUrl(mascota.getFoto())) {
            return ResponseEntity.status(302).location(URI.create(mascota.getFoto())).build();
        }
        return fotoMascotaService.archivoLocal(mascota)
                .map(path -> ResponseEntity.ok()
                        .contentType(FotoMascotaService.tipoDe(path))
                        .header("Cache-Control", "private, max-age=3600")
                        .body((Resource) new FileSystemResource(path)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void validarFoto(MultipartFile archivoFoto, BindingResult result) {
        try {
            fotoMascotaService.validar(archivoFoto);
        } catch (IllegalArgumentException e) {
            result.rejectValue("foto", "fotoInvalida", e.getMessage());
        }
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("especies", especieRepository.findAll());
        model.addAttribute("razas", razaRepository.findAll());
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "mascotas");
        model.addAttribute("navItems", DashboardController.navTutor());
    }

    private void verificarPropietario(com.vetlink.mascota.model.Mascota mascota,
                                      VetlinkUserPrincipal principal) {
        if (!mascota.getTutor().getIdUsuario().equals(principal.getUsuario().getIdUsuario())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso sobre esta mascota");
        }
    }
}
