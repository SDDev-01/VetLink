package com.vetlink.emergencia.controller;

import com.vetlink.emergencia.dto.EmergenciaForm;
import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/emergencias")
public class EmergenciaController {

    private final EmergenciaRepository repo;
    private final MascotaRepository mascotas;
    private final VeterinariaRepository veterinarias;
    private final UsuarioRepository usuarios;
    private final VeterinarioRepository veterinarios;

    public EmergenciaController(EmergenciaRepository repo, MascotaRepository mascotas,
                                VeterinariaRepository veterinarias, UsuarioRepository usuarios,
                                VeterinarioRepository veterinarios) {
        this.repo = repo;
        this.mascotas = mascotas;
        this.veterinarias = veterinarias;
        this.usuarios = usuarios;
        this.veterinarios = veterinarios;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        shell(m, p);
        var u = p.getUsuario();
        switch (u.getRol()) {
            case TUTOR -> {
                m.addAttribute("emergencias", repo.findByMascota_Tutor_IdUsuarioOrderByCreadaEnDesc(u.getIdUsuario()));
                m.addAttribute("mascotas", mascotas.findByTutor_IdUsuarioAndEstado(u.getIdUsuario(), (byte) 1));
                m.addAttribute("veterinarias", veterinarias.findByEstado((byte) 1));
                m.addAttribute("form", new EmergenciaForm());
            }
            case VETERINARIO ->
                m.addAttribute("emergencias", repo.findByVeterinario_IdUsuarioOrderByCreadaEnDesc(u.getIdUsuario()));
            case RECEPCIONISTA -> {
                var v = u.getRecepcionista().getVeterinaria();
                m.addAttribute("emergencias", repo.findByVeterinaria_IdVeterinariaOrderByCreadaEnDesc(v.getIdVeterinaria()));
                // VL-25: consulta directa en vez de cargar todos los usuarios.
                m.addAttribute("veterinarios", veterinarios.findByVeterinaria_IdVeterinaria(v.getIdVeterinaria()).stream()
                        .map(Veterinario::getUsuario)
                        .toList());
            }
        }
        return "emergencias/lista";
    }

    @PostMapping
    public String crear(@AuthenticationPrincipal VetlinkUserPrincipal p,
                        @Valid @ModelAttribute("form") EmergenciaForm f, BindingResult r, Model m) {
        if (r.hasErrors()) {
            return listar(p, m);
        }
        var mascota = mascotas.findById(f.getIdMascota()).orElseThrow();
        if (!mascota.getTutor().getIdUsuario().equals(p.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("La mascota no te pertenece");
        }
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        var veterinaria = veterinarias.findById(f.getIdVeterinaria()).orElseThrow();
        // VL-09: solo clínicas verificadas reciben emergencias.
        if (!veterinaria.isActiva()) {
            throw new IllegalArgumentException("Veterinaria no encontrada");
        }
        e.setVeterinaria(veterinaria);
        e.setMotivo(f.getMotivo().trim());
        e.setDetalle(f.getDetalle());
        e.setCreadaEn(LocalDateTime.now());
        repo.save(e);
        return "redirect:/emergencias?creada";
    }

    @PostMapping("/{id}/asignar")
    public String asignar(@PathVariable Integer id, @RequestParam Integer idVeterinario,
                          @AuthenticationPrincipal VetlinkUserPrincipal p) {
        Emergencia e = deMiClinica(id, p);
        var v = usuarios.findById(idVeterinario).orElseThrow();
        if (v.getVeterinario() == null
                || !v.getVeterinario().getVeterinaria().getIdVeterinaria().equals(e.getVeterinaria().getIdVeterinaria())) {
            throw new AccessDeniedException("Veterinario inválido");
        }
        e.setVeterinario(v);
        e.setEstado(EstadoEmergencia.ASIGNADA);
        repo.save(e);
        return "redirect:/emergencias?asignada";
    }

    /**
     * VL-04: cambio de estado con reglas por rol.
     * - Cerrada (ATENDIDA/CANCELADA): no se toca.
     * - TUTOR: solo cancela sus propias emergencias PENDIENTES.
     * - RECEPCIONISTA (de la clínica): ASIGNADA (con veterinario) o CANCELADA; nunca ATENDIDA.
     * - VETERINARIO asignado: solo ASIGNADA -> ATENDIDA.
     */
    @PostMapping("/{id}/estado")
    public String estado(@PathVariable Integer id, @RequestParam EstadoEmergencia estado,
                         @AuthenticationPrincipal VetlinkUserPrincipal p) {
        Emergencia e = repo.findById(id).orElseThrow();
        Usuario u = p.getUsuario();
        EstadoEmergencia actual = e.getEstado();

        if (actual == EstadoEmergencia.ATENDIDA || actual == EstadoEmergencia.CANCELADA) {
            throw new IllegalStateException("La emergencia ya está cerrada");
        }

        Rol rol = u.getRol();
        if (rol == Rol.TUTOR) {
            boolean esSuya = e.getMascota() != null
                    && e.getMascota().getTutor().getIdUsuario().equals(u.getIdUsuario());
            if (estado != EstadoEmergencia.CANCELADA || !esSuya || actual != EstadoEmergencia.PENDIENTE) {
                throw new AccessDeniedException("Cambio de estado no permitido");
            }
        } else if (rol == Rol.RECEPCIONISTA) {
            deMiClinica(id, p);
            if (estado == EstadoEmergencia.ASIGNADA) {
                if (e.getVeterinario() == null) {
                    throw new IllegalStateException("Primero asigna un veterinario a la emergencia");
                }
            } else if (estado != EstadoEmergencia.CANCELADA) {
                throw new AccessDeniedException("Cambio de estado no permitido");
            }
        } else if (rol == Rol.VETERINARIO) {
            boolean esAsignado = e.getVeterinario() != null
                    && e.getVeterinario().getIdUsuario().equals(u.getIdUsuario());
            if (!esAsignado || actual != EstadoEmergencia.ASIGNADA || estado != EstadoEmergencia.ATENDIDA) {
                throw new AccessDeniedException("Cambio de estado no permitido");
            }
        } else {
            throw new AccessDeniedException("Cambio de estado no permitido");
        }

        e.setEstado(estado);
        repo.save(e);
        return "redirect:/emergencias?actualizada";
    }

    /** Errores de negocio (emergencia cerrada, falta veterinario): vuelve a la lista con el mensaje. */
    @ExceptionHandler(IllegalStateException.class)
    public String errorDeEstado(IllegalStateException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/emergencias";
    }

    private Emergencia deMiClinica(Integer id, VetlinkUserPrincipal p) {
        Emergencia e = repo.findById(id).orElseThrow();
        if (p.getUsuario().getRecepcionista() == null
                || !e.getVeterinaria().getIdVeterinaria()
                        .equals(p.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria())) {
            throw new AccessDeniedException("No pertenece a tu veterinaria");
        }
        return e;
    }

    private void shell(Model m, VetlinkUserPrincipal p) {
        m.addAttribute("usuario", p.getUsuario());
        m.addAttribute("seccionActiva", "emergencias");
        m.addAttribute("navItems", switch (p.getUsuario().getRol()) {
            case TUTOR -> DashboardController.navTutor();
            case VETERINARIO -> DashboardController.navVeterinario();
            case RECEPCIONISTA -> DashboardController.navRecepcionista();
        });
    }
}
