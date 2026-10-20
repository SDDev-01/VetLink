package com.vetlink.veterinaria.service.impl;

import com.vetlink.usuario.model.Recepcionista;
import com.vetlink.usuario.repository.RecepcionistaRepository;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import com.vetlink.veterinaria.service.VeterinariaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VeterinariaServiceImpl implements VeterinariaService {

    private final VeterinariaRepository veterinariaRepository;
    private final RecepcionistaRepository recepcionistaRepository;

    public VeterinariaServiceImpl(VeterinariaRepository veterinariaRepository, RecepcionistaRepository recepcionistaRepository) {
        this.veterinariaRepository = veterinariaRepository;
        this.recepcionistaRepository = recepcionistaRepository;
    }

    @Override
    public List<Veterinaria> listarActivas() {
        return veterinariaRepository.findByEstado((byte) 1);
    }

    @Override
    public List<Veterinaria> buscarPorNombre(String nombre) {
        return veterinariaRepository.findByNombreContainingIgnoreCaseAndEstado(nombre, (byte) 1);
    }

    @Override
    public Veterinaria buscarPorId(Integer idVeterinaria) {
        return veterinariaRepository.findById(idVeterinaria)
                .orElseThrow(() -> new IllegalArgumentException("Veterinaria no encontrada"));
    }

    @Override
    @Transactional
    public void desactivar(Integer idVeterinaria, Integer idRecepcionista) {
        // NOTA: a diferencia del prototipo original, la tabla `recepcionista` de
        // mydb.sql no distingue quien registro la veterinaria. La regla aplicada
        // aqui es: cualquier recepcionista vinculado a esa veterinaria puede
        // administrarla.
        verificarPertenece(idVeterinaria, idRecepcionista);
        Veterinaria veterinaria = buscarPorId(idVeterinaria);
        veterinaria.setEstado((byte) 0);
        veterinariaRepository.save(veterinaria);
    }

    @Override
    @Transactional
    public Veterinaria actualizarInfo(Integer idVeterinaria, Integer idRecepcionista,
                                       String nombre, String descripcion, String paginaWeb, String logo) {
        verificarPertenece(idVeterinaria, idRecepcionista);
        Veterinaria veterinaria = buscarPorId(idVeterinaria);
        veterinaria.setNombre(nombre);
        veterinaria.setDescripcion(descripcion);
        veterinaria.setPaginaWeb(paginaWeb);
        if (logo != null && !logo.isBlank()) {
            veterinaria.setLogo(logo);
        }
        return veterinariaRepository.save(veterinaria);
    }

    private void verificarPertenece(Integer idVeterinaria, Integer idRecepcionista) {
        Recepcionista recepcionista = recepcionistaRepository.findById(idRecepcionista)
                .orElseThrow(() -> new SecurityException("No autorizado"));
        if (!recepcionista.getVeterinaria().getIdVeterinaria().equals(idVeterinaria)) {
            throw new SecurityException("No administras esta veterinaria");
        }
    }
}
