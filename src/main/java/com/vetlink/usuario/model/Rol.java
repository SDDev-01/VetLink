package com.vetlink.usuario.model;

/**
 * Rol efectivo de un Usuario dentro del sistema. No es una columna de la
 * tabla `usuario` (el SQL lo modela normalizado en tutor/veterinario/
 * recepcionista); se calcula a partir de cual de esas tres subtablas tiene
 * asociada el usuario. Se usa para las autoridades de Spring Security y
 * para decidir que dashboard/menu se muestra en Thymeleaf.
 */
public enum Rol {
    TUTOR,
    VETERINARIO,
    RECEPCIONISTA
}
