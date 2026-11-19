package com.vetlink.pasaporte.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Cliente HTTP hacia el servicio de pasaportes (FastAPI), configurado con vetlink.docs.*. */
@Configuration
public class DocsServiceConfig {

    public static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(2);
    public static final String ENCABEZADO_TOKEN = "X-Internal-Token";

    @Bean
    public RestClient pasaporteRestClient(@Value("${vetlink.docs.base-url}") String baseUrl,
                                          @Value("${vetlink.docs.internal-token}") String internalToken) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIEMPO_MAXIMO);
        fabrica.setReadTimeout(TIEMPO_MAXIMO);
        return RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(baseUrl)
                .defaultHeader(ENCABEZADO_TOKEN, internalToken)
                .build();
    }
}
