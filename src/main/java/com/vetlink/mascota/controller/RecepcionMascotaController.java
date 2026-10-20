package com.vetlink.mascota.controller;

import com.vetlink.mascota.dto.MascotaForm;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.EspecieRepository;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.mascota.repository.RazaRepository;
import com.vetlink.mascota.service.MascotaService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Tutor;
import com.vetlink.usuario.repository.TutorRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/** RF0001/RF0002/RF0004 para el rol RECEPCIONISTA: registra y actualiza mascotas
 *  en nombre de un dueño ya existente en el sistema (los dueños no dejan de ser
 *  quienes las poseen; la recepcionista solo captura los datos por ellos). */
@Controller
@RequestMapping("/recepcion/mascotas")
public class RecepcionMascotaController {

    private final MascotaService mascotaService;
    private final MascotaRepository mascotaRepository;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final TutorRepository tutorRepository;

    public RecepcionMascotaController(MascotaService mascotaService, MascotaRepository mascotaRepository,
                                       EspecieRepository especieRepository, RazaRepository razaRepository,
                                       TutorRepository tutorRepository) {
        this.mascotaService = mascotaService;
        this.mascotaRepository = mascotaRepository;
        this.especieRepository = especieRepository;
        this.razaRepository = razaRepository;
        this.tutorRepository = tutorRepository;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                          @RequestParam(required = false) String q, Model model) {
        cargarShell(model, principal);
        Integer idVeterinaria = principal.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria();
        var mascotas = mascotaRepository.buscarConCitaEnVeterinaria(idVeterinaria);
        if (q != null && !q.isBlank()) {
            String qLower = q.toLowerCase();
            mascotas = mascotas.stream().filter(m -> m.getNombre().toLowerCase().contains(qLower)).toList();
        }
        model.addAttribute("mascotas", mascotas);
        model.addAttribute("q", q);
        return "recepcion/mascotas/lista";
    }

    @GetMapping("/nueva")
    public String nuevaForm(@AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        cargarCatalogos(model);
        model.addAttribute("form", new MascotaForm());
        return "recepcion/mascotas/formulario";
    }

    @PostMapping("/nueva")
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal principal,
                         @Valid @ModelAttribute("form") MascotaForm form, BindingResult result,
                         @RequestParam Integer idTutor, Model model) {
        if (result.hasErrors()) {
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "recepcion/mascotas/formulario";
        }
        try {
            mascotaService.crear(form, idTutor);
        } catch (RuntimeException e) {
            cargarShell(model, principal);
            cargarCatalogos(model);
            model.addAttribute("errorGeneral", e.getMessage());
            return "recepcion/mascotas/formulario";
        }
        return "redirect:/recepcion/mascotas?creada";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal, Model model) {
        cargarShell(model, principal);
        cargarCatalogos(model);
        Mascota mascota = mascotaService.buscarPorId(id);
        MascotaForm form = new MascotaForm();
        form.setIdMascota(mascota.getIdMascota());
        form.setNombre(mascota.getNombre());
        form.setIdEspecie(mascota.getEspecie().getIdEspecie());
        form.setIdRaza(mascota.getRaza().getIdRaza());
        form.setGenero(mascota.getGenero());
        form.setFechaNacimiento(mascota.getFechaNacimiento());
        form.setPeso(mascota.getPeso());
        form.setFoto(mascota.getFoto());
        model.addAttribute("form", form);
        model.addAttribute("dueno", mascota.getTutor());
        return "recepcion/mascotas/formulario";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, @AuthenticationPrincipal VetlinkUserPrincipal principal,
                          @Valid @ModelAttribute("form") MascotaForm form, BindingResult result, Model model) {
        form.setIdMascota(id);
        if (result.hasErrors()) {
            cargarShell(model, principal);
            cargarCatalogos(model);
            return "recepcion/mascotas/formulario";
        }
        mascotaService.actualizarSinVerificarTutor(form);
        return "redirect:/recepcion/mascotas?actualizada";
    }

    private void cargarShell(Model model, VetlinkUserPrincipal principal) {
        model.addAttribute("usuario", principal.getUsuario());
        model.addAttribute("seccionActiva", "mascotas");
        model.addAttribute("navItems", DashboardController.navRecepcionista());
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("especies", especieRepository.findAll());
        model.addAttribute("razas", razaRepository.findAll());
        model.addAttribute("duenos", tutorRepository.findAll());
    }
}
