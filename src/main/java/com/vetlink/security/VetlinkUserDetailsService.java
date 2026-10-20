package com.vetlink.security;

import com.vetlink.usuario.model.Usuario;
import com.vetlink.usuario.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VetlinkUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public VetlinkUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("No existe una cuenta con ese correo"));
        if (usuario.getRol() == null) {
            throw new UsernameNotFoundException("La cuenta no tiene un rol asignado");
        }
        return new VetlinkUserPrincipal(usuario, correo);
    }
}
