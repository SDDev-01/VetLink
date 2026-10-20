package com.vetlink.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public String accesoDenegado(SecurityException ex, Model model) {
        model.addAttribute("mensaje", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String noEncontrado(IllegalArgumentException ex, Model model) {
        model.addAttribute("mensaje", ex.getMessage());
        return "error/404";
    }
}
