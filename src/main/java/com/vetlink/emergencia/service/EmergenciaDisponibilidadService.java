package com.vetlink.emergencia.service;

import com.vetlink.disponibilidad.service.DisponibilidadService;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.model.Veterinario;
import com.vetlink.usuario.repository.VeterinarioRepository;
import com.vetlink.veterinaria.model.ServicioVeterinaria;
import com.vetlink.veterinaria.repository.ServicioVeterinariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Disponibilidad real para urgencias. Un veterinario está disponible si: está activo, el
 * momento actual está dentro de su horario aprobado, no tiene ausencias, no está atendiendo
 * otra cita (ni tiene una cita confirmada en curso) y no tiene otra emergencia asignada abierta.
 * Si la clínica autorizó explícitamente el servicio de urgencias a algunos veterinarios, solo
 * cuentan esos.
 */
@Service
@Transactional(readOnly = true)
public class EmergenciaDisponibilidadService {

    private final VeterinarioRepository veterinarios;
    private final EmergenciaRepository emergencias;
    private final DisponibilidadService disponibilidad;
    private final ServicioVeterinariaRepository serviciosClinica;

    public EmergenciaDisponibilidadService(VeterinarioRepository veterinarios, EmergenciaRepository emergencias,
                                           DisponibilidadService disponibilidad, ServicioVeterinariaRepository serviciosClinica) {
        this.veterinarios = veterinarios;
        this.emergencias = emergencias;
        this.disponibilidad = disponibilidad;
        this.serviciosClinica = serviciosClinica;
    }

    public Optional<Integer> servicioEmergencia(Integer idVeterinaria) {
        return serviciosClinica.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .map(ServicioVeterinaria::getServicio)
                .filter(s -> s.isEmergencia())
                .map(s -> s.getIdServicio())
                .findFirst();
    }

    public boolean ofreceEmergencias(Integer idVeterinaria) {
        return servicioEmergencia(idVeterinaria).isPresent();
    }

    public boolean disponibleAhora(Usuario vet, Integer idServicioEmergencia) {
        LocalDateTime ahora = disponibilidad.ahora();
        if (!vet.isActivo()) return false;
        if (idServicioEmergencia != null && disponibilidad.tieneServiciosAutorizados(vet.getIdUsuario())
                && !disponibilidad.autorizadoPara(vet.getIdUsuario(), idServicioEmergencia)) {
            return false;
        }
        if (!disponibilidad.disponibleEn(vet, ahora, ahora.plusMinutes(1), true)) return false;
        if (disponibilidad.ocupadoAhora(vet.getIdUsuario())) return false;
        return !emergencias.existsByVeterinario_IdUsuarioAndEstado(vet.getIdUsuario(), EstadoEmergencia.ASIGNADA);
    }

    public List<Usuario> disponiblesAhora(Integer idVeterinaria) {
        Integer servicio = servicioEmergencia(idVeterinaria).orElse(null);
        return veterinarios.findByVeterinaria_IdVeterinaria(idVeterinaria).stream()
                .map(Veterinario::getUsuario)
                .filter(u -> disponibleAhora(u, servicio))
                .toList();
    }
}
