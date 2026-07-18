package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.NotaVentaRequest;
import com.sistemaGestion.api.model.NotaVenta;
import com.sistemaGestion.api.service.NotaVentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notas-venta")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class NotaVentaController {

    private final NotaVentaService notaVentaService;

    @PostMapping
    public ResponseEntity<NotaVenta> crear(@RequestBody NotaVentaRequest request) {
        return ResponseEntity.ok(notaVentaService.crear(request));
    }

}