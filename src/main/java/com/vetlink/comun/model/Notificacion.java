package com.vetlink.comun.model;

/** Ítem de la campana de notificaciones del dashboard. claseColor usa las mismas
 *  clases vl-badge-* ya definidas en app.css (ok/warn/danger/info). */
public record Notificacion(String icono, String claseColor, String texto) {}
