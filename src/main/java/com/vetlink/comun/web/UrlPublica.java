package com.vetlink.comun.web;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Comprueba si la URL pública (la que va dentro del QR y de los enlaces compartidos) puede
 * abrirse desde Internet. La misma regla existe en FastAPI (app/config.py).
 */
public final class UrlPublica {

    private UrlPublica() {
    }

    /**
     * DUE-04: URL base de enlaces y QR. Misma regla que FastAPI (app/config.py, resolver_url_publica):
     * con IP definida y una URL pública vacía o de localhost se usa http://IP[:puerto]; una URL
     * pública real (dominio o túnel) tiene prioridad; sin nada, http://localhost.
     */
    public static String resolver(String publicBaseUrl, String ip, String puerto) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.strip().replaceAll("/+$", "");
        String laIp = ip == null ? "" : ip.strip();
        URI uri = null;
        try {
            if (!base.isEmpty()) uri = URI.create(base);
        } catch (IllegalArgumentException ignorada) {
            uri = null;
        }
        String hostBase = uri == null || uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean baseLocal = base.isEmpty() || List.of("localhost", "127.0.0.1", "0.0.0.0", "[::1]", "::1").contains(hostBase);
        if (!laIp.isEmpty() && laIp.matches("^[A-Za-z0-9.:-]+$") && baseLocal) {
            String esquema = uri != null && "https".equalsIgnoreCase(uri.getScheme()) ? "https" : "http";
            String p = uri != null && uri.getPort() > 0 ? String.valueOf(uri.getPort()) : (puerto == null ? "" : puerto.strip());
            String defecto = "https".equals(esquema) ? "443" : "80";
            String host = laIp.contains(":") ? "[" + laIp + "]" : laIp;
            return esquema + "://" + host + (p.isEmpty() || p.equals(defecto) ? "" : ":" + p);
        }
        return base.isEmpty() ? "http://localhost" : base;
    }

    public static List<String> problemas(String url) {
        List<String> r = new ArrayList<>();
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            r.add("La URL pública configurada no es válida.");
            return r;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.isEmpty()) {
            r.add("La URL pública configurada no es válida.");
            return r;
        }
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local") || host.equals("0.0.0.0")) {
            r.add("El QR apunta a «" + host + "», que solo existe en el equipo del servidor.");
        } else if (host.matches("^[0-9.]+$") || host.contains(":")) {
            try {
                InetAddress ip = InetAddress.getByName(host);
                if (ip.isSiteLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress()) {
                    r.add("El QR apunta a una IP privada (" + host + "): solo abrirá dentro de la misma red Wi-Fi.");
                }
            } catch (Exception ignorada) {
                // host numérico inválido: lo reportará el navegador
            }
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            r.add("La URL no usa HTTPS; en producción el enlace debe ser seguro.");
        }
        return r;
    }
}
