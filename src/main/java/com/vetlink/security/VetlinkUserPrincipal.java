package com.vetlink.security;

import com.vetlink.usuario.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre nuestra entidad Usuario y el contrato UserDetails que
 * exige Spring Security. La autoridad ("ROLE_TUTOR", "ROLE_VETERINARIO",
 * "ROLE_RECEPCIONISTA") se calcula dinamicamente a partir de Usuario.getRol().
 */
public class VetlinkUserPrincipal implements UserDetails {

    private final Usuario usuario;
    private final String correo;

    public VetlinkUserPrincipal(Usuario usuario, String correo) {
        this.usuario = usuario;
        this.correo = correo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}
