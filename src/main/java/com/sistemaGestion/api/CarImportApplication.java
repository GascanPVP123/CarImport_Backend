package com.sistemaGestion.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class CarImportApplication {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("NUEVO HASH: " + hash);

        // Verificar que funciona
        boolean matches = encoder.matches("admin123", hash);
        System.out.println("¿Funciona?: " + matches);

        SpringApplication.run(CarImportApplication.class, args);
    }
}