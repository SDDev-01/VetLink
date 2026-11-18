package com.vetlink.mascota.service;

import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
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
 * VL-22: fotos de mascotas subidas como archivo (JPEG o PNG, máximo 5 MB).
 * Se guardan en {uploads}/mascotas/{idMascota}/{uuid}.{ext} y en la columna `foto`
 * queda la ruta relativa "mascotas/{idMascota}/{uuid}.{ext}". Las fotos antiguas
 * que son URL (http...) se siguen usando tal cual.
 */
@Service
public class FotoMascotaService {

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private final MascotaRepository mascotaRepository;
    private final Path carpetaBase;

    public FotoMascotaService(MascotaRepository mascotaRepository,
                              @Value("${vetlink.uploads.dir:uploads}") String uploadsDir) {
        this.mascotaRepository = mascotaRepository;
        this.carpetaBase = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    public static boolean hayArchivo(MultipartFile archivo) {
        return archivo != null && !archivo.isEmpty();
    }

    /** Valida tipo y tamaño. Revisa también la firma del archivo, no solo el Content-Type que manda el navegador. */
    public void validar(MultipartFile archivo) {
        if (!hayArchivo(archivo)) {
            return;
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("La foto no puede pesar más de 5 MB");
        }
        if (extension(archivo).isEmpty()) {
            throw new IllegalArgumentException("La foto debe ser una imagen JPG o PNG");
        }
    }

    @Transactional
    public void guardar(Mascota mascota, MultipartFile archivo) {
        validar(archivo);
        if (!hayArchivo(archivo)) {
            return;
        }
        String ext = extension(archivo).orElseThrow();
        String relativa = "mascotas/" + mascota.getIdMascota() + "/" + UUID.randomUUID() + "." + ext;
        Path destino = resolver(relativa);
        try {
            Files.createDirectories(destino.getParent());
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la foto", e);
        }
        mascota.setFoto(relativa);
        mascotaRepository.save(mascota);
    }

    /** Archivo local de la foto, si la mascota tiene una foto subida (no una URL) y el archivo existe. */
    public Optional<Path> archivoLocal(Mascota mascota) {
        String foto = mascota.getFoto();
        if (foto == null || foto.isBlank() || esUrl(foto)) {
            return Optional.empty();
        }
        Path path = resolver(foto);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public static boolean esUrl(String foto) {
        return foto != null && foto.regionMatches(true, 0, "http", 0, 4);
    }

    public static MediaType tipoDe(Path path) {
        return path.getFileName().toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    /** Resuelve la ruta dentro de la carpeta de uploads; nunca fuera de ella. */
    private Path resolver(String relativa) {
        Path path = carpetaBase.resolve(relativa).normalize();
        if (!path.startsWith(carpetaBase)) {
            throw new IllegalArgumentException("Ruta de foto inválida");
        }
        return path;
    }

    /** "png" o "jpg" según el Content-Type declarado y los primeros bytes del archivo. */
    private static Optional<String> extension(MultipartFile archivo) {
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        byte[] cabecera = new byte[8];
        int leidos;
        try (InputStream in = archivo.getInputStream()) {
            leidos = in.readNBytes(cabecera, 0, cabecera.length);
        } catch (IOException e) {
            return Optional.empty();
        }
        boolean firmaPng = leidos >= 8 && (cabecera[0] & 0xFF) == 0x89 && cabecera[1] == 'P'
                && cabecera[2] == 'N' && cabecera[3] == 'G';
        boolean firmaJpeg = leidos >= 3 && (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xFF) == 0xD8
                && (cabecera[2] & 0xFF) == 0xFF;
        if (tipo.equals("image/png") && firmaPng) {
            return Optional.of("png");
        }
        if ((tipo.equals("image/jpeg") || tipo.equals("image/jpg")) && firmaJpeg) {
            return Optional.of("jpg");
        }
        return Optional.empty();
    }
}
