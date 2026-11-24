package com.vetlink.chat.service;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * A quién puede escribirle cada usuario (se valida en el backend al abrir y al leer):
 * <ul>
 *   <li>TUTOR: a las veterinarias activas (el canal lo atiende la recepción de la clínica) y a
 *       los veterinarios que han atendido a sus mascotas. Nunca a otros tutores.</li>
 *   <li>VETERINARIO: solo a los dueños de pacientes que ha atendido (cita en atención o atendida).</li>
 *   <li>RECEPCIONISTA: a los dueños con citas en su clínica y al personal de su clínica.</li>
 * </ul>
 * Nunca a usuarios sin rol, inactivos ni a sí mismo.
 */
@Service
@Transactional(readOnly = true)
public class ContactosChatService {

    private static final Set<EstadoCita> ATENDIDOS = EnumSet.of(EstadoCita.EN_ATENCION, EstadoCita.ATENDIDA);

    private final EntityManager em;

    public ContactosChatService(EntityManager em) {
        this.em = em;
    }

    /** Contactos personales que se ofrecen para iniciar conversación (para el tutor ver {@link #veterinariasParaTutor}). */
    public List<Usuario> contactosPermitidos(Usuario yo) {
        Rol rol = yo.getRol();
        if (rol == null) return List.of();
        Map<Integer, Usuario> contactos = new LinkedHashMap<>();
        switch (rol) {
            case TUTOR -> agregar(contactos, veterinariosQueAtendieron(yo.getIdUsuario()));
            case RECEPCIONISTA -> {
                Integer clinica = yo.getRecepcionista().getVeterinaria().getIdVeterinaria();
                agregar(contactos, dueniosConCitaEnClinica(clinica, yo.getIdUsuario()));
                agregar(contactos, personalDe(List.of(clinica), yo.getIdUsuario()));
            }
            case VETERINARIO -> agregar(contactos, dueniosAtendidosPor(yo.getIdUsuario()));
        }
        List<Usuario> lista = new ArrayList<>(contactos.values());
        lista.sort(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Usuario::getApellido, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    public boolean puedeEscribir(Usuario yo, Usuario otro) {
        if (otro == null || otro.getIdUsuario().equals(yo.getIdUsuario()) || !otro.isActivo() || otro.getRol() == null) {
            return false;
        }
        if (yo.getRol() == Rol.TUTOR && otro.getRol() == Rol.RECEPCIONISTA) {
            Veterinaria v = otro.getRecepcionista().getVeterinaria();
            return v != null && v.isActiva();
        }
        return contactosPermitidos(yo).stream().anyMatch(u -> u.getIdUsuario().equals(otro.getIdUsuario()));
    }

    public List<Veterinaria> veterinariasParaTutor() {
        return em.createQuery("""
                        select v from Veterinaria v where v.estado = 1
                          and exists (select 1 from Recepcionista r where r.veterinaria = v and r.usuario.estado = 1)
                        order by v.nombre
                        """, Veterinaria.class)
                .getResultList();
    }

    /** Quién atiende el canal de una clínica: la recepcionista principal activa (o cualquier recepcionista activa). */
    public Optional<Usuario> responsableDeClinica(Integer idVeterinaria) {
        return em.createQuery("""
                        select r.usuario from Recepcionista r
                        where r.veterinaria.idVeterinaria = :v and r.veterinaria.estado = 1 and r.usuario.estado = 1
                        order by r.esPrincipal desc, r.idUsuario asc
                        """, Usuario.class)
                .setParameter("v", idVeterinaria)
                .setMaxResults(1)
                .getResultStream().findFirst();
    }

    // ------------------------------------------------------------------
    // Consultas JPQL
    // ------------------------------------------------------------------

    private List<Usuario> personalDe(List<Integer> clinicas, Integer yo) {
        if (clinicas.isEmpty()) return List.of();
        return em.createQuery("""
                        select u from Usuario u
                        where u.estado = 1 and u.idUsuario <> :yo
                          and (exists (select 1 from Veterinario v
                                       where v.usuario = u and v.veterinaria.idVeterinaria in :clinicas)
                               or exists (select 1 from Recepcionista r
                                          where r.usuario = u and r.veterinaria.idVeterinaria in :clinicas))
                        """, Usuario.class)
                .setParameter("yo", yo)
                .setParameter("clinicas", clinicas)
                .getResultList();
    }

    private List<Usuario> veterinariosQueAtendieron(Integer idTutor) {
        return em.createQuery("""
                        select distinct c.veterinario from Cita c
                        where c.mascota.tutor.idUsuario = :yo and c.estado in :estados and c.veterinario.estado = 1
                        """, Usuario.class)
                .setParameter("yo", idTutor)
                .setParameter("estados", ATENDIDOS)
                .getResultList();
    }

    private List<Usuario> dueniosConCitaEnClinica(Integer idClinica, Integer yo) {
        return em.createQuery("""
                        select distinct u from Cita c join c.mascota m join m.tutor u
                        where c.veterinaria.idVeterinaria = :clinica
                          and u.estado = 1 and u.idUsuario <> :yo
                          and exists (select 1 from Tutor t where t.usuario = u)
                        """, Usuario.class)
                .setParameter("clinica", idClinica)
                .setParameter("yo", yo)
                .getResultList();
    }

    private List<Usuario> dueniosAtendidosPor(Integer idVeterinario) {
        return em.createQuery("""
                        select distinct u from Cita c join c.mascota m join m.tutor u
                        where c.veterinario.idUsuario = :yo and c.estado in :estados
                          and u.estado = 1 and u.idUsuario <> :yo
                          and exists (select 1 from Tutor t where t.usuario = u)
                        """, Usuario.class)
                .setParameter("yo", idVeterinario)
                .setParameter("estados", ATENDIDOS)
                .getResultList();
    }

    private static void agregar(Map<Integer, Usuario> destino, List<Usuario> usuarios) {
        usuarios.forEach(u -> destino.putIfAbsent(u.getIdUsuario(), u));
    }
}
