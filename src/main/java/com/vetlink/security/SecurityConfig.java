package com.vetlink.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Control de acceso basado en roles. Autenticación por formulario + sesión de servidor.
 * Las reglas de URL son la primera barrera; cada controlador/servicio vuelve a verificar
 * la pertenencia del recurso (mascota, cita, clínica) para evitar manipulación de ids.
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
                .requestMatchers("/", "/registro", "/login", "/directorio/**", "/css/**", "/js/**", "/resource/**",
                        "/api/catalogos/**", "/api/registro/disponible", "/error").permitAll()
                // La foto también la ve el personal que atiende a la mascota (lo verifica AccesoPacienteService).
                .requestMatchers(HttpMethod.GET, "/mascotas/*/foto").authenticated()
                .requestMatchers("/mascotas/**", "/citas/mias/**", "/citas/*/reprogramacion/**").hasRole("TUTOR")
                .requestMatchers("/recepcion/**", "/citas/recepcion/**").hasRole("RECEPCIONISTA")
                .requestMatchers("/disponibilidad/**").hasRole("VETERINARIO")
                .requestMatchers("/agenda/**").hasAnyRole("VETERINARIO", "RECEPCIONISTA")
                .requestMatchers("/historial/mias", "/historial/mascota/**", "/historial/documento/**",
                        "/historial/archivos/**").hasAnyRole("TUTOR", "VETERINARIO", "RECEPCIONISTA")
                .requestMatchers("/historial/nuevo", "/historial/nuevo/**", "/historial/*/editar").hasRole("VETERINARIO")
                .requestMatchers("/historial/pacientes", "/historial/paciente/**").hasAnyRole("VETERINARIO", "RECEPCIONISTA")
                .requestMatchers("/veterinarias/admin/**", "/reportes/**").hasAnyRole("RECEPCIONISTA", "VETERINARIO")
                .requestMatchers("/dashboard/**", "/notificaciones/**", "/internal/auth/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/carnet/**").hasRole("VETERINARIO")
                .requestMatchers("/citas/mascota/**", "/citas/agendar/**").hasRole("TUTOR")
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
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/403")
                // La subpetición de Nginx (auth_request) necesita 401, no una redirección al login;
                // el resto de rutas sigue redirigiendo a /login.
                .authenticationEntryPoint(entradaSinSesion()));

        return http.build();
    }

    private static AuthenticationEntryPoint entradaSinSesion() {
        AuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");
        AuthenticationEntryPoint noAutorizado = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        return (req, res, e) -> {
            String ruta = req.getRequestURI().substring(req.getContextPath().length());
            (ruta.startsWith("/internal/auth/") ? noAutorizado : login).commence(req, res, e);
        };
    }
}
