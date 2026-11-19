package com.vetlink.pasaporte;

import com.sun.net.httpserver.HttpServer;
import com.vetlink.pasaporte.config.DocsServiceConfig;
import com.vetlink.pasaporte.integration.FastApiPasaporteAdapter;
import com.vetlink.pasaporte.model.DuracionPasaporte;
import com.vetlink.pasaporte.model.Pasaporte;
import com.vetlink.pasaporte.service.PasaporteNoEncontradoException;
import com.vetlink.pasaporte.service.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Adapter hacia FastAPI probado con MockRestServiceServer: no necesita Python encendido. */
class FastApiPasaporteAdapterTest {

    private static final String BASE = "http://fastapi:8000";
    private static final String TOKEN_INTERNO = "token-de-pruebas";

    private MockRestServiceServer servidor;
    private FastApiPasaporteAdapter adapter;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE)
                .defaultHeader(DocsServiceConfig.ENCABEZADO_TOKEN, TOKEN_INTERNO);
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new FastApiPasaporteAdapter(builder.build());
    }

    @Test
    void crearEnviaElContratoYTraduceLaRespuesta() {
        servidor.expect(requestTo(BASE + "/internal/pasaportes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(DocsServiceConfig.ENCABEZADO_TOKEN, TOKEN_INTERNO))
                .andExpect(jsonPath("$.idMascota").value(2))
                .andExpect(jsonPath("$.idUsuario").value(8))
                .andExpect(jsonPath("$.duracionHoras").value(168))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"token": "abc123", "url": "http://localhost/p/abc123", "expiraEn": "2026-11-26T10:15:00"}
                        """));

        Pasaporte p = adapter.crear(2, 8, DuracionPasaporte.DIAS_7);

        assertThat(p.token()).isEqualTo("abc123");
        assertThat(p.url()).isEqualTo("http://localhost/p/abc123");
        assertThat(p.expiraEn()).isEqualTo(LocalDateTime.of(2026, 11, 26, 10, 15));
        servidor.verify();
    }

    @Test
    void listarTraduceLaLista() {
        servidor.expect(requestTo(BASE + "/internal/pasaportes?idMascota=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(DocsServiceConfig.ENCABEZADO_TOKEN, TOKEN_INTERNO))
                .andRespond(withSuccess("""
                        [{"token": "t1", "url": "http://localhost/p/t1", "creadoEn": "2026-11-19T10:15:00",
                          "expiraEn": "2026-11-20T10:15:00", "revocadoEn": null, "estado": "ACTIVO", "aperturas": 3},
                         {"token": "t0", "url": "http://localhost/p/t0", "creadoEn": "2026-11-01T08:00:00",
                          "expiraEn": "2026-11-02T08:00:00", "revocadoEn": "2026-11-01T09:00:00", "estado": "REVOCADO", "aperturas": 0}]
                        """, MediaType.APPLICATION_JSON));

        List<Pasaporte> lista = adapter.listar(2);

        assertThat(lista).extracting(Pasaporte::token).containsExactly("t1", "t0");
        assertThat(lista.get(0).activo()).isTrue();
        assertThat(lista.get(0).aperturas()).isEqualTo(3);
        assertThat(lista.get(1).revocadoEn()).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));
    }

    @Test
    void revocarLlamaALaRutaDelToken() {
        servidor.expect(requestTo(BASE + "/internal/pasaportes/t1/revocar"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        adapter.revocar("t1");

        servidor.verify();
    }

    @Test
    void un404SeTraduceAPasaporteNoEncontrado() {
        servidor.expect(requestTo(BASE + "/internal/pasaportes/inexistente/revocar"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.revocar("inexistente"))
                .isInstanceOf(PasaporteNoEncontradoException.class);
    }

    @Test
    void tiempoAgotadoSeTraduceAServicioNoDisponible() {
        servidor.expect(requestTo(BASE + "/internal/pasaportes?idMascota=2"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> adapter.listar(2))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage(ServicioNoDisponibleException.MENSAJE);
    }

    @Test
    void errorDelServidorOTokenMalConfiguradoSeTraducenAServicioNoDisponible() {
        servidor.expect(requestTo(BASE + "/internal/pasaportes?idMascota=2"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        servidor.expect(requestTo(BASE + "/internal/pasaportes?idMascota=3"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> adapter.listar(2)).isInstanceOf(ServicioNoDisponibleException.class);
        assertThatThrownBy(() -> adapter.listar(3)).isInstanceOf(ServicioNoDisponibleException.class);
    }

    /** El cliente real (DocsServiceConfig) corta a los 2 s aunque el servidor no responda. */
    @Test
    void elClienteRealCortaALosDosSegundos() throws Exception {
        HttpServer lento = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        lento.createContext("/", intercambio -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            intercambio.close();
        });
        lento.start();
        try {
            String base = "http://127.0.0.1:" + lento.getAddress().getPort();
            var real = new FastApiPasaporteAdapter(new DocsServiceConfig().pasaporteRestClient(base, TOKEN_INTERNO));

            long inicio = System.nanoTime();
            assertThatThrownBy(() -> real.listar(1)).isInstanceOf(ServicioNoDisponibleException.class);
            Duration transcurrido = Duration.ofNanos(System.nanoTime() - inicio);

            assertThat(transcurrido).isBetween(Duration.ofMillis(1_500), Duration.ofMillis(4_000));
        } finally {
            lento.stop(0);
        }
    }
}
