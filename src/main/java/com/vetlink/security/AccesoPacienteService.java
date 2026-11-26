package com.vetlink.security;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Regla única de "quién puede ver qué" sobre una mascota. Se aplica en el backend en cada
 * consulta y descarga, nunca solo ocultando botones:
 * <ul>
 *   <li>TUTOR: solo sus mascotas actuales y vigentes (tras una transferencia, el anterior dueño pierde el
 *       acceso; las dadas de baja, estado 0, no se le muestran en ninguna vista).</li>
 *   <li>VETERINARIO: mascotas con una cita confirmada, en atención o atendida con él, o con una
 *       emergencia que tenga asignada.</li>
 *   <li>RECEPCIONISTA: datos básicos de mascotas con cualquier cita en su clínica; historia
 *       clínica solo si hay una cita autorizada (confirmada/en atención/atendida) en su clínica,
 *       y solo de los registros hechos en su clínica.</li>
 * </ul>
 */
@Service
public class AccesoPacienteService {

    private final CitaRepository citaRepository;
    private final EmergenciaRepository emergenciaRepository;

    public AccesoPacienteService(CitaRepository citaRepository, EmergenciaRepository emergenciaRepository) {
        this.citaRepository = citaRepository;
        this.emergenciaRepository = emergenciaRepository;
    }

    /** Acceso a la información clínica de la mascota (historial, carnet, adjuntos). */
    public void verificar(Mascota mascota, VetlinkUserPrincipal principal) {
        if (!puedeVerClinica(mascota, principal.getUsuario())) {
            throw new AccessDeniedException("No tienes permiso para consultar esta mascota");
        }
    }

    public boolean puedeVerClinica(Mascota mascota, Usuario usuario) {
        Rol rol = usuario.getRol();
        if (rol == null) return false;
        return switch (rol) {
            case TUTOR -> esDuenioVigente(mascota, usuario.getIdUsuario());
            case VETERINARIO -> citaRepository.existsByMascota_IdMascotaAndVeterinario_IdUsuarioAndEstadoIn(
                    mascota.getIdMascota(), usuario.getIdUsuario(), EstadoCita.RELACION_CLINICA)
                    || emergenciaRepository.existsByMascota_IdMascotaAndVeterinario_IdUsuario(
                    mascota.getIdMascota(), usuario.getIdUsuario());
            case RECEPCIONISTA -> citaRepository.existsByMascota_IdMascotaAndVeterinaria_IdVeterinariaAndEstadoIn(
                    mascota.getIdMascota(), clinicaDe(usuario), EstadoCita.RELACION_CLINICA);
        };
    }

    /** Datos básicos (ficha, foto, peso): la recepción los ve con cualquier cita en su clínica. */
    public void verificarDatosBasicos(Mascota mascota, VetlinkUserPrincipal principal) {
        Usuario u = principal.getUsuario();
        if (u.getRol() == Rol.RECEPCIONISTA) {
            if (!citaRepository.existsByMascota_IdMascotaAndVeterinaria_IdVeterinaria(mascota.getIdMascota(), clinicaDe(u))) {
                throw new AccessDeniedException("Esta mascota no es paciente de tu clínica");
            }
            return;
        }
        if (u.getRol() == Rol.VETERINARIO
                && citaRepository.existsByMascota_IdMascotaAndVeterinario_IdUsuario(mascota.getIdMascota(), u.getIdUsuario())) {
            return;
        }
        verificar(mascota, principal);
    }

    /** Un documento clínico concreto: acceso a la mascota y, para recepción, solo de su clínica. */
    public void verificarDocumento(HistorialClinico h, VetlinkUserPrincipal principal) {
        if (!puedeVerDocumento(h, principal.getUsuario())) {
            throw new AccessDeniedException("No tienes permiso para consultar este documento");
        }
    }

    public boolean puedeVerDocumento(HistorialClinico h, Usuario usuario) {
        if (!puedeVerClinica(h.getMascota(), usuario)) return false;
        if (usuario.getRol() == Rol.RECEPCIONISTA) {
            return h.getVeterinaria() != null && h.getVeterinaria().getIdVeterinaria().equals(clinicaDe(usuario));
        }
        return true;
    }

    /** Solo el dueño: para editar la mascota, transferirla o compartir su pasaporte de salud. */
    public void verificarPropietario(Mascota mascota, VetlinkUserPrincipal principal) {
        if (!esDuenioVigente(mascota, principal.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("No tienes permiso sobre esta mascota");
        }
    }

    /**
     * Regla única del dueño: la mascota es suya y su registro está vigente (estado 1).
     * Es la misma condición con la que «Mis mascotas» arma su lista, así que ninguna otra
     * vista ni URL directa le muestra al dueño una mascota que no aparece allí.
     */
    public static boolean esDuenioVigente(Mascota mascota, Integer idUsuario) {
        return mascota != null && mascota.isVigente() && mascota.getTutor() != null
                && mascota.getTutor().getIdUsuario().equals(idUsuario);
    }

    private static Integer clinicaDe(Usuario u) {
        if (u.getRecepcionista() == null || u.getRecepcionista().getVeterinaria() == null) {
            throw new AccessDeniedException("No tienes una veterinaria asociada");
        }
        return u.getRecepcionista().getVeterinaria().getIdVeterinaria();
    }
}
