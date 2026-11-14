package com.vetlink.fase2;

import com.vetlink.cita.dto.CitaCalendarDTO;
import com.vetlink.cita.model.Cita;
import com.vetlink.cita.model.EstadoCita;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.vacuna.model.CarnetVacuna;
import com.vetlink.vacuna.model.FabricanteVacuna;
import com.vetlink.vacuna.model.LoteVacuna;
import com.vetlink.vacuna.model.TipoVacuna;
import com.vetlink.vacuna.model.Vacuna;
import com.vetlink.vacuna.repository.CarnetVacunaRepository;
import com.vetlink.vacuna.repository.FabricanteVacunaRepository;
import com.vetlink.vacuna.repository.LoteVacunaRepository;
import com.vetlink.vacuna.repository.TipoVacunaRepository;
import com.vetlink.vacuna.repository.VacunaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-13: "Próxima cita" del dashboard del dueño solo considera citas futuras de mascotas activas. */
class VL13ProximaCitaTest extends PruebaWebBase {

    @Autowired CarnetVacunaRepository carnetRepository;
    @Autowired VacunaRepository vacunaRepository;
    @Autowired LoteVacunaRepository loteRepository;
    @Autowired TipoVacunaRepository tipoRepository;
    @Autowired FabricanteVacunaRepository fabricanteRepository;

    private MvcResult dashboard(com.vetlink.usuario.model.Usuario duenio) throws Exception {
        return mockMvc.perform(get("/dashboard").with(como(duenio))).andExpect(status().isOk()).andReturn();
    }

    @Test
    void proximaCitaEsLaFuturaYNoLaPasada() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        var pasada = crearCita(mascota, vet, clinica, LocalDate.now().minusMonths(7), LocalTime.of(9, 0), EstadoCita.AGENDADA);
        var futura = crearCita(mascota, vet, clinica, LocalDate.now().plusDays(2), LocalTime.of(9, 0), EstadoCita.AGENDADA);
        sincronizar();

        var model = dashboard(duenio).getModelAndView().getModel();
        assertThat(((Cita) model.get("proximaCita")).getIdCita()).isEqualTo(futura.getIdCita());

        @SuppressWarnings("unchecked")
        var calendario = (List<CitaCalendarDTO>) model.get("todasCitas");
        assertThat(calendario).extracting(CitaCalendarDTO::idCita)
                .contains(futura.getIdCita()).doesNotContain(pasada.getIdCita());
    }

    @Test
    void citaFuturaDeMascotaInactivaNoAparece() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var inactiva = crearMascota(duenio);
        inactiva.setEstado((byte) 0);
        crearCita(inactiva, vet, clinica, LocalDate.now().plusDays(1), LocalTime.of(9, 0), EstadoCita.AGENDADA);
        sincronizar();

        var model = dashboard(duenio).getModelAndView().getModel();
        assertThat(model.get("proximaCita")).isNull();
        assertThat((List<?>) model.get("notificaciones")).isEmpty();
    }

    @Test
    void vacunasPendientesNoCuentanMascotasInactivas() throws Exception {
        var duenio = crearDuenio();
        var activa = crearMascota(duenio);
        var inactiva = crearMascota(duenio);
        inactiva.setEstado((byte) 0);
        TipoVacuna tipo = new TipoVacuna();
        tipo.setNombreTipo("Viral");
        FabricanteVacuna fab = new FabricanteVacuna();
        fab.setNombre("Lab");
        Vacuna vacuna = new Vacuna();
        vacuna.setNombreVacuna("Rabia");
        vacuna.setEspecie(activa.getEspecie());
        vacuna.setTipoVacuna(tipoRepository.save(tipo));
        vacuna.setFabricanteVacuna(fabricanteRepository.save(fab));
        vacunaRepository.save(vacuna);
        LoteVacuna lote = new LoteVacuna();
        lote.setNumeroLote("L1");
        lote.setFechaVencimiento(LocalDate.now().plusYears(1));
        loteRepository.save(lote);
        for (var m : List.of(activa, inactiva)) {
            CarnetVacuna cv = new CarnetVacuna();
            cv.setMascota(m);
            cv.setVacuna(vacuna);
            cv.setLote(lote);
            cv.setUsuario(duenio);
            cv.setDosis("1");
            cv.setFechaAplicacion(LocalDate.now().minusYears(1));
            cv.setProximaDosis(LocalDate.now().plusDays(10));
            carnetRepository.save(cv);
        }
        sincronizar();

        assertThat(dashboard(duenio).getModelAndView().getModel().get("statVacunas")).isEqualTo(1);
    }
}
