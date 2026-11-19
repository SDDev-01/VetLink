package com.vetlink.pasaporte.integration;

import com.vetlink.pasaporte.model.DuracionPasaporte;
import com.vetlink.pasaporte.model.Pasaporte;
import com.vetlink.pasaporte.service.PasaporteNoEncontradoException;
import com.vetlink.pasaporte.service.PasaporteService;
import com.vetlink.pasaporte.service.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Adapter: implementa {@link PasaporteService} llamando por REST al servicio FastAPI (docs-service).
 * Traduce el dominio de VetLink a HTTP/JSON y los fallos de red a excepciones del dominio,
 * así los controladores no saben que existe FastAPI. Contrato: docs/contrato-documentos.md.
 */
@Service
public class FastApiPasaporteAdapter implements PasaporteService {

    private static final ParameterizedTypeReference<List<Pasaporte>> LISTA = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public FastApiPasaporteAdapter(@Qualifier("pasaporteRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Pasaporte crear(Integer idMascota, Integer idUsuario, DuracionPasaporte duracion) {
        return llamar(() -> restClient.post()
                .uri("/internal/pasaportes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("idMascota", idMascota, "idUsuario", idUsuario, "duracionHoras", duracion.getHoras()))
                .retrieve()
                .body(Pasaporte.class));
    }

    @Override
    public List<Pasaporte> listar(Integer idMascota) {
        List<Pasaporte> pasaportes = llamar(() -> restClient.get()
                .uri(uri -> uri.path("/internal/pasaportes").queryParam("idMascota", idMascota).build())
                .retrieve()
                .body(LISTA));
        return pasaportes == null ? List.of() : pasaportes;
    }

    @Override
    public void revocar(String token) {
        llamar(() -> restClient.post()
                .uri("/internal/pasaportes/{token}/revocar", token)
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T llamar(Supplier<T> peticion) {
        try {
            return peticion.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new PasaporteNoEncontradoException(e);
        } catch (RestClientException e) {
            // Red caída, tiempo agotado, 401 por token mal configurado, 5xx o JSON inesperado.
            throw new ServicioNoDisponibleException(e);
        }
    }
}
