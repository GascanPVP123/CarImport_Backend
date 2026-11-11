package com.sistemaGestion.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Aplica el prefijo /api opcionalmente a todos los RestController
        // Permitiendo que /dashboard/ejecutivo y /api/dashboard/ejecutivo apunten al mismo controlador
        configurer.addPathPrefix("/api", c -> true);
    }
}
