package com.vetlink.pattern.strategy;

import com.vetlink.cita.dto.AgendarCitaForm;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompositeValidadorCita {

    private final List<ValidadorCita> validadores;

    public CompositeValidadorCita(List<ValidadorCita> validadores) {
        this.validadores = validadores;
    }

    public void validarTodo(AgendarCitaForm form) {
        validadores.forEach(v -> v.validar(form));
    }
}
