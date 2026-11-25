package com.sistemaGestion.api.security;

import com.sistemaGestion.api.exception.NoAutenticadoException;
import com.sistemaGestion.api.model.Usuario;
import com.sistemaGestion.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resuelve el usuario autenticado a partir del SecurityContext (lo llena JwtAuthFilter
 * con el username del token). Sustituye al antiguo usuario fijo con id 1.
 */
@Component
@RequiredArgsConstructor
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    /** @throws NoAutenticadoException si no hay token válido o el usuario no existe / está inactivo. */
    public Usuario obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || auth.getName() == null) {
            throw new NoAutenticadoException("Debe iniciar sesión para realizar esta operación");
        }
        Usuario usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado: " + auth.getName()));
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new NoAutenticadoException("El usuario " + usuario.getUsername() + " está inactivo");
        }
        return usuario;
    }
}
