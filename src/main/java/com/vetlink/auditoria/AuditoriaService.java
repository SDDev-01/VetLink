package com.vetlink.auditoria;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Bitácora de acciones sensibles. Se escribe en la misma transacción de la operación:
 * si la operación se revierte, su rastro también (no quedan registros de cosas que no pasaron).
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository repo;
    private final Clock reloj;

    public AuditoriaService(AuditoriaRepository repo, Clock reloj) {
        this.repo = repo;
        this.reloj = reloj;
    }

    public void registrar(Integer idUsuario, String accion, String entidad, Object idEntidad, String detalle) {
        Auditoria a = new Auditoria();
        a.setIdUsuario(idUsuario);
        a.setAccion(recortar(accion, 60));
        a.setEntidad(recortar(entidad, 40));
        a.setIdEntidad(idEntidad == null ? null : recortar(String.valueOf(idEntidad), 40));
        a.setDetalle(recortar(detalle, 1000));
        a.setIp(ipActual());
        a.setFecha(LocalDateTime.now(reloj));
        repo.save(a);
    }

    private static String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getRemoteAddr();
            return ip == null ? null : recortar(ip, 45);
        }
        return null;
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
