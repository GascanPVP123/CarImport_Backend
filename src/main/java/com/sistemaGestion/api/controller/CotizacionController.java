package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.model.Cotizacion;
import com.sistemaGestion.api.service.CotizacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class CotizacionController {

    @Autowired
    private CotizacionService cotizacionService;

    // Registrar una cotización
    @PostMapping
    public ResponseEntity<Cotizacion> crearCotizacion(@RequestBody Cotizacion cotizacion) {
        try {
            Cotizacion nuevaCotizacion = cotizacionService.generarCotizacion(cotizacion);
            return ResponseEntity.ok(nuevaCotizacion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Listar historial
    @GetMapping
    public ResponseEntity<List<Cotizacion>> listarCotizaciones() {
        return ResponseEntity.ok(cotizacionService.listarTodas());
    }
}