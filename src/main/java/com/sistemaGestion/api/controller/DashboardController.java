package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.DashboardEjecutivoDTO;
import com.sistemaGestion.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
// Acepta ambas rutas para ser 100% compatible con tus otros servicios
@RequestMapping({"/api/dashboard", "/api/v1/dashboard"})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/ejecutivo")
    public ResponseEntity<DashboardEjecutivoDTO> obtenerDashboardEjecutivo(
            @RequestParam(defaultValue = "2026") int anio,
            @RequestParam(defaultValue = "10") int mes
    ) {
        return ResponseEntity.ok(dashboardService.obtenerResumenEjecutivo(anio, mes));
    }
}