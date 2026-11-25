package com.vetlink.historial.controller;

import com.vetlink.auditoria.AuditoriaService;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.model.ArchivoHistorial;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.repository.ArchivoHistorialRepository;
import com.vetlink.historial.service.ArchivoClinicoService;
import com.vetlink.historial.service.ExpedienteService;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.security.AccesoPacienteService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Rol;
import com.vetlink.vacuna.service.CarnetVacunaService;
import com.vetlink.veterinaria.service.VeterinariaInfoService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Historia clínica: gestor de expedientes (carpetas por mascota), documento clínico
 * estructurado, edición por el autor (24 h) y descarga autorizada de adjuntos.
 * Los registros nuevos solo se crean durante una cita en atención (ver AgendaController).
 */
@Controller
@RequestMapping("/historial")
public class HistorialClinicoController {

    private final HistorialClinicoService historialService;
    private final MascotaService mascotaService;
    private final CitaRepository citaRepository;
    private final CarnetVacunaService carnetVacunaService;
    private final ArchivoHistorialRepository archivoRepository;
    private final ArchivoClinicoService archivoClinicoService;
    private final AccesoPacienteService accesoPacienteService;
    private final ExpedienteService expedientes;
    private final VeterinariaInfoService infoVeterinaria;
    private final AuditoriaService auditoria;
    private final com.vetlink.mascota.repository.MascotaRepository mascotaRepository;

    public HistorialClinicoController(HistorialClinicoService historialService, MascotaService mascotaService,
                                      CitaRepository citaRepository, CarnetVacunaService carnetVacunaService,
                                      ArchivoHistorialRepository archivoRepository, ArchivoClinicoService archivoClinicoService,
                                      AccesoPacienteService accesoPacienteService, ExpedienteService expedientes,
                                      VeterinariaInfoService infoVeterinaria, AuditoriaService auditoria,
                                      com.vetlink.mascota.repository.MascotaRepository mascotaRepository) {
        this.historialService = historialService;
        this.mascotaService = mascotaService;
        this.citaRepository = citaRepository;
        this.carnetVacunaService = carnetVacunaService;
        this.archivoRepository = archivoRepository;
        this.archivoClinicoService = archivoClinicoService;
        this.accesoPacienteService = accesoPacienteService;
        this.expedientes = expedientes;
        this.infoVeterinaria = infoVeterinaria;
        this.auditoria = auditoria;
        this.mascotaRepository = mascotaRepository;
    }

