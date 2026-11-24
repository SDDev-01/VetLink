package com.vetlink.usuario.repository;

import com.vetlink.usuario.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Integer> {

    /** VL-06: un dueño por número de documento exacto. */
    Optional<Tutor> findByUsuario_NumeroDocumento(String numeroDocumento);

    /** VL-06: un dueño por correo exacto (sin distinguir mayúsculas); el correo vive en la tabla `correo`. */
    @Query("""
           select distinct t from Tutor t, Correo c
           where c.usuario = t.usuario
             and lower(c.correo) = lower(:correo)
           """)
    Optional<Tutor> buscarPorCorreo(@Param("correo") String correo);

    /** VL-06: búsqueda de recepción: el texto puede ser un documento o un correo. */
    default Optional<Tutor> buscarPorDocumentoOCorreo(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String limpio = texto.trim();
        return limpio.contains("@") ? buscarPorCorreo(limpio) : findByUsuario_NumeroDocumento(limpio);
    }
}
