package com.vetlink.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Control de acceso basado en roles. Autenticacion por formulario + sesion
 * de servidor (no JWT): es el modelo natural para una app Spring MVC +
 * Thymeleaf renderizada en servidor.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(VetlinkUserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authProvider) throws Exception {
        http
            .authenticationProvider(authProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/registro", "/login", "/directorio/**", "/css/**", "/js/**", "/resource/**", "/uploads/**", "/api/catalogos/**").permitAll()
                .requestMatchers("/mascotas/**", "/citas/mias/**").hasRole("TUTOR")
                .requestMatchers("/recepcion/**", "/citas/recepcion/**").hasRole("RECEPCIONISTA")
                .requestMatchers("/historial/mias", "/historial/mascota/**").hasAnyRole("TUTOR", "VETERINARIO", "RECEPCIONISTA")
                .requestMatchers("/historial/nuevo", "/historial/nuevo/**").hasRole("VETERINARIO")
                .requestMatchers("/historial/pacientes").hasAnyRole("VETERINARIO", "RECEPCIONISTA")
                .requestMatchers("/veterinarias/admin/**", "/reportes/**").hasAnyRole("RECEPCIONISTA", "VETERINARIO")
                .requestMatchers("/dashboard/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"));

        return http.build();
    }
}
