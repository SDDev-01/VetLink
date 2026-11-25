package com.vetlink.mascota.controller;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.service.CitaService;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.mascota.repository.RazaRepository;
import com.vetlink.mascota.service.FotoMascotaService;
import com.vetlink.mascota.service.GestionMascotaService;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.security.AccesoPacienteService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.vacuna.service.CarnetVacunaService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Mascotas del TUTOR: alta, edición, ficha, transferencia y situación (sin borrado directo). */
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
    private final GestionMascotaService gestion;
    private final com.vetlink.mascota.service.RazaCatalogoService catalogoRazas;

    public MascotaController(MascotaService mascotaService, EspecieRepository especieRepository, RazaRepository razaRepository,
                             CitaService citaService, HistorialClinicoService historialClinicoService,
                             CarnetVacunaService carnetVacunaService, FotoMascotaService fotoMascotaService,
                             AccesoPacienteService accesoPacienteService, GestionMascotaService gestion,
                             com.vetlink.mascota.service.RazaCatalogoService catalogoRazas) {
        this.catalogoRazas = catalogoRazas;
        this.mascotaService = mascotaService;
        this.especieRepository = especieRepository;
        this.razaRepository = razaRepository;
        this.citaService = citaService;
        this.historialClinicoService = historialClinicoService;
        this.carnetVacunaService = carnetVacunaService;
        this.fotoMascotaService = fotoMascotaService;
        this.accesoPacienteService = accesoPacienteService;
        this.gestion = gestion;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        Integer yo = principal.getUsuario().getIdUsuario();
        var mascotas = mascotaService.listarActivasPorTutor(yo);
        model.addAttribute("mascotas", mascotas);
        model.addAttribute("transferenciasPendientes", mascotas.stream()
                .map(m -> gestion.pendienteDeMascota(m.getIdMascota()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(t -> t.getMascota().getIdMascota(), t -> t)));
        model.addAttribute("recibidas", gestion.recibidasPendientes(yo));
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
        validarRaza(form, result);
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
        form.setFoto(mascota.getFotoUrl());
        cargarShell(model, principal);
        cargarCatalogos(model);
        model.addAttribute("form", form);
        model.addAttribute("mascotaActual", mascota);
        return "mascotas/formulario";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                             @Valid @ModelAttribute("form") MascotaForm form, BindingResult result,
                             @RequestParam(value = "archivoFoto", required = false) MultipartFile archivoFoto, Model model) {
        var actual = mascotaService.buscarPorId(id);
        verificarPropietario(actual, principal);
        form.setIdMascota(id);
        // DUE-01: especie, género y fecha de nacimiento no se editan: se usan los guardados
        // (lo que llegue en la petición se descarta y sus errores de validación no aplican).
        form.setIdEspecie(actual.getEspecie().getIdEspecie());
        form.setGenero(actual.getGenero());
        form.setFechaNacimiento(actual.getFechaNacimiento());
        validarFoto(archivoFoto, result);
        validarRaza(form, result);
        boolean hayErrores = result.hasGlobalErrors() || result.getFieldErrors().stream()
                .anyMatch(e -> !MascotaForm.CAMPOS_INMUTABLES.contains(e.getField()));
        if (hayErrores) {
            return volverAEditar(form, actual, principal, model, null);
        }
        try {
            var mascota = mascotaService.actualizar(form, principal.getUsuario().getIdUsuario());
            fotoMascotaService.guardar(mascota, archivoFoto);
        } catch (SecurityException e) {
            throw new org.springframework.security.access.AccessDeniedException(e.getMessage());
        } catch (RuntimeException e) {
            return volverAEditar(form, actual, principal, model, e.getMessage());
        }
        return "redirect:/mascotas?actualizada";
    }

    private String volverAEditar(MascotaForm form, com.vetlink.mascota.model.Mascota actual,
                                 VetlinkUserPrincipal principal, Model model, String error) {
        form.setFoto(actual.getFotoUrl());
        if (error != null) model.addAttribute("errorGeneral", error);
        model.addAttribute("mascotaActual", actual);
        cargarShell(model, principal);
        cargarCatalogos(model);
        return "mascotas/formulario";
    }

    /** DUE-02: una raza del catálogo o «Otra raza» con su nombre. */
    private static void validarRaza(MascotaForm form, BindingResult result) {
        if (form.getIdRaza() == null && !form.usaRazaPersonalizada()) {
            result.rejectValue("idRaza", "raza.requerida", "Selecciona una raza o elige «Otra raza» y escríbela");
        }
    }

    /** El borrado directo se reemplazó por transferencia y reporte de situación. */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        ra.addFlashAttribute("errorMascota", "Las mascotas ya no se eliminan. Usa «Transferir» o reporta su situación "
                + "(perdida, encontrada o fallecida) para conservar su historial.");
        return "redirect:/mascotas";
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
                .filter(c -> EstadoCita.RESERVAS_VIGENTES.contains(c.getEstado())).toList());
        model.addAttribute("historial", historialClinicoService.listarPorMascota(id));
        model.addAttribute("carnet", carnetVacunaService.listarPorMascota(id));
        model.addAttribute("eventos", gestion.eventos(id));
        model.addAttribute("transferenciaPendiente", gestion.pendienteDeMascota(id).orElse(null));
        return "mascotas/detalle";
    }

    // ------------------------------------------------------------------
    // Transferencias
    // ------------------------------------------------------------------

    @GetMapping("/transferencias")
    public String transferencias(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        Integer yo = principal.getUsuario().getIdUsuario();
        model.addAttribute("recibidas", gestion.recibidas(yo));
        model.addAttribute("enviadas", gestion.enviadas(yo));
        return "mascotas/transferencias";
    }

    @PostMapping("/{id}/transferir")
    public String transferir(@PathVariable Integer id, @RequestParam(required = false) String destino,
                             @RequestParam(required = false) String mensaje,
                             @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes ra) {
        if (destino == null || destino.isBlank()) {
            ra.addFlashAttribute("errorMascota", "Indica el documento o correo del nuevo tutor.");
            return "redirect:/mascotas/" + id;
        }
        return ejecutar(ra, "/mascotas/" + id, "Solicitud enviada. La propiedad cambiará cuando el nuevo tutor la acepte.",
                () -> gestion.solicitarTransferencia(id, principal.getUsuario().getIdUsuario(), destino, mensaje));
    }

    @PostMapping("/transferencias/{idT}/aceptar")
    public String aceptar(@PathVariable Integer idT, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                          RedirectAttributes ra) {
        return ejecutar(ra, "/mascotas/transferencias", "Transferencia aceptada. La mascota ya aparece en tu lista.",
                () -> gestion.aceptar(idT, principal.getUsuario().getIdUsuario()));
    }

    @PostMapping("/transferencias/{idT}/rechazar")
    public String rechazar(@PathVariable Integer idT, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                           RedirectAttributes ra) {
        return ejecutar(ra, "/mascotas/transferencias", "Rechazaste la transferencia.",
                () -> gestion.rechazar(idT, principal.getUsuario().getIdUsuario()));
    }

    @PostMapping("/transferencias/{idT}/cancelar")
    public String cancelarTransferencia(@PathVariable Integer idT, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                                        RedirectAttributes ra) {
        return ejecutar(ra, "/mascotas/transferencias", "Cancelaste la transferencia.",
                () -> gestion.cancelar(idT, principal.getUsuario().getIdUsuario()));
    }

    // ------------------------------------------------------------------
    // Situación
    // ------------------------------------------------------------------

    @PostMapping("/{id}/perdida")
    public String perdida(@PathVariable Integer id, @RequestParam(required = false) String detalle,
                          @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes ra) {
        return ejecutar(ra, "/mascotas/" + id, "Reportaste a la mascota como perdida.",
                () -> gestion.reportarPerdida(id, principal.getUsuario().getIdUsuario(), detalle));
    }

    @PostMapping("/{id}/encontrada")
    public String encontrada(@PathVariable Integer id, @RequestParam(required = false) String detalle,
                             @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes ra) {
        return ejecutar(ra, "/mascotas/" + id, "¡Qué buena noticia! La mascota vuelve a estar activa.",
                () -> gestion.reportarEncontrada(id, principal.getUsuario().getIdUsuario(), detalle));
    }

    @PostMapping("/{id}/fallecida")
    public String fallecida(@PathVariable Integer id, @RequestParam(required = false) String detalle,
                            @AuthenticationPrincipal VetlinkUserPrincipal principal, RedirectAttributes ra) {
        return ejecutar(ra, "/mascotas/" + id, "Lamentamos tu pérdida. Registramos el fallecimiento y conservamos su historial.",
                () -> gestion.reportarFallecimiento(id, principal.getUsuario().getIdUsuario(), detalle));
    }

    private String ejecutar(RedirectAttributes ra, String volver, String ok, Supplier<?> accion) {
        try {
            accion.get();
            ra.addFlashAttribute("mensajeOk", ok);
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMascota", e.getMessage());
        }
        return "redirect:" + volver;
    }

    /** VL-22: la foto solo la ve quien tiene acceso a la mascota. */
    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        var mascota = mascotaService.buscarPorId(id);
        accesoPacienteService.verificarDatosBasicos(mascota, principal);
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

    /** Razas ordenadas alfabéticamente; viajan con su especie para que el formulario solo muestre las que corresponden. */
    private void cargarCatalogos(Model model) {
        model.addAttribute("especies", especieRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(com.vetlink.mascota.model.Especie::getNombreEspecie)).toList());
        model.addAttribute("razas", catalogoRazas.todasOrdenadas());
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "mascotas");
        model.addAttribute("navItems", DashboardController.navTutor());
    }

    private void verificarPropietario(com.vetlink.mascota.model.Mascota mascota, VetlinkUserPrincipal principal) {
        accesoPacienteService.verificarPropietario(mascota, principal);
    }
}
