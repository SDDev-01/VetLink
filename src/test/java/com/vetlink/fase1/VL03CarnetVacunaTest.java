package com.vetlink.fase1;

import com.vetlink.mascota.model.Especie;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.support.PruebaWebBase;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-03: carnet de vacunas protegido y validaciones al registrar una vacuna. */
class VL03CarnetVacunaTest extends PruebaWebBase {

    @Autowired VacunaRepository vacunaRepository;
    @Autowired LoteVacunaRepository loteRepository;
    @Autowired TipoVacunaRepository tipoVacunaRepository;
    @Autowired FabricanteVacunaRepository fabricanteRepository;
    @Autowired CarnetVacunaRepository carnetRepository;

    private Vacuna crearVacuna(Especie especie) {
        TipoVacuna tipo = new TipoVacuna();
        tipo.setNombreTipo("Viral");
        FabricanteVacuna fab = new FabricanteVacuna();
        fab.setNombre("Lab");
        Vacuna v = new Vacuna();
        v.setNombreVacuna("Vacuna " + siguiente());
        v.setEspecie(especie);
        v.setTipoVacuna(tipoVacunaRepository.save(tipo));
        v.setFabricanteVacuna(fabricanteRepository.save(fab));
        return vacunaRepository.save(v);
    }

    private LoteVacuna crearLote(LocalDate vence) {
        LoteVacuna l = new LoteVacuna();
        l.setNumeroLote("L-" + siguiente());
        l.setFechaVencimiento(vence);
        return loteRepository.save(l);
    }

    private MockHttpServletRequestBuilder postVacuna(Mascota m, Vacuna v, LoteVacuna l) {
        return post("/carnet/" + m.getIdMascota() + "/nueva").with(csrf())
                .param("idVacuna", v.getIdVacuna().toString())
                .param("idLote", l.getIdLote().toString())
                .param("dosis", "1")
                .param("fechaAplicacion", LocalDate.now().toString())
                .param("proximaDosis", LocalDate.now().plusYears(1).toString());
    }

    @Test
    void duenioNoVeCarnetDeMascotaAjena() throws Exception {
        var ajena = crearMascota(crearDuenio());
        var otroDuenio = crearDuenio();

        mockMvc.perform(get("/carnet/" + ajena.getIdMascota()).with(como(otroDuenio)))
                .andExpect(status().isForbidden());
    }

    @Test
    void veterinarioSinCitasNoRegistraVacuna() throws Exception {
        var clinica = crearClinica();
        var vetExtranio = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        var vacuna = crearVacuna(mascota.getEspecie());
        long antes = carnetRepository.count();

        mockMvc.perform(postVacuna(mascota, vacuna, crearLote(LocalDate.now().plusYears(1))).with(como(vetExtranio)))
                .andExpect(status().isForbidden());

        assertThat(carnetRepository.count()).isEqualTo(antes);
    }

    @Test
    void recepcionistaNoRegistraVacuna() throws Exception {
        var clinica = crearClinica();
        var recepcion = crearRecepcionista(clinica, true);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, crearVeterinario(clinica), clinica);
        var vacuna = crearVacuna(mascota.getEspecie());

        mockMvc.perform(postVacuna(mascota, vacuna, crearLote(LocalDate.now().plusYears(1))).with(como(recepcion)))
                .andExpect(status().isForbidden());
    }

    @Test
    void veterinarioQueAtendioRegistraVacunaValida() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);
        var vacuna = crearVacuna(mascota.getEspecie());
        long antes = carnetRepository.count();

        mockMvc.perform(postVacuna(mascota, vacuna, crearLote(LocalDate.now().plusYears(1))).with(como(vet)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/historial/paciente/" + mascota.getIdMascota() + "?vacunaRegistrada"));

        assertThat(carnetRepository.count()).isEqualTo(antes + 1);
    }

    @Test
    void vacunaDeOtraEspecieNoSeGuardaYMuestraMensaje() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);
        var vacunaGato = crearVacuna(crearEspecie("Gato"));
        long antes = carnetRepository.count();

        mockMvc.perform(postVacuna(mascota, vacunaGato, crearLote(LocalDate.now().plusYears(1))).with(como(vet)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/historial/paciente/" + mascota.getIdMascota()))
                .andExpect(flash().attribute("errorVacuna", "La vacuna no corresponde a la especie de la mascota"));

        assertThat(carnetRepository.count()).isEqualTo(antes);
    }

    @Test
    void loteVencidoNoSeGuarda() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var mascota = crearMascota(crearDuenio());
        crearCita(mascota, vet, clinica);

        mockMvc.perform(postVacuna(mascota, crearVacuna(mascota.getEspecie()), crearLote(LocalDate.now().minusDays(1)))
                        .with(como(vet)))
                .andExpect(flash().attribute("errorVacuna", "El lote está vencido para esa fecha"));
    }

    @Test
    void historialSigueFuncionandoParaDuenioYVeterinarioAutorizado() throws Exception {
        var clinica = crearClinica();
        var vet = crearVeterinario(clinica);
        var duenio = crearDuenio();
        var mascota = crearMascota(duenio);
        crearCita(mascota, vet, clinica);
        sincronizar();

        mockMvc.perform(get("/historial/mascota/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/historial/paciente/" + mascota.getIdMascota()).with(como(vet)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/carnet/" + mascota.getIdMascota()).with(como(duenio)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/historial/paciente/" + mascota.getIdMascota()).with(como(crearVeterinario(clinica))))
                .andExpect(status().isForbidden());
    }
}
