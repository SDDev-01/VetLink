package com.vetlink.fase2;

import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.cita.repository.CitaRepository;
import com.vetlink.cita.service.AtencionService;
import com.vetlink.historial.dto.HistorialClinicoForm;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * VL-18: finalizar una atención es atómico. Si falla el guardado de la cita ATENDIDA, la cita
 * sigue EN_ATENCION (no queda una cita «atendida» a medias). Corre SIN la transacción de prueba
 * (NOT_SUPPORTED) para ver el commit/rollback real.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VL18AtencionTransaccionTest extends PruebaWebBase {

    @MockitoSpyBean CitaRepository citaRepositorySpy;
    @Autowired AtencionService atencionService;

    @Test
    void siFallaGuardarLaCitaAtendidaLaCitaSigueEnAtencion() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(3).withSecond(0).withNano(0);
        Cita cita = crearCita(mascota, vet, clinica, inicio.toLocalDate(), inicio.toLocalTime(), EstadoCita.CONFIRMADA);

        atencionService.iniciar(cita.getIdCita(), vet.getIdUsuario());
        HistorialClinicoForm form = new HistorialClinicoForm();
        form.setIdMascota(mascota.getIdMascota());
        form.setMotivoConsulta("Control");
        form.setDiagnostico("Sano");
        atencionService.registrarHistorial(cita.getIdCita(), form, vet.getIdUsuario());

        doAnswer(inv -> {
            Cita c = inv.getArgument(0);
            if (c.getEstado() == EstadoCita.ATENDIDA) {
                throw new IllegalStateException("Falla simulada al guardar la cita");
            }
            return inv.callRealMethod();
        }).when(citaRepositorySpy).save(any(Cita.class));

        assertThatThrownBy(() -> atencionService.finalizar(cita.getIdCita(), vet.getIdUsuario()))
                .hasMessage("Falla simulada al guardar la cita");

        Cita recargada = citaRepository.findById(cita.getIdCita()).orElseThrow();
        assertThat(recargada.getEstado()).isEqualTo(EstadoCita.EN_ATENCION);
        assertThat(recargada.getFinAtencion()).isNull();
    }
}
