package com.vetlink.chat.service;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.usuario.model.Rol;
import com.vetlink.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VL-07: a quién puede escribirle cada usuario en el chat.
 * - TUTOR: al personal (veterinarios y recepcionistas) de las clínicas donde alguna
 *   de sus mascotas tiene o tuvo una cita no cancelada.
 * - RECEPCIONISTA: a los dueños de mascotas con cita en su clínica y al personal de su clínica.
 * - VETERINARIO: a los dueños de mascotas que tienen o tuvieron cita con él y al personal de su clínica.
 * Nunca a usuarios sin rol, inactivos (estado = 0) ni a sí mismo.
 */
@Service
@Transactional(readOnly = true)
public class ContactosChatService {

    private final EntityManager em;

    public ContactosChatService(EntityManager em) {
        this.em = em;
    }

    public List<Usuario> contactosPermitidos(Usuario yo) {
        Rol rol = yo.getRol();
        if (rol == null) {
            return List.of();
        }
        Map<Integer, Usuario> contactos = new LinkedHashMap<>();
        switch (rol) {
            case TUTOR -> agregar(contactos, personalDe(clinicasDelDuenio(yo.getIdUsuario()), yo.getIdUsuario()));
            case RECEPCIONISTA -> {
                Integer clinica = yo.getRecepcionista().getVeterinaria().getIdVeterinaria();
                agregar(contactos, dueniosConCitaEnClinica(clinica, yo.getIdUsuario()));
                agregar(contactos, personalDe(List.of(clinica), yo.getIdUsuario()));
            }
            case VETERINARIO -> {
                agregar(contactos, dueniosConCitaConVeterinario(yo.getIdUsuario()));
                if (yo.getVeterinario().getVeterinaria() != null) {
                    agregar(contactos, personalDe(
                            List.of(yo.getVeterinario().getVeterinaria().getIdVeterinaria()), yo.getIdUsuario()));
                }
            }
        }
        List<Usuario> lista = new ArrayList<>(contactos.values());
        lista.sort(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Usuario::getApellido, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    public boolean puedeEscribir(Usuario yo, Usuario otro) {
        if (otro == null || otro.getIdUsuario().equals(yo.getIdUsuario())) {
            return false;
        }
        return contactosPermitidos(yo).stream().anyMatch(u -> u.getIdUsuario().equals(otro.getIdUsuario()));
    }

    // ------------------------------------------------------------------
    // Consultas JPQL
    // ------------------------------------------------------------------

    private List<Integer> clinicasDelDuenio(Integer idDuenio) {
        return em.createQuery("""
                        select distinct c.veterinaria.idVeterinaria from Cita c
                        where c.mascota.tutor.idUsuario = :yo
                          and c.estado <> :cancelada
                        """, Integer.class)
                .setParameter("yo", idDuenio)
                .setParameter("cancelada", EstadoCita.CANCELADA)
                .getResultList();
    }

    /** Veterinarios y recepcionistas activos de las clínicas dadas. */
    private List<Usuario> personalDe(List<Integer> clinicas, Integer yo) {
        if (clinicas.isEmpty()) {
            return List.of();
        }
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

    private List<Usuario> dueniosConCitaConVeterinario(Integer idVeterinario) {
        return em.createQuery("""
                        select distinct u from Cita c join c.mascota m join m.tutor u
                        where c.veterinario.idUsuario = :yo
                          and u.estado = 1 and u.idUsuario <> :yo
                          and exists (select 1 from Tutor t where t.usuario = u)
                        """, Usuario.class)
                .setParameter("yo", idVeterinario)
                .getResultList();
    }

    private static void agregar(Map<Integer, Usuario> destino, List<Usuario> usuarios) {
        usuarios.forEach(u -> destino.putIfAbsent(u.getIdUsuario(), u));
    }
}
