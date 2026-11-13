package com.vetlink.fase2;

import com.vetlink.support.PruebaWebBase;
import com.vetlink.ubicacion.model.Ciudad;
import com.vetlink.veterinaria.model.DiaSemana;
import com.vetlink.veterinaria.model.HorarioVeterinaria;
import com.vetlink.veterinaria.model.Veterinaria;
import com.vetlink.veterinaria.repository.DiaSemanaRepository;
import com.vetlink.veterinaria.repository.HorarioVeterinariaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** VL-14: domingo disponible, varios rangos por día y validación apertura < cierre. */
class VL14HorariosTest extends PruebaWebBase {

    @Autowired DiaSemanaRepository diaSemanaRepository;
    @Autowired HorarioVeterinariaRepository horarioRepository;

    private DiaSemana dia(String nombre) {
        DiaSemana d = new DiaSemana();
        d.setDiaSemana(nombre);
        return diaSemanaRepository.save(d);
    }

    private void horario(Veterinaria clinica, DiaSemana dia, String abre, String cierra) {
        HorarioVeterinaria h = new HorarioVeterinaria();
        h.setVeterinaria(clinica);
        h.setDiaSemana(dia);
        h.setHoraApertura(LocalTime.parse(abre));
        h.setHoraCierre(LocalTime.parse(cierra));
        h.setEstado((byte) 1);
        horarioRepository.save(h);
    }

    private static LocalDate proximo(DayOfWeek d) {
        return LocalDate.now().with(TemporalAdjusters.next(d));
    }

    @Test
    void clinicaConHorarioDominicalNoApareceCerradaElDomingo() throws Exception {
        var clinica = crearClinica();
        horario(clinica, dia("Domingo"), "09:00", "13:00");
        var duenio = crearDuenio();

        mockMvc.perform(get("/citas/horarios-disponibles").with(como(duenio))
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("fecha", proximo(DayOfWeek.SUNDAY).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrado", is(false)))
                .andExpect(jsonPath("$.apertura", is("09:00")));
    }

    @Test
    void dosRangosElLunesVienenEnLaRespuesta() throws Exception {
        var clinica = crearClinica();
        var lunes = dia("Lunes");
        horario(clinica, lunes, "14:00", "18:00");
        horario(clinica, lunes, "08:00", "12:00");
        horario(clinica, lunes, "20:34", "00:23"); // inválido, se ignora

        mockMvc.perform(get("/citas/horarios-disponibles").with(como(crearDuenio()))
                        .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                        .param("fecha", proximo(DayOfWeek.MONDAY).toString()))
                .andExpect(jsonPath("$.cerrado", is(false)))
                .andExpect(jsonPath("$.rangos", hasSize(2)))
                .andExpect(jsonPath("$.rangos[0].apertura", is("08:00")))
                .andExpect(jsonPath("$.rangos[0].cierre", is("12:00")))
                .andExpect(jsonPath("$.rangos[1].apertura", is("14:00")))
                .andExpect(jsonPath("$.rangos[1].cierre", is("18:00")))
                .andExpect(jsonPath("$.apertura", is("08:00")))
                .andExpect(jsonPath("$.cierre", is("18:00")));
    }

    @Test
    void registrarHorarioDe18A08DaError() throws Exception {
        var lunes = dia("Lunes");
        Ciudad ciudad = new Ciudad();
        ciudad.setNombre("Bogotá");
        ciudad = ciudadRepository.save(ciudad);
        int n = siguiente();
        long antes = veterinariaRepository.count();

        mockMvc.perform(post("/registro").with(csrf())
                        .param("numeroDocumento", "HOR" + n)
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Ana").param("apellido", "Horario")
                        .param("correo", "hor" + n + "@pruebas.vetlink")
                        .param("telefono", "3001234567")
                        .param("password", PASSWORD_PRUEBAS).param("confirmarPassword", PASSWORD_PRUEBAS)
                        .param("rol", "VETERINARIA")
                        .param("nuevaVeterinaria.nit", "901" + n)
                        .param("nuevaVeterinaria.nombre", "Clínica Nocturna")
                        .param("nuevaVeterinaria.telefono", "6011234567")
                        .param("nuevaVeterinaria.correo", "noct" + n + "@pruebas.vetlink")
                        .param("nuevaVeterinaria.direccion", "Calle 1")
                        .param("nuevaVeterinaria.idCiudad", ciudad.getIdCiudad().toString())
                        .param("nuevaVeterinaria.barrio", "Centro")
                        .param("nuevaVeterinaria.horarios[0].idDiaSemana", lunes.getIdDiaSemana().toString())
                        .param("nuevaVeterinaria.horarios[0].abierto", "true")
                        .param("nuevaVeterinaria.horarios[0].horaApertura", "18:00")
                        .param("nuevaVeterinaria.horarios[0].horaCierre", "08:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("la hora de apertura debe ser anterior a la de cierre")));

        assertThat(veterinariaRepository.count()).isEqualTo(antes);
        assertThat(usuarioRepository.findByNumeroDocumento("HOR" + n)).isEmpty();
    }

    @Test
    void registrarDosRangosElMismoDia() throws Exception {
        var lunes = dia("Lunes");
        Ciudad ciudad = new Ciudad();
        ciudad.setNombre("Bogotá");
        ciudad = ciudadRepository.save(ciudad);
        int n = siguiente();

        mockMvc.perform(post("/registro").with(csrf())
                        .param("numeroDocumento", "HOR" + n)
                        .param("idTipoDocumento", tipoDocumento().getIdTipoDocumento().toString())
                        .param("nombre", "Ana").param("apellido", "Partida")
                        .param("correo", "par" + n + "@pruebas.vetlink")
                        .param("telefono", "3001234567")
                        .param("password", PASSWORD_PRUEBAS).param("confirmarPassword", PASSWORD_PRUEBAS)
                        .param("rol", "VETERINARIA")
                        .param("nuevaVeterinaria.nit", "902" + n)
                        .param("nuevaVeterinaria.nombre", "Clínica Partida")
                        .param("nuevaVeterinaria.telefono", "6011234567")
                        .param("nuevaVeterinaria.correo", "part" + n + "@pruebas.vetlink")
                        .param("nuevaVeterinaria.direccion", "Calle 2")
                        .param("nuevaVeterinaria.idCiudad", ciudad.getIdCiudad().toString())
                        .param("nuevaVeterinaria.barrio", "Centro")
                        .param("nuevaVeterinaria.horarios[0].idDiaSemana", lunes.getIdDiaSemana().toString())
                        .param("nuevaVeterinaria.horarios[0].abierto", "true")
                        .param("nuevaVeterinaria.horarios[0].horaApertura", "08:00")
                        .param("nuevaVeterinaria.horarios[0].horaCierre", "12:00")
                        .param("nuevaVeterinaria.horarios[1].idDiaSemana", lunes.getIdDiaSemana().toString())
                        .param("nuevaVeterinaria.horarios[1].abierto", "true")
                        .param("nuevaVeterinaria.horarios[1].horaApertura", "14:00")
                        .param("nuevaVeterinaria.horarios[1].horaCierre", "18:00"))
                .andExpect(status().is3xxRedirection());

        sincronizar();
        var clinica = usuarioRepository.findByNumeroDocumento("HOR" + n).orElseThrow()
                .getRecepcionista().getVeterinaria();
        assertThat(horarioRepository.findByVeterinaria_IdVeterinaria(clinica.getIdVeterinaria())).hasSize(2);
    }
}
