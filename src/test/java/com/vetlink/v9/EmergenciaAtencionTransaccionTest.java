package com.vetlink.v9;

import com.vetlink.emergencia.model.Emergencia;
import com.vetlink.emergencia.model.EstadoEmergencia;
import com.vetlink.emergencia.repository.EmergenciaRepository;
import com.vetlink.emergencia.service.EmergenciaAtencionService;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.notificacion.NotificacionService;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * VET-03: el registro clínico y el cambio de la emergencia a ATENDIDA son una sola transacción.
 * Sin transacción de prueba (NOT_SUPPORTED): cada llamada usa su propia transacción real.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmergenciaAtencionTransaccionTest extends PruebaWebBase {

    @Autowired EmergenciaAtencionService atencion;
    @Autowired EmergenciaRepository emergencias;
    @MockitoSpyBean NotificacionService notificaciones;

    private Emergencia asignada(Mascota mascota, Usuario vet) {
        Emergencia e = new Emergencia();
        e.setMascota(mascota);
        e.setSolicitante(mascota.getTutor());
        e.setVeterinaria(vet.getVeterinario().getVeterinaria());
        e.setVeterinario(vet);
        e.setMotivo("Intoxicación");
        e.setEstado(EstadoEmergencia.ASIGNADA);
        e.setCreadaEn(LocalDateTime.now().minusMinutes(10));
        return emergencias.save(e);
    }

    private static HistorialClinicoForm form(Mascota m) {
        HistorialClinicoForm f = new HistorialClinicoForm();
        f.setIdMascota(m.getIdMascota());
        f.setMotivoConsulta("Intoxicación");
        f.setDiagnostico("Intoxicación leve");
        return f;
    }

    @Test
    void siFallaUnPasoPosteriorNoQuedaNiElRegistroNiElCambioDeEstado() {
        var vet = crearVeterinario(crearClinica());
        var mascota = crearMascota(crearDuenio());
        var e = asignada(mascota, vet);
        // Falla la notificación al dueño, que ocurre DESPUÉS de insertar el historial y cambiar el estado.
        doThrow(new IllegalStateException("Servicio de notificaciones caído")).when(notificaciones)
                .notificar(anyInt(), eq("EMERGENCIA_ATENDIDA"), anyString(), anyString(), anyString(), anyString());
        try {
            assertThatThrownBy(() -> atencion.registrarAtencion(e.getIdEmergencia(), form(mascota), vet.getIdUsuario()))
                    .hasMessage("Servicio de notificaciones caído");
        } finally {
            reset(notificaciones);
        }
        assertThat(emergencias.findById(e.getIdEmergencia()).orElseThrow().getEstado()).isEqualTo(EstadoEmergencia.ASIGNADA);
        assertThat(historialRepository.findByEmergencia_IdEmergencia(e.getIdEmergencia())).isEmpty();
    }

    @Test
    void dosEnviosSimultaneosCreanUnSoloRegistro() throws Exception {
        var vet = crearVeterinario(crearClinica());
        var mascota = crearMascota(crearDuenio());
        var e = asignada(mascota, vet);

        int n = 4;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        List<String> errores = new CopyOnWriteArrayList<>();
        List<Future<?>> tareas = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            tareas.add(pool.submit(() -> {
                try {
                    salida.await();
                    atencion.registrarAtencion(e.getIdEmergencia(), form(mascota), vet.getIdUsuario());
                    exitos.incrementAndGet();
                } catch (Exception ex) {
                    errores.add(ex.getMessage());
                }
                return null;
            }));
        }
        salida.countDown();
        for (Future<?> t : tareas) t.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(errores).hasSize(n - 1);
        assertThat(emergencias.findById(e.getIdEmergencia()).orElseThrow().getEstado()).isEqualTo(EstadoEmergencia.ATENDIDA);
        assertThat(historialRepository.findAll().stream()
                .filter(h -> h.getEmergencia() != null && h.getEmergencia().getIdEmergencia().equals(e.getIdEmergencia())))
                .hasSize(1);
    }
}
