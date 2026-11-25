package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.NotaVentaRequest;
import com.sistemaGestion.api.model.NotaVenta;
import com.sistemaGestion.api.service.NotaVentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notas-venta")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotaVentaController {

    private final NotaVentaService notaVentaService;

    @PostMapping
    public ResponseEntity<NotaVenta> crear(@RequestBody NotaVentaRequest request) {
        return ResponseEntity.ok(notaVentaService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<NotaVenta>> obtenerHistorial() {
        return ResponseEntity.ok(notaVentaService.obtenerHistorial());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaVenta> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(notaVentaService.obtenerPorId(id));
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        notaVentaService.anular(id);
        return ResponseEntity.noContent().build();
    }
}