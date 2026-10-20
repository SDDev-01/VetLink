package com.vetlink.emergencia.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import lombok.*;
@Getter @Setter public class EmergenciaForm { @NotNull private Integer idMascota; @NotNull private Integer idVeterinaria; @NotBlank private String motivo; private String detalle; }
