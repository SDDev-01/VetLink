package com.vetlink.mascota.repository;

import com.vetlink.mascota.model.TransferenciaMascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferenciaMascotaRepository extends JpaRepository<TransferenciaMascota, Integer> {

    Optional<TransferenciaMascota> findFirstByMascota_IdMascotaAndEstado(Integer idMascota,
                                                                         TransferenciaMascota.Estado estado);

    List<TransferenciaMascota> findByTutorDestino_IdUsuarioAndEstadoOrderByCreadaEnDesc(Integer idTutor,
                                                                                      TransferenciaMascota.Estado estado);

    List<TransferenciaMascota> findByTutorOrigen_IdUsuarioOrderByCreadaEnDesc(Integer idTutor);

    List<TransferenciaMascota> findByTutorDestino_IdUsuarioOrderByCreadaEnDesc(Integer idTutor);

    List<TransferenciaMascota> findByMascota_IdMascotaOrderByCreadaEnDesc(Integer idMascota);
}
