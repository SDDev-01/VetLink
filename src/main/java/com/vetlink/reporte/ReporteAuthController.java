package com.vetlink.reporte;

import com.vetlink.historial.model.HistorialClinico;
import com.vetlink.historial.repository.HistorialClinicoRepository;
import com.vetlink.mascota.model.Mascota;
import com.vetlink.mascota.repository.MascotaRepository;
import com.vetlink.security.AccesoPacienteService;
import com.vetlink.security.VetlinkUserPrincipal;
import com.vetlink.usuario.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autorización de descargas de reportes PDF (servicio FastAPI) mediante {@code auth_request} de Nginx.
 *
 * <p>Nginx llama aquí con la cookie de sesión del usuario y la URI original. Spring valida la
 * sesión (si no hay, responde 401) y aplica la MISMA regla de acceso clínico del resto de la
 * aplicación (AccesoPacienteService); si se permite, responde 204 con la identidad que Nginx
 * reenvía a FastAPI. Cambiar el id en la URL no sirve: se verifica el recurso pedido.</p>
 *
 * <p>Nginx bloquea {@code /internal/} desde el exterior: solo la subpetición interna llega aquí.</p>
 */
@RestController
public class ReporteAuthController {

    static final Pattern MASCOTA = Pattern.compile("^/api/reportes/mascotas/(\\d{1,10})/(historial|vacunas)(\\?.*)?$");
    static final Pattern REGISTRO = Pattern.compile("^/api/reportes/historial/(\\d{1,10})/pdf(\\?.*)?$");

    private final MascotaRepository mascotas;
    private final HistorialClinicoRepository historiales;
    private final AccesoPacienteService acceso;

    public ReporteAuthController(MascotaRepository mascotas, HistorialClinicoRepository historiales,
                                 AccesoPacienteService acceso) {
        this.mascotas = mascotas;
        this.historiales = historiales;
        this.acceso = acceso;
    }

    @GetMapping("/internal/auth/reportes")
    public ResponseEntity<Void> autorizar(@RequestHeader(value = "X-Original-URI", required = false) String uri,
                                          @AuthenticationPrincipal VetlinkUserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (uri == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Usuario u = principal.getUsuario();
        boolean permitido = false;
        Matcher m = MASCOTA.matcher(uri);
        if (m.matches()) {
            Optional<Mascota> mascota = mascotas.findById(Integer.valueOf(m.group(1)));
            permitido = mascota.isPresent() && acceso.puedeVerClinica(mascota.get(), u);
        } else {
            Matcher r = REGISTRO.matcher(uri);
            if (r.matches()) {
                Optional<HistorialClinico> h = historiales.findById(Integer.valueOf(r.group(1)));
                permitido = h.isPresent() && acceso.puedeVerDocumento(h.get(), u);
            }
        }
        if (!permitido) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Integer clinica = switch (u.getRol()) {
            case RECEPCIONISTA -> u.getRecepcionista().getVeterinaria().getIdVeterinaria();
            case VETERINARIO -> u.getVeterinario().getVeterinaria() == null ? null
                    : u.getVeterinario().getVeterinaria().getIdVeterinaria();
            case TUTOR -> null;
        };
        return ResponseEntity.noContent()
                .header("X-Vetlink-Usuario", String.valueOf(u.getIdUsuario()))
                .header("X-Vetlink-Rol", u.getRol().name())
                .header("X-Vetlink-Veterinaria", clinica == null ? "" : String.valueOf(clinica))
                .build();
    }
}
