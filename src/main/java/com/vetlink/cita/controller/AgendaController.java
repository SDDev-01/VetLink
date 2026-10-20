package com.vetlink.cita.controller;

import com.vetlink.cita.dto.CitaCalendarDTO;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.historial.service.HistorialClinicoService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.controller.DashboardController;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Agenda clínica: el alcance se calcula con el usuario de sesión, nunca con filtros del navegador. */
@Controller @RequestMapping("/agenda")
public class AgendaController {
    private final CitaRepository citas; private final HistorialClinicoService historiales;
    public AgendaController(CitaRepository citas, HistorialClinicoService historiales) { this.citas=citas; this.historiales=historiales; }
    @GetMapping public String agenda(@AuthenticationPrincipal VetlinkUserPrincipal p, Model m) {
        List<Cita> lista = citasPara(p); shell(m,p); m.addAttribute("citas",lista);
        m.addAttribute("citasJson", lista.stream().map(c -> new CitaCalendarDTO(c.getIdCita(),c.getFecha().toString(),c.getHora().toString(),c.getEstado().name(),c.getMascota()==null?"Cupo disponible":c.getMascota().getNombre(),c.getServicio().getNombreServicio())).toList());
        return "citas/agenda";
    }
    @GetMapping("/{id}") public String detalle(@PathVariable Integer id,@AuthenticationPrincipal VetlinkUserPrincipal p,Model m){Cita c=autorizada(id,p);shell(m,p);m.addAttribute("cita",c);m.addAttribute("historial",c.getMascota()==null?List.of():historiales.listarPorMascota(c.getMascota().getIdMascota()));return "citas/detalle-profesional";}
    @GetMapping("/{id}/atender") public String atenderForm(@PathVariable Integer id,@AuthenticationPrincipal VetlinkUserPrincipal p,Model m){Cita c=autorizadaVeterinario(id,p); if(c.getEstado()!=EstadoCita.AGENDADA)throw new IllegalStateException("Solo se atienden citas agendadas");shell(m,p);m.addAttribute("cita",c);m.addAttribute("historialPrevio",historiales.listarPorMascota(c.getMascota().getIdMascota()));HistorialClinicoForm f=new HistorialClinicoForm();f.setIdMascota(c.getMascota().getIdMascota());m.addAttribute("form",f);return "historial/atender-cita";}
    @PostMapping("/{id}/atender") public String atender(@PathVariable Integer id,@AuthenticationPrincipal VetlinkUserPrincipal p,@Valid @ModelAttribute("form") HistorialClinicoForm f,BindingResult r,Model m){Cita c=autorizadaVeterinario(id,p);if(!c.getMascota().getIdMascota().equals(f.getIdMascota()))throw new org.springframework.security.access.AccessDeniedException("La mascota no coincide con la cita");if(r.hasErrors())return atenderForm(id,p,m);historiales.crear(f,p.getUsuario().getIdUsuario());c.setEstado(EstadoCita.ATENDIDA);citas.save(c);return "redirect:/agenda/"+id+"?atendida";}
    private List<Cita> citasPara(VetlinkUserPrincipal p){return switch(p.getUsuario().getRol()){case VETERINARIO->citas.findByVeterinario_IdUsuarioOrderByFechaDesc(p.getUsuario().getIdUsuario());case RECEPCIONISTA->citas.buscarConFiltros(p.getUsuario().getRecepcionista().getVeterinaria().getIdVeterinaria(),null,null,null,null);default->throw new org.springframework.security.access.AccessDeniedException("Agenda no disponible");};}
    private Cita autorizada(Integer id,VetlinkUserPrincipal p){Cita c=citas.findById(id).orElseThrow();if(!citasPara(p).stream().anyMatch(x->x.getIdCita().equals(id)))throw new org.springframework.security.access.AccessDeniedException("No tienes acceso a esta cita");return c;}
    private Cita autorizadaVeterinario(Integer id,VetlinkUserPrincipal p){if(p.getUsuario().getRol()!=com.vetlink.usuario.model.Rol.VETERINARIO)throw new org.springframework.security.access.AccessDeniedException("Solo un veterinario puede atender citas");return autorizada(id,p);}
    private void shell(Model m,VetlinkUserPrincipal p){m.addAttribute("usuario",p.getUsuario());m.addAttribute("seccionActiva","citas");m.addAttribute("navItems",p.getUsuario().getRol()==com.vetlink.usuario.model.Rol.VETERINARIO?DashboardController.navVeterinario():DashboardController.navRecepcionista());}
}
