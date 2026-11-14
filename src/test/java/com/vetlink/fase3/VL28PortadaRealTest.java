package com.vetlink.fase3;

import com.vetlink.resena.model.Calificacion;
import com.vetlink.resena.model.Resena;
import com.vetlink.resena.repository.CalificacionRepository;
import com.vetlink.resena.repository.ResenaRepository;
import com.vetlink.support.PruebaWebBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

/** VL-28: la portada muestra cifras reales y no promete lo que el sistema no ofrece. */
class VL28PortadaRealTest extends PruebaWebBase {

    @Autowired CalificacionRepository calificacionRepository;
    @Autowired ResenaRepository resenaRepository;

    private void resena(com.vetlink.veterinaria.model.Veterinaria clinica, int valor) {
        Calificacion c = new Calificacion();
        c.setValor(valor);
        c.setDescripcion("v" + valor);
        Resena r = new Resena();
        r.setCalificacion(calificacionRepository.save(c));
        r.setVeterinaria(clinica);
        r.setTutor(crearDuenio());
        r.setFechaPublicacion(LocalDate.now());
        resenaRepository.save(r);
    }

    @Test
    void portadaMuestraLasCifrasDeLaBase() throws Exception {
        var a = crearClinica();
        var b = crearClinica();
        var inactiva = crearClinica();
        inactiva.setEstado((byte) 0);
        crearVeterinario(a);
        crearVeterinario(b);
        crearVeterinario(b);
        var duenio = crearDuenio();
        crearMascota(duenio);
        crearMascota(duenio);
        crearMascota(duenio).setEstado((byte) 0);
        resena(a, 5);
        resena(a, 4);
        resena(b, 4); // promedio 4,333 -> 4,3
        sincronizar();

        long clinicas = veterinariaRepository.countByEstado((byte) 1);
        long vets = veterinarioRepository.count();
        long mascotas = mascotaRepository.countByEstado((byte) 1);

        mockMvc.perform(get("/"))
                .andExpect(model().attribute("statClinicas", is(clinicas)))
                .andExpect(model().attribute("statVeterinarios", is(vets)))
                .andExpect(model().attribute("statMascotas", is(mascotas)))
                .andExpect(model().attribute("statPromedio", is(4.3)))
                .andExpect(content().string(containsString("4,3/5")))
                .andExpect(content().string(not(containsString("10K+"))))
                .andExpect(content().string(not(containsString("Soporte 24/7"))))
                .andExpect(content().string(not(containsString("Cancelación gratuita"))))
                .andExpect(content().string(not(containsString("Sin tarjeta de crédito"))));
    }

    @Test
    void sinResenasNoInventaUnPromedio() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("Sin reseñas")));
    }
}
