package com.vetlink.comun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Hora del servidor como única fuente de verdad para reglas de tiempo (ventana de atención,
 * horas pasadas, expiraciones). Se inyecta como {@link Clock} para poder fijarla en pruebas.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(ReglasNegocio reglas) {
        return Clock.system(reglas.zona());
    }
}
