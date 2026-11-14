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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * VL-18: si falla el guardado de la cita, la historia tampoco queda.
 * Corre SIN la transacción de prueba (NOT_SUPPORTED) para ver el commit/rollback real,
 * y con un espía sobre CitaRepository que falla al guardar una cita ATENDIDA.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VL18AtencionTransaccionTest extends PruebaWebBase {

    @MockitoSpyBean CitaRepository citaRepositorySpy;
    @Autowired AtencionService atencionService;

    @Test
    void siFallaGuardarLaCitaNoQuedaHistoria() {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var cita = crearCita(mascota, vet, clinica, LocalDate.now(), LocalTime.of(10, 0), EstadoCita.AGENDADA);
        long historiasAntes = historialRepository.count();

        doAnswer(inv -> {
            Cita c = inv.getArgument(0);
            if (c.getEstado() == EstadoCita.ATENDIDA) {
                throw new IllegalStateException("Falla simulada al guardar la cita");
            }
            return inv.callRealMethod();
        }).when(citaRepositorySpy).save(any(Cita.class));

        HistorialClinicoForm form = new HistorialClinicoForm();
        form.setIdMascota(mascota.getIdMascota());
        form.setMotivoConsulta("Control");
        form.setDiagnostico("Sano");

        assertThatThrownBy(() -> atencionService.atender(cita.getIdCita(), form, vet.getIdUsuario()))
                .hasMessage("Falla simulada al guardar la cita");

        assertThat(historialRepository.count()).isEqualTo(historiasAntes);
        assertThat(citaRepository.findById(cita.getIdCita()).orElseThrow().getEstado()).isEqualTo(EstadoCita.AGENDADA);
    }
}
