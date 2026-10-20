package com.vetlink.historial.controller;

import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.vacuna.service.CarnetVacunaService;
import com.vetlink.vacuna.repository.LoteVacunaRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

import java.util.LinkedHashMap;

/** Registro de historial clinico por el VETERINARIO tras una consulta. */
@Controller
@RequestMapping("/historial")
public class HistorialClinicoController {

    private final HistorialClinicoService historialService;
    private final MascotaService mascotaService;
    private final CitaRepository citaRepository;
    private final CarnetVacunaService carnetVacunaService;
    private final LoteVacunaRepository loteVacunaRepository;
    private final com.vetlink.vacuna.repository.VacunaRepository vacunaRepository;
    private final com.vetlink.historial.repository.ArchivoHistorialRepository archivoRepository;

    public HistorialClinicoController(HistorialClinicoService historialService, MascotaService mascotaService,
                                       CitaRepository citaRepository, CarnetVacunaService carnetVacunaService,
                                       LoteVacunaRepository loteVacunaRepository,
                                       com.vetlink.vacuna.repository.VacunaRepository vacunaRepository,
                                       com.vetlink.historial.repository.ArchivoHistorialRepository archivoRepository) {
        this.historialService = historialService;
        this.mascotaService = mascotaService;
        this.citaRepository = citaRepository;
        this.carnetVacunaService = carnetVacunaService;
        this.loteVacunaRepository = loteVacunaRepository;
        this.vacunaRepository = vacunaRepository;
        this.archivoRepository = archivoRepository;
    }

    /** Pacientes: para VETERINARIO son las mascotas que ha atendido; para RECEPCIONISTA,
     *  todas las mascotas que han tenido una cita en su veterinaria. */
    @GetMapping("/pacientes")
    public String misPacientes(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var pacientes = new LinkedHashMap<Integer, com.vetlink.mascota.model.Mascota>();
        if (principal.getUsuario().getRol() == com.vetlink.usuario.model.Rol.RECEPCIONISTA) {
            Integer idVeterinaria = principal.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria();
            citaRepository.buscarConFiltros(idVeterinaria, null, null, null, null)
                    .forEach(c -> {
                        if (c.getMascota() != null) {
                            pacientes.putIfAbsent(c.getMascota().getIdMascota(), c.getMascota());
                        }
                    });
        } else {
            citaRepository.findByVeterinario_IdUsuarioOrderByFechaDesc(principal.getUsuario().getIdUsuario())
                    .forEach(c -> {
                        if (c.getMascota() != null) {
                            pacientes.putIfAbsent(c.getMascota().getIdMascota(), c.getMascota());
                        }
                    });
        }
        model.addAttribute("pacientes", pacientes.values());
        return "historial/pacientes";
    }

    @GetMapping("/mias")
    public String misHistoriales(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        model.addAttribute("historial", historialService.listarPorTutor(principal.getUsuario().getIdUsuario()));
        return "historial/mias";
    }

    @GetMapping("/mascota/{idMascota}")
    public String listar(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(idMascota);
        verificarAccesoPaciente(mascota, principal);
        model.addAttribute("mascota", mascota);
        model.addAttribute("historial", historialService.listarPorMascota(idMascota));
        return "historial/lista";
    }

    @GetMapping("/nuevo/{idMascota}")
    public String nuevoForm(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(idMascota);
        verificarAccesoPaciente(mascota, principal);
        model.addAttribute("mascota", mascota);
        HistorialClinicoForm form = new HistorialClinicoForm();
        form.setIdMascota(idMascota);
        model.addAttribute("form", form);
        return "historial/formulario";
    }

