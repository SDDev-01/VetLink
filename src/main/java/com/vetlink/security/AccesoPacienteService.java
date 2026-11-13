package com.vetlink.security;

import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * VL-03: regla única de "quién puede ver los datos clínicos de una mascota".
 * - TUTOR: solo sus propias mascotas.
 * - VETERINARIO: mascotas con las que tiene o tuvo alguna cita.
 * - RECEPCIONISTA: mascotas que tienen o tuvieron cita en su clínica.
 */
@Service
public class AccesoPacienteService {

    private final CitaRepository citaRepository;

    public AccesoPacienteService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    public void verificar(Mascota mascota, VetlinkUserPrincipal principal) {
        Usuario usuario = principal.getUsuario();
        Rol rol = usuario.getRol();
        if (rol == null) {
            throw new AccessDeniedException("No tienes permiso para consultar esta mascota");
        }
        boolean permitido = switch (rol) {
            case TUTOR -> mascota.getTutor().getIdUsuario().equals(usuario.getIdUsuario());
            case VETERINARIO -> citaRepository.existsByMascota_IdMascotaAndVeterinario_IdUsuario(
                    mascota.getIdMascota(), usuario.getIdUsuario());
            case RECEPCIONISTA -> citaRepository.existsByMascota_IdMascotaAndVeterinaria_IdVeterinaria(
                    mascota.getIdMascota(), usuario.getRecepcionista().getVeterinaria().getIdVeterinaria());
        };
        if (!permitido) {
            throw new AccessDeniedException("No tienes permiso para consultar esta mascota");
        }
    }
}
