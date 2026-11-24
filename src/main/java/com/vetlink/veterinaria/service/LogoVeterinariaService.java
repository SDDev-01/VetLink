package com.vetlink.veterinaria.service;

import com.vetlink.comun.config.ReglasNegocio;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.VeterinariaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Imagen real de la veterinaria (PNG o JPEG, tamaño máximo configurable). Se guarda en
 * {uploads}/veterinarias/{id}/{uuid}.{ext}; en la columna logo queda la ruta relativa.
 * Si no hay imagen subida se muestra /resource/veterinaria-default.svg.
 */
@Service
public class LogoVeterinariaService {

    private final VeterinariaRepository repo;
    private final ReglasNegocio reglas;
    private final Path carpetaBase;

    public LogoVeterinariaService(VeterinariaRepository repo, ReglasNegocio reglas,
                                  @Value("${vetlink.uploads.dir:uploads}") String uploadsDir) {
        this.repo = repo;
        this.reglas = reglas;
        this.carpetaBase = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    public String validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("Selecciona una imagen");
        if (archivo.getSize() > reglas.tamanoMaximoImagenBytes()) {
            throw new IllegalArgumentException("La imagen no puede pesar más de " + reglas.tamanoMaximoImagenMb() + " MB");
        }
        byte[] b = new byte[8];
        int n;
        try (InputStream in = archivo.getInputStream()) {
            n = in.readNBytes(b, 0, 8);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer la imagen");
        }
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        throw new IllegalArgumentException("El formato del archivo no está permitido (usa PNG o JPG)");
    }

    @Transactional
    public Veterinaria guardar(Veterinaria v, MultipartFile archivo) {
        String ext = validar(archivo);
        String relativa = "veterinarias/" + v.getIdVeterinaria() + "/" + UUID.randomUUID() + "." + ext;
        Path destino = resolver(relativa);
        try {
            Files.createDirectories(destino.getParent());
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
        v.setLogo(relativa);
        return repo.save(v);
    }

    public Optional<Path> archivo(Veterinaria v) {
        String logo = v.getLogo();
        if (logo == null || !logo.startsWith("veterinarias/")) return Optional.empty();
        try {
            Path p = resolver(logo);
            return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static MediaType tipo(Path p) {
        return p.getFileName().toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    private Path resolver(String relativa) {
        Path p = carpetaBase.resolve(relativa).normalize();
        if (!p.startsWith(carpetaBase)) throw new IllegalArgumentException("Ruta inválida");
        return p;
    }
}
