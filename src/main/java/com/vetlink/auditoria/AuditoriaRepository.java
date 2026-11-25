package com.vetlink.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByEntidadAndIdEntidadOrderByFechaDesc(String entidad, String idEntidad);
}
