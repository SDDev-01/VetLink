package com.vetlink.comun.model;

/** Item del sidebar del dashboard: clave de seccion, URL destino, icono FontAwesome y texto. */
public record NavItem(String key, String url, String icono, String label) {}
