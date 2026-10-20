package com.vetlink.usuario.service;

import com.vetlink.usuario.dto.RegistroUsuarioForm;
import com.vetlink.usuario.model.Usuario;

public interface UsuarioService {
    Usuario registrar(RegistroUsuarioForm form);
    boolean correoEnUso(String correo);
    boolean documentoEnUso(String numeroDocumento);
    Usuario actualizarPerfil(Integer idUsuario, com.vetlink.usuario.dto.PerfilForm form);
    void desactivarCuenta(Integer idUsuario, String passwordActual);
}
