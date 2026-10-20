package com.vetlink.vacuna.service.impl;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import com.vetlink.vacuna.model.CarnetVacuna;
import com.vetlink.vacuna.model.LoteVacuna;
import com.vetlink.vacuna.model.Vacuna;
import com.vetlink.vacuna.repository.CarnetVacunaRepository;
import com.vetlink.vacuna.repository.LoteVacunaRepository;
import com.vetlink.vacuna.repository.VacunaRepository;
import com.vetlink.vacuna.service.CarnetVacunaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CarnetVacunaServiceImpl implements CarnetVacunaService {

    private final CarnetVacunaRepository carnetVacunaRepository;
    private final MascotaRepository mascotaRepository;
    private final VacunaRepository vacunaRepository;
    private final LoteVacunaRepository loteVacunaRepository;
    private final UsuarioRepository usuarioRepository;

    public CarnetVacunaServiceImpl(CarnetVacunaRepository carnetVacunaRepository, MascotaRepository mascotaRepository,
                                    VacunaRepository vacunaRepository, LoteVacunaRepository loteVacunaRepository,
                                    UsuarioRepository usuarioRepository) {
        this.carnetVacunaRepository = carnetVacunaRepository;
        this.mascotaRepository = mascotaRepository;
        this.vacunaRepository = vacunaRepository;
        this.loteVacunaRepository = loteVacunaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<CarnetVacuna> listarPorMascota(Integer idMascota) {
        return carnetVacunaRepository.findByMascota_IdMascotaOrderByFechaAplicacionDesc(idMascota);
    }

    @Override
    @Transactional
    public CarnetVacuna registrar(Integer idMascota, Integer idVacuna, Integer idLote, String dosis,
                                   LocalDate fechaAplicacion, LocalDate proximaDosis,
                                   String observaciones, Integer idUsuarioQueAplica) {
        Mascota mascota = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        Vacuna vacuna = vacunaRepository.findById(idVacuna)
                .orElseThrow(() -> new IllegalArgumentException("Vacuna no encontrada"));
        LoteVacuna lote = loteVacunaRepository.findById(idLote)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));
        Usuario usuario = usuarioRepository.findById(idUsuarioQueAplica)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (proximaDosis.isBefore(fechaAplicacion)) {
            throw new IllegalArgumentException("La próxima dosis no puede ser antes de la fecha de aplicación");
        }

        CarnetVacuna carnet = new CarnetVacuna();
        carnet.setMascota(mascota);
        carnet.setVacuna(vacuna);
        carnet.setLote(lote);
        carnet.setUsuario(usuario);
        carnet.setDosis(dosis);
        carnet.setFechaAplicacion(fechaAplicacion);
        carnet.setProximaDosis(proximaDosis);
        carnet.setObservaciones(observaciones);
        return carnetVacunaRepository.save(carnet);
    }
}
