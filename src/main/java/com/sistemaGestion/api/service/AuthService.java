package com.sistemaGestion.api.service;

import com.sistemaGestion.api.dto.AuthRequest;
import com.sistemaGestion.api.dto.AuthResponse;
import com.sistemaGestion.api.model.Usuario;
import com.sistemaGestion.api.repository.UsuarioRepository;
import com.sistemaGestion.api.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(AuthRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El usuario ya existe");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEmail(request.getEmail());
        usuario.setRol(Usuario.Rol.ADMIN);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        String token = jwtUtil.generateToken(usuario.getUsername());
        return new AuthResponse(token, usuario.getUsername(), usuario.getRol().name());
    }

    public AuthResponse login(AuthRequest request) {
        log.info("Buscando usuario: {}", request.getUsername());

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        log.info("Usuario encontrado: {}", usuario.getUsername());
        log.info("Password en BD: {}", usuario.getPassword());
        log.info("Password recibido: {}", request.getPassword());

        boolean matches = passwordEncoder.matches(request.getPassword(), usuario.getPassword());
        log.info("¿Coinciden las contraseñas?: {}", matches);

        if (!matches) {
            throw new RuntimeException("Credenciales inválidas");
        }

        String token = jwtUtil.generateToken(usuario.getUsername());
        return new AuthResponse(token, usuario.getUsername(), usuario.getRol().name());
    }
}