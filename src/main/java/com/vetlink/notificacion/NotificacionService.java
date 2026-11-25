package com.vetlink.notificacion;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Envío de notificaciones a usuarios concretos.
 * Si llega un evento con una claveUnica que el usuario ya tiene, se reutiliza la misma fila
 * (se actualiza el texto y vuelve a quedar sin leer): nunca hay duplicados.
 */
@Service
public class NotificacionService {

    private final NotificacionRepository repo;
    private final Clock reloj;

    public NotificacionService(NotificacionRepository repo, Clock reloj) {
        this.repo = repo;
        this.reloj = reloj;
    }

    @Transactional
    public void notificar(Integer idUsuario, String tipo, String titulo, String mensaje, String enlace, String claveUnica) {
        if (idUsuario == null) return;
        NotificacionUsuario n = claveUnica == null ? new NotificacionUsuario()
                : repo.findByIdUsuarioAndClaveUnica(idUsuario, claveUnica).orElseGet(NotificacionUsuario::new);
        n.setIdUsuario(idUsuario);
        n.setTipo(recortar(tipo, 40));
        n.setTitulo(recortar(titulo, 150));
        n.setMensaje(recortar(mensaje, 500));
        n.setEnlace(recortar(enlace, 300));
        n.setClaveUnica(recortar(claveUnica, 150));
        n.setLeida(false);
        n.setCreadaEn(LocalDateTime.now(reloj));
        repo.save(n);
    }

    @Transactional
    public void notificarVarios(Collection<Integer> usuarios, String tipo, String titulo, String mensaje, String enlace,
                                String claveUnica) {
        usuarios.stream().distinct().forEach(u -> notificar(u, tipo, titulo, mensaje, enlace, claveUnica));
    }

    /** Solo si no existe ya (recordatorios): no reabre una notificación leída. */
    @Transactional
    public boolean notificarUnaVez(Integer idUsuario, String tipo, String titulo, String mensaje, String enlace,
                                   String claveUnica) {
        if (repo.findByIdUsuarioAndClaveUnica(idUsuario, claveUnica).isPresent()) return false;
        notificar(idUsuario, tipo, titulo, mensaje, enlace, claveUnica);
        return true;
    }

    @Transactional(readOnly = true)
    public List<NotificacionUsuario> recientes(Integer idUsuario, int limite) {
        return repo.findByIdUsuarioOrderByCreadaEnDesc(idUsuario, PageRequest.of(0, limite));
    }

    @Transactional(readOnly = true)
    public long noLeidas(Integer idUsuario) {
        return repo.countByIdUsuarioAndLeidaFalse(idUsuario);
    }

    /** Marca como leída y devuelve el enlace de destino. Solo el destinatario puede hacerlo. */
    @Transactional
    public String abrir(Long idNotificacion, Integer idUsuario) {
        NotificacionUsuario n = repo.findById(idNotificacion)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));
        if (!n.getIdUsuario().equals(idUsuario)) {
            throw new AccessDeniedException("Esta notificación no es tuya");
        }
        n.setLeida(true);
        return n.getEnlace() == null || !n.getEnlace().startsWith("/") ? "/dashboard" : n.getEnlace();
    }

    @Transactional
    public void marcarTodasLeidas(Integer idUsuario) {
        repo.marcarTodasLeidas(idUsuario);
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
