package com.vetlink.fase2;

import com.vetlink.cita.model.EstadoCita;
import com.vetlink.resena.model.Calificacion;
import com.vetlink.resena.repository.CalificacionRepository;
import com.vetlink.resena.repository.ResenaRepository;
import com.vetlink.support.PruebaWebBase;
import com.vetlink.usuario.model.Usuario;
import com.vetlink.veterinaria.model.Veterinaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/** VL-10: reseñas de 1 a 5 estrellas, solo tras una cita ATENDIDA y una por clínica. */
class VL10ResenasTest extends PruebaWebBase {

    @Autowired CalificacionRepository calificacionRepository;
    @Autowired ResenaRepository resenaRepository;

    @BeforeEach
    void calificaciones() {
        // Lo mismo que queda en mydb tras V5: valores 5..1.
        String[] desc = {"Muy malo", "Malo", "Regular", "Bueno", "Excelente"};
        for (int v = 1; v <= 5; v++) {
            Calificacion c = new Calificacion();
            c.setValor(v);
            c.setDescripcion(desc[v - 1]);
            calificacionRepository.save(c);
        }
    }

    private void citaCon(Usuario duenio, Veterinaria clinica, EstadoCita estado, LocalDate fecha) {
        crearCita(crearMascota(duenio), crearVeterinario(clinica), clinica, fecha, LocalTime.of(10, 0), estado);
    }

    private org.springframework.test.web.servlet.ResultActions resenar(Usuario duenio, Veterinaria clinica, int valor)
            throws Exception {
        return mockMvc.perform(post("/resenas/nueva").with(csrf()).with(como(duenio))
                .param("idVeterinaria", clinica.getIdVeterinaria().toString())
                .param("valor", String.valueOf(valor))
                .param("comentario", "Comentario de prueba"));
    }

    @Test
    void resenaDeUnaEstrellaConCitaAtendidaSeGuarda() throws Exception {
        var clinica = crearClinica();
        var duenio = crearDuenio();
        citaCon(duenio, clinica, EstadoCita.ATENDIDA, LocalDate.now().minusDays(3));
        long antes = resenaRepository.count();

        resenar(duenio, clinica, 1)
                .andExpect(redirectedUrl("/directorio/" + clinica.getIdVeterinaria() + "?gracias"));

        assertThat(resenaRepository.count()).isEqualTo(antes + 1);
    }

    @Test
    void soloConCitaAgendadaFuturaDaError() throws Exception {
        var clinica = crearClinica();
        var duenio = crearDuenio();
        citaCon(duenio, clinica, EstadoCita.AGENDADA, LocalDate.now().plusDays(5));
        long antes = resenaRepository.count();

        resenar(duenio, clinica, 4)
                .andExpect(flash().attribute("errorResena",
                        "Debes haber sido atendido en esta veterinaria para dejar una reseña"));

        assertThat(resenaRepository.count()).isEqualTo(antes);
    }

    @Test
    void segundaResenaALaMismaClinicaDaError() throws Exception {
        var clinica = crearClinica();
        var duenio = crearDuenio();
        citaCon(duenio, clinica, EstadoCita.ATENDIDA, LocalDate.now().minusDays(3));

        resenar(duenio, clinica, 5);
        long antes = resenaRepository.count();
        resenar(duenio, clinica, 2)
                .andExpect(flash().attribute("errorResena", "Ya dejaste una reseña para esta veterinaria"));

        assertThat(resenaRepository.count()).isEqualTo(antes);
    }

    @Test
    void formularioMuestraCincoEstrellas() throws Exception {
        var clinica = crearClinica();

        mockMvc.perform(get("/directorio/" + clinica.getIdVeterinaria()).with(como(crearDuenio())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"estrella1\"")))
                .andExpect(content().string(containsString("id=\"estrella5\"")));
    }
}
