package com.vetlink.fase4;

import com.vetlink.cita.dto.AgendarCitaForm;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.service.CitaService;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dos (o más) usuarios reservan el mismo cupo al mismo tiempo: solo uno lo consigue y el resto
 * recibe «Esta cita acaba de ser reservada por otro usuario...». Sin transacción de prueba
 * (NOT_SUPPORTED) para que cada hilo use su propia transacción real con bloqueos.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VL32ConcurrenciaReservaTest extends PruebaWebBase {

    @Autowired CitaService citaService;

    @Test
    void soloUnaReservaGanaElMismoCupo() throws Exception {
        var clinica = crearClinica();
        ofrecerServicio(clinica, servicioPorDefecto());
        Usuario vet = crearVeterinario(clinica);
        Cita cupo = crearCita(null, vet, clinica, LocalDate.now().plusDays(7), LocalTime.of(9, 0), EstadoCita.DISPONIBLE);

        int n = 6;
        List<Usuario> duenios = new ArrayList<>();
        List<Mascota> mascotas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Usuario d = crearDuenio();
            duenios.add(d);
            mascotas.add(crearMascota(d));
        }

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        List<String> mensajes = new CopyOnWriteArrayList<>();
        List<Future<?>> tareas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int k = i;
            tareas.add(pool.submit(() -> {
                AgendarCitaForm f = new AgendarCitaForm();
                f.setIdCupo(cupo.getIdCita());
                f.setIdMascota(mascotas.get(k).getIdMascota());
                f.setIdVeterinaria(clinica.getIdVeterinaria());
                f.setIdVeterinario(vet.getIdUsuario());
                f.setIdServicio(servicioPorDefecto().getIdServicio());
                f.setFecha(cupo.getFecha());
                f.setHora(cupo.getHora());
                try {
                    salida.await();
                    citaService.agendar(f, duenios.get(k).getIdUsuario());
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    mensajes.add(e.getMessage());
                }
                return null;
            }));
        }
        salida.countDown();
        for (Future<?> t : tareas) t.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(mensajes).hasSize(n - 1)
                .allMatch(m -> m.startsWith("Esta cita acaba de ser reservada por otro usuario"));
        Cita final_ = citaRepository.findById(cupo.getIdCita()).orElseThrow();
        assertThat(final_.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(final_.getMascota()).isNotNull();
    }
}