    /** Tutor: sus carpetas. */
    @GetMapping("/mias")
    public String misHistoriales(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal, "historial");
        model.addAttribute("carpetas", expedientes.carpetasPara(principal.getUsuario()));
        model.addAttribute("modoLectura", true);
        return "historial/mias";
    }

    /**
     * VETERINARIO: lista de pacientes con relación clínica.
     * RECEPCIONISTA: gestor de expedientes de solo lectura (no es una copia del módulo Mascotas).
     */
    @GetMapping("/pacientes")
    public String misPacientes(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var usuario = principal.getUsuario();
        if (usuario.getRol() == Rol.RECEPCIONISTA) {
            cargarShell(model, principal, "historial");
            model.addAttribute("carpetas", expedientes.carpetasPara(usuario));
            model.addAttribute("modoLectura", true);
            return "historial/mias";
        }
        cargarShell(model, principal, "pacientes");
        model.addAttribute("pacientes", mascotaRepository.buscarConCitaConVeterinario(usuario.getIdUsuario()));
        return "historial/pacientes";
    }

    /** Carpeta de una sola mascota. */
    @GetMapping("/mascota/{idMascota}")
    public String carpeta(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var mascota = mascotaService.buscarPorId(idMascota);
        accesoPacienteService.verificar(mascota, principal);
        cargarShell(model, principal, "historial");
        model.addAttribute("mascota", mascota);
        model.addAttribute("carpetas", List.of(expedientes.carpeta(mascota, principal.getUsuario())));
        model.addAttribute("abierta", idMascota);
        return "historial/mias";
    }

    /** Documento clínico con estructura de expediente. */
    @GetMapping("/documento/{numero}")
    public String documento(@PathVariable Integer numero, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        HistorialClinico h = historialService.buscarPorId(numero);
        accesoPacienteService.verificarDocumento(h, principal);
        cargarShell(model, principal, principal.getUsuario().getRol() == Rol.VETERINARIO ? "pacientes" : "historial");
        model.addAttribute("h", h);
        model.addAttribute("mascota", h.getMascota());
        model.addAttribute("archivos", archivoRepository.findByHistorial_NumeroHistorial(numero));
        Integer idClinica = h.getVeterinaria() != null ? h.getVeterinaria().getIdVeterinaria() : null;
        model.addAttribute("clinica", h.getVeterinaria());
        model.addAttribute("direccionClinica", idClinica == null ? "" : infoVeterinaria.direccion(idClinica));
        model.addAttribute("logoClinica", VeterinariaInfoService.logoUrl(h.getVeterinaria()));
        model.addAttribute("editable", historialService.esEditable(h, principal.getUsuario().getIdUsuario()));
        auditoria.registrar(principal.getUsuario().getIdUsuario(), "HISTORIAL_CONSULTAR", "historial_clinico", numero, null);
        return "historial/documento";
    }

    /** VETERINARIO: carpeta médica completa (resumen, carnet, historial, citas). Recepción va a la carpeta de lectura. */
    @GetMapping("/paciente/{idMascota}")
    public String carpetaMedica(@PathVariable Integer idMascota, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        var mascota = mascotaService.buscarPorId(idMascota);
        if (principal.getUsuario().getRol() == Rol.RECEPCIONISTA) {
            accesoPacienteService.verificar(mascota, principal);
            return "redirect:/historial/mascota/" + idMascota;
        }
        accesoPacienteService.verificar(mascota, principal);
        cargarShell(model, principal, "pacientes");
        var historial = historialService.listarPorMascota(idMascota);
        model.addAttribute("mascota", mascota);
        model.addAttribute("historial", historial);
        model.addAttribute("carnet", carnetVacunaService.listarPorMascota(idMascota));
        Integer yo = principal.getUsuario().getIdUsuario();
        model.addAttribute("citas", citaRepository.findByMascota_IdMascotaOrderByFechaDescHoraDesc(idMascota).stream()
                .filter(c -> principal.getUsuario().getRol() != Rol.VETERINARIO || c.getVeterinario().getIdUsuario().equals(yo))
                .toList());
        model.addAttribute("idUsuarioActual", yo);
        model.addAttribute("editables", historial.stream()
                .filter(h -> historialService.esEditable(h, yo))
                .map(HistorialClinico::getNumeroHistorial)
                .collect(java.util.stream.Collectors.toSet()));
        var archivos = new LinkedHashMap<Integer, List<ArchivoHistorial>>();
        for (HistorialClinico h : historial) archivos.put(h.getNumeroHistorial(), archivoRepository.findByHistorial_NumeroHistorial(h.getNumeroHistorial()));
        model.addAttribute("archivosPorHistorial", archivos);
        return "historial/paciente";
    }

    /** Ya no se crean historias fuera de una cita en atención. */
    @GetMapping({"/nuevo/{idMascota}", "/nuevo"})
    public String nuevoForm(@PathVariable(required = false) Integer idMascota, RedirectAttributes ra) {
        ra.addFlashAttribute("errorGeneral", "Los registros clínicos se crean desde una cita en atención (Mi agenda → Atender cita).");
        return idMascota == null ? "redirect:/agenda" : "redirect:/historial/paciente/" + idMascota;
    }

    @PostMapping("/nuevo")
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal principal) {
        throw new AccessDeniedException("Los registros clínicos solo se crean durante una cita en atención.");
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        HistorialClinico historial = historialService.buscarPorId(id);
        verificarEditable(historial, principal);
        cargarShell(model, principal, "pacientes");
        HistorialClinicoForm form = new HistorialClinicoForm();
        form.setIdMascota(historial.getMascota().getIdMascota());
        form.setTipoRegistro(historial.getTipoRegistro());
        form.setMotivoConsulta(historial.getMotivoConsulta());
        form.setAntecedentes(historial.getAntecedentes());
        form.setTemperatura(historial.getTemperatura());
        form.setFrecuenciaCardiaca(historial.getFrecuenciaCardiaca());
        form.setFrecuenciaRespiratoria(historial.getFrecuenciaRespiratoria());
        form.setPesoKg(historial.getPesoKg());
        form.setEvaluacionClinica(historial.getEvaluacionClinica());
        form.setHallazgos(historial.getHallazgos());
        form.setDiagnostico(historial.getDiagnostico());
        form.setTratamiento(historial.getTratamiento());
        form.setMedicamentosIndicados(historial.getMedicamentosIndicados());
        form.setRecomendaciones(historial.getRecomendaciones());
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
        HistorialClinico existente = historialService.buscarPorId(id);
        verificarEditable(existente, principal);
        form.setIdMascota(existente.getMascota().getIdMascota());
        if (result.hasErrors()) {
            cargarShell(model, principal, "pacientes");
            model.addAttribute("mascota", existente.getMascota());
            model.addAttribute("historialId", id);
            model.addAttribute("editando", true);
            return "historial/formulario";
        }
        try {
            historialService.actualizar(id, form, principal.getUsuario().getIdUsuario());
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        }
        auditoria.registrar(principal.getUsuario().getIdUsuario(), "HISTORIAL_EDITAR", "historial_clinico", id, null);
        return "redirect:/historial/paciente/" + form.getIdMascota() + "?actualizado";
    }

    /** Adjuntar a un registro propio dentro del plazo de edición. */
    @PostMapping("/{id}/archivos")
    public String subirArchivo(@PathVariable Integer id, @RequestParam("archivo") MultipartFile archivo,
                               @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes ra) {
        HistorialClinico historial = historialService.buscarPorId(id);
        verificarEditable(historial, principal);
        try {
            archivoClinicoService.guardar(historial, archivo, principal.getUsuario());
            auditoria.registrar(principal.getUsuario().getIdUsuario(), "ADJUNTO_SUBIR", "historial_clinico", id, null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorGeneral", e.getMessage());
            return "redirect:/historial/documento/" + id;
        }
        return "redirect:/historial/documento/" + id + "?archivoSubido";
    }

    /** Descarga / vista previa autorizada de un adjunto. */
    @GetMapping("/archivos/{idArchivo}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Integer idArchivo,
                                                     @RequestParam(required = false, defaultValue = "false") boolean descargar,
                                                     @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        ArchivoHistorial adjunto = archivoRepository.findById(idArchivo)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));
        accesoPacienteService.verificarDocumento(adjunto.getHistorial(), principal);
        Path path = archivoClinicoService.archivoEnDisco(adjunto)
                .orElseThrow(() -> new IllegalArgumentException("El archivo ya no existe en el almacenamiento"));
        String tipo = adjunto.getTipoArchivo();
        boolean seguroEnLinea = "application/pdf".equals(tipo) || "image/png".equals(tipo) || "image/jpeg".equals(tipo);
        ContentDisposition cd = (descargar || !seguroEnLinea ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(adjunto.getNombreArchivo() == null ? "adjunto" : adjunto.getNombreArchivo(), StandardCharsets.UTF_8)
                .build();
        auditoria.registrar(principal.getUsuario().getIdUsuario(), "ADJUNTO_DESCARGAR", "archivo_historial", idArchivo, null);
        return ResponseEntity.ok()
                .contentType(seguroEnLinea ? MediaType.parseMediaType(tipo) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new FileSystemResource(path));
    }

    /** VL-19: solo el autor y solo durante las 24 horas siguientes al registro. */
    private void verificarEditable(HistorialClinico historial, VetlinkUserPrincipal principal) {
        if (!historial.getVeterinario().getIdUsuario().equals(principal.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("Solo quien registró este historial puede modificarlo.");
        }
        if (!historialService.esEditable(historial, principal.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("La historia ya no se puede editar");
        }
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal, String seccion) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", seccion);
        model.addAttribute("navItems", DashboardController.navPara(principal.getUsuario()));
    }
}
