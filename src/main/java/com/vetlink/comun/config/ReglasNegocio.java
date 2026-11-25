package com.vetlink.comun.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;
import java.util.List;

/**
 * Reglas de negocio configurables en un solo lugar (prefijo {@code vetlink.reglas}).
 * Los valores por defecto están documentados en REGLAS_NEGOCIO.md y se pueden cambiar
 * en application.properties o con variables de entorno (p. ej. VETLINK_REGLAS_MARGENINICIOATENCIONMIN).
 *
 * @param duracionServicioPorDefectoMin  duración usada cuando la clínica no definió la del servicio.
 * @param anticipacionMinimaReservaMin   cuánto antes del inicio se puede reservar un cupo como mínimo.
 * @param anticipacionMinimaLiberacionMin si una cita se cancela con menos anticipación, el cupo no se vuelve a ofrecer.
 * @param margenInicioAtencionMin        minutos después de la hora programada durante los que se puede iniciar la atención.
 * @param expiracionPendienteHoras       horas que una reserva puede seguir pendiente de confirmación.
 * @param confirmacionAntesDelInicioMin  una reserva pendiente expira, a más tardar, estos minutos antes del inicio.
 * @param horizonteAgendaDias            días hacia adelante para los que se generan cupos automáticamente.
 * @param recordatorioHorasAntes         con cuánta antelación se envía el recordatorio de una cita confirmada.
 * @param tamanoMaximoAdjuntoMb          tamaño máximo de un adjunto clínico.
 * @param tamanoMaximoImagenMb           tamaño máximo de imágenes (logo de veterinaria).
 * @param formatosAdjuntos               extensiones permitidas para adjuntos clínicos.
 * @param zonaHoraria                    zona horaria de negocio (la del servidor debe coincidir).
 * @param publicBaseUrl                  URL pública con la que se construyen los enlaces y QR.
 * @param tareasHabilitadas              activa las tareas programadas (generación, expiración, recordatorios).
 */
@ConfigurationProperties(prefix = "vetlink.reglas")
public record ReglasNegocio(
        Integer duracionServicioPorDefectoMin,
        Integer anticipacionMinimaReservaMin,
        Integer anticipacionMinimaLiberacionMin,
        Integer margenInicioAtencionMin,
        Integer expiracionPendienteHoras,
        Integer confirmacionAntesDelInicioMin,
        Integer horizonteAgendaDias,
        Integer recordatorioHorasAntes,
        Integer tamanoMaximoAdjuntoMb,
        Integer tamanoMaximoImagenMb,
        List<String> formatosAdjuntos,
        String zonaHoraria,
        String publicBaseUrl,
        Boolean tareasHabilitadas) {

    public ReglasNegocio {
        duracionServicioPorDefectoMin = positivo(duracionServicioPorDefectoMin, 30);
        anticipacionMinimaReservaMin = noNegativo(anticipacionMinimaReservaMin, 60);
        anticipacionMinimaLiberacionMin = noNegativo(anticipacionMinimaLiberacionMin, 120);
        margenInicioAtencionMin = positivo(margenInicioAtencionMin, 15);
        expiracionPendienteHoras = positivo(expiracionPendienteHoras, 24);
        confirmacionAntesDelInicioMin = noNegativo(confirmacionAntesDelInicioMin, 30);
        horizonteAgendaDias = positivo(horizonteAgendaDias, 14);
        recordatorioHorasAntes = positivo(recordatorioHorasAntes, 24);
        tamanoMaximoAdjuntoMb = positivo(tamanoMaximoAdjuntoMb, 10);
        tamanoMaximoImagenMb = positivo(tamanoMaximoImagenMb, 2);
        formatosAdjuntos = formatosAdjuntos == null || formatosAdjuntos.isEmpty()
                ? List.of("pdf", "jpg", "jpeg", "png") : List.copyOf(formatosAdjuntos);
        zonaHoraria = zonaHoraria == null || zonaHoraria.isBlank() ? "America/Bogota" : zonaHoraria;
        publicBaseUrl = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "http://localhost" : publicBaseUrl.replaceAll("/+$", "");
        tareasHabilitadas = tareasHabilitadas == null || tareasHabilitadas;
    }

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria);
    }

    public long tamanoMaximoAdjuntoBytes() {
        return tamanoMaximoAdjuntoMb * 1024L * 1024L;
    }

    public long tamanoMaximoImagenBytes() {
        return tamanoMaximoImagenMb * 1024L * 1024L;
    }

    private static Integer positivo(Integer v, int defecto) {
        return v == null || v <= 0 ? defecto : v;
    }

    private static Integer noNegativo(Integer v, int defecto) {
        return v == null || v < 0 ? defecto : v;
    }
}
