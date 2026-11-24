package com.vetlink.historial.service;

import com.vetlink.comun.config.ReglasNegocio;
import com.vetlink.historial.model.ArchivoHistorial;
import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.repository.ArchivoHistorialRepository;
import com.vetlink.usuario.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Adjuntos clínicos (PDF, JPG/JPEG, PNG). Se valida extensión, tamaño y firma real del
 * archivo (no solo el Content-Type del navegador). Se guardan con nombre aleatorio en
 * {uploads}/historial/{numeroHistorial}/{uuid}.{ext}; el nombre original solo se conserva
 * (saneado) para mostrarlo. Nunca se sirven por URL estática: siempre pasan por un
 * controlador con verificación de acceso.
 */
@Service
public class ArchivoClinicoService {

    private final ArchivoHistorialRepository repo;
    private final ReglasNegocio reglas;
    private final Clock reloj;
    private final Path carpetaBase;

    public ArchivoClinicoService(ArchivoHistorialRepository repo, ReglasNegocio reglas, Clock reloj,
                                 @Value("${vetlink.uploads.dir:uploads}") String uploadsDir) {
        this.repo = repo;
        this.reglas = reglas;
        this.reloj = reloj;
        this.carpetaBase = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    /** Devuelve la extensión normalizada ("pdf", "jpg", "png") o lanza IllegalArgumentException. */
    public String validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo");
        }
        if (archivo.getSize() > reglas.tamanoMaximoAdjuntoBytes()) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de " + reglas.tamanoMaximoAdjuntoMb() + " MB");
        }
        String original = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!reglas.formatosAdjuntos().contains(ext)) {
            throw new IllegalArgumentException("El formato del archivo no está permitido");
        }
        String real = firma(archivo).orElseThrow(() -> new IllegalArgumentException("El formato del archivo no está permitido"));
        boolean coincide = switch (real) {
            case "pdf" -> ext.equals("pdf");
            case "png" -> ext.equals("png");
            case "jpg" -> ext.equals("jpg") || ext.equals("jpeg");
            default -> false;
        };
        if (!coincide) {
            throw new IllegalArgumentException("El contenido del archivo no corresponde a su extensión");
        }
        return real;
    }

    @Transactional
    public ArchivoHistorial guardar(HistorialClinico historial, MultipartFile archivo, Usuario autor) {
        String ext = validar(archivo);
        String relativa = "historial/" + historial.getNumeroHistorial() + "/" + UUID.randomUUID() + "." + ext;
        Path destino = resolver(relativa);
        try {
            Files.createDirectories(destino.getParent());
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
        ArchivoHistorial a = new ArchivoHistorial();
        a.setHistorial(historial);
        a.setNombreArchivo(nombreSeguro(archivo.getOriginalFilename(), ext));
        a.setRutaArchivo(relativa);
        a.setTipoArchivo(switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            default -> "image/jpeg";
        });
        a.setTamanoBytes(archivo.getSize());
        a.setSubidoEn(LocalDateTime.now(reloj));
        a.setSubidoPor(autor);
        return repo.save(a);
    }

    /**
     * Archivo en disco. Las rutas nuevas son relativas a la carpeta de uploads; las antiguas
     * empezaban por "uploads/" (relativas al directorio de trabajo). En ambos casos se exige
     * que la ruta final quede dentro de una carpeta permitida.
     */
    public Optional<Path> archivoEnDisco(ArchivoHistorial a) {
        String ruta = a.getRutaArchivo();
        if (ruta == null || ruta.isBlank()) return Optional.empty();
        Path p;
        if (ruta.startsWith("uploads/")) {
            Path legado = Path.of("uploads").toAbsolutePath().normalize();
            p = Path.of(ruta).toAbsolutePath().normalize();
            if (!p.startsWith(legado)) return Optional.empty();
        } else {
            try {
                p = resolver(ruta);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }

    private Path resolver(String relativa) {
        Path p = carpetaBase.resolve(relativa).normalize();
        if (!p.startsWith(carpetaBase)) throw new IllegalArgumentException("Ruta de archivo inválida");
        return p;
    }

    static String nombreSeguro(String original, String ext) {
        String base = original == null ? "adjunto" : Path.of(original.replace('\\', '/')).getFileName().toString();
        base = base.replaceAll("[^A-Za-z0-9._ áéíóúÁÉÍÓÚñÑ-]", "_").trim();
        if (base.isBlank() || base.startsWith(".")) base = "adjunto." + ext;
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }

    private static Optional<String> firma(MultipartFile archivo) {
        byte[] b = new byte[8];
        int n;
        try (InputStream in = archivo.getInputStream()) {
            n = in.readNBytes(b, 0, b.length);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (n >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return Optional.of("pdf");
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return Optional.of("png");
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return Optional.of("jpg");
        return Optional.empty();
    }
}