    /** RF0013 — carpeta médica completa de una mascota (resumen + carnet + historial + citas),
     *  visible para VETERINARIO/RECEPCIONISTA. Incluye historial de OTRAS veterinarias
     *  a propósito: listarPorMascota no filtra por veterinaria, es el diferencial de VetLink. */
    @GetMapping("/paciente/{idMascota}")
    public String carpetaMedica(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        var mascota = mascotaService.buscarPorId(idMascota);
        verificarAccesoPaciente(mascota, principal);
        var historial = historialService.listarPorMascota(idMascota);

        model.addAttribute("mascota", mascota);
        model.addAttribute("historial", historial);
        model.addAttribute("carnet", carnetVacunaService.listarPorMascota(idMascota));
        model.addAttribute("citas", citaRepository.findByMascota_IdMascotaOrderByFechaDescHoraDesc(idMascota));
        model.addAttribute("idUsuarioActual", principal.getUsuario().getIdUsuario());

        var minutos = new LinkedHashMap<Integer, Long>();
        for (HistorialClinico h : historial) minutos.put(h.getNumeroHistorial(), historialService.minutosRestantesParaEliminar(h));
        model.addAttribute("minutosParaEliminar", minutos);
        var archivos = new LinkedHashMap<Integer, java.util.List<com.vetlink.historial.model.ArchivoHistorial>>();
        for (HistorialClinico h : historial) archivos.put(h.getNumeroHistorial(), archivoRepository.findByHistorial_NumeroHistorial(h.getNumeroHistorial()));
        model.addAttribute("archivosPorHistorial", archivos);

        model.addAttribute("formHistorial", new HistorialClinicoForm());
        model.addAttribute("vacunasEspecie", vacunaRepository.findByEspecie_IdEspecie(mascota.getEspecie().getIdEspecie()));
        model.addAttribute("lotes", loteVacunaRepository.findAll());
        return "historial/paciente";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        HistorialClinico historial = historialService.buscarPorId(id);
        verificarAutor(historial, principal);
        cargarShell(model, principal);
        HistorialClinicoForm form = new HistorialClinicoForm();
        form.setIdMascota(historial.getMascota().getIdMascota());
        form.setMotivoConsulta(historial.getMotivoConsulta());
        form.setDiagnostico(historial.getDiagnostico());
        form.setObservaciones(historial.getObservaciones());
        form.setProximaCita(historial.getProximaCita());
        model.addAttribute("form", form);
        model.addAttribute("mascota", historial.getMascota());
        model.addAttribute("editando", true);
        model.addAttribute("historialId", id);
        return "historial/formulario";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                          @Valid @ModelAttribute("form") HistorialClinicoForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            cargarShell(model, principal);
            model.addAttribute("mascota", mascotaService.buscarPorId(form.getIdMascota()));
            model.addAttribute("editando", true);
            return "historial/formulario";
        }
        try {
            historialService.actualizar(id, form, principal.getUsuario().getIdUsuario());
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        }
        return "redirect:/historial/paciente/" + form.getIdMascota() + "?actualizado";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, @RequestParam Integer idMascota,
                            @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        try {
            historialService.eliminar(id, principal.getUsuario().getIdUsuario());
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        }
        return "redirect:/historial/paciente/" + idMascota + "?eliminado";
    }

    @PostMapping("/{id}/archivos")
    public String subirArchivo(@PathVariable Integer id, @RequestParam("archivo") MultipartFile archivo,
                               @AuthenticationPrincipal VetlinkUserPrincipal principal) throws java.io.IOException {
        HistorialClinico historial = historialService.buscarPorId(id);
        verificarAutor(historial, principal);
        if (archivo.isEmpty() || archivo.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("El archivo debe pesar entre 1 byte y 10 MB");
        String original = archivo.getOriginalFilename() == null ? "adjunto" : Path.of(archivo.getOriginalFilename()).getFileName().toString();
        Path carpeta = Path.of("uploads", "historial", String.valueOf(id)); Files.createDirectories(carpeta);
        String guardado = UUID.randomUUID() + "-" + original; Path destino = carpeta.resolve(guardado); archivo.transferTo(destino);
        var adjunto = new com.vetlink.historial.model.ArchivoHistorial(); adjunto.setHistorial(historial);
        String tipoArchivo = archivo.getContentType() == null || archivo.getContentType().isBlank() ? "application/octet-stream" : archivo.getContentType();
        if (tipoArchivo.length() > 255) tipoArchivo = tipoArchivo.substring(0, 255);
        adjunto.setNombreArchivo(original); adjunto.setRutaArchivo(destino.toString().replace('\\','/'));
        adjunto.setTipoArchivo(tipoArchivo); archivoRepository.save(adjunto);
        return "redirect:/historial/paciente/" + historial.getMascota().getIdMascota() + "?archivoSubido";
    }

    @GetMapping("/archivos/{idArchivo}")
    public ResponseEntity<org.springframework.core.io.Resource> descargarArchivo(@PathVariable Integer idArchivo,
             @AuthenticationPrincipal VetlinkUserPrincipal principal) throws java.io.IOException {
        var adjunto = archivoRepository.findById(idArchivo).orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));
        verificarAccesoPaciente(adjunto.getHistorial().getMascota(), principal);
        Path path=Path.of(adjunto.getRutaArchivo()); if(!Files.exists(path)) throw new IllegalArgumentException("El archivo ya no existe en el almacenamiento");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(adjunto.getTipoArchivo())).body(new org.springframework.core.io.FileSystemResource(path));
    }

    private void verificarAutor(HistorialClinico historial, VetlinkUserPrincipal principal) {
        if (!historial.getVeterinario().getIdUsuario().equals(principal.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("Solo quien registró este historial puede modificarlo.");
        }
    }


    @PostMapping("/nuevo")
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                         @Valid @ModelAttribute("form") HistorialClinicoForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            cargarShell(model, principal);
            model.addAttribute("mascota", mascotaService.buscarPorId(form.getIdMascota()));
            return "historial/formulario";
        }
        verificarAccesoPaciente(mascotaService.buscarPorId(form.getIdMascota()), principal);
        historialService.crear(form, principal.getUsuario().getIdUsuario());
        return "redirect:/historial/paciente/" + form.getIdMascota() + "?creado";
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "historial");
        model.addAttribute("navItems", switch (principal.getUsuario().getRol()) {
            case TUTOR -> DashboardController.navTutor();
            case VETERINARIO -> DashboardController.navVeterinario();
            case RECEPCIONISTA -> DashboardController.navRecepcionista();
        });
    }

    private void verificarAccesoPaciente(com.vetlink.mascota.model.Mascota mascota,
                                      VetlinkUserPrincipal principal) {
        var rol = principal.getUsuario().getRol();
        boolean permitido = switch (rol) {
            case TUTOR -> mascota.getTutor().getIdUsuario().equals(principal.getUsuario().getIdUsuario());
            case VETERINARIO -> citaRepository.findByVeterinario_IdUsuarioOrderByFechaDesc(
                    principal.getUsuario().getIdUsuario()).stream()
                    .anyMatch(c -> c.getMascota() != null && c.getMascota().getIdMascota().equals(mascota.getIdMascota()));
            case RECEPCIONISTA -> citaRepository.buscarConFiltros(
                    principal.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria(), null, null, null, null).stream()
                    .anyMatch(c -> c.getMascota() != null && c.getMascota().getIdMascota().equals(mascota.getIdMascota()));
        };
        if (!permitido) {
            throw new AccessDeniedException("No tienes permiso para consultar esta mascota");
        }
    }
}
