package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.CargoPendienteDTO;
import com.sistemaGestion.api.dto.PagoReciboRequest;
import com.sistemaGestion.api.dto.PagoReciboResponseDTO;
import com.sistemaGestion.api.exception.ConflictoException;
import com.sistemaGestion.api.exception.RecursoNoEncontradoException;
import com.sistemaGestion.api.exception.ReglaNegocioException;
import com.sistemaGestion.api.service.PagoReciboService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tesorería — pagos/vouchers de clientes y su aplicación a cargos de cuenta corriente.
 *
 * POST   /api/pagos-recibos                              registrar pago (+ asignaciones opcionales)
 * GET    /api/pagos-recibos?clienteId=&desde=&hasta=     listar (resumen)
 * GET    /api/pagos-recibos/{id}                         detalle con asignaciones
 * GET    /api/pagos-recibos/cargos-pendientes/{clienteId} cargos por cobrar del cliente
 * DELETE /api/pagos-recibos/{id}                         anular pago (revierte estados de cargos)
 */
@RestController
@RequestMapping("/api/pagos-recibos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PagoReciboController {

    /** Índice creado por docs/migrations/2026-11-25_01_pagos_recibos_voucher_unico.sql */
    private static final String INDICE_VOUCHER_UNICO = "ux_pagos_recibos_voucher";

    private final PagoReciboService pagoReciboService;

    @PostMapping
    public ResponseEntity<PagoReciboResponseDTO> registrar(@RequestBody PagoReciboRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoReciboService.registrar(request));
    }

    @GetMapping
    public ResponseEntity<List<PagoReciboResponseDTO>> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(pagoReciboService.listar(clienteId, desde, hasta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoReciboResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pagoReciboService.obtener(id));
    }

    @GetMapping("/cargos-pendientes/{clienteId}")
    public ResponseEntity<List<CargoPendienteDTO>> cargosPendientes(@PathVariable Long clienteId) {
        return ResponseEntity.ok(pagoReciboService.cargosPendientes(clienteId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        pagoReciboService.anular(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Errores en el formato del proyecto: { "error": "..." } =====

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException e) {
        return error(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, String>> reglaNegocio(ReglaNegocioException e) {
        return error(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, String>> conflicto(ConflictoException e) {
        return error(HttpStatus.CONFLICT, e);
    }

    /**
     * Carrera entre dos registros del mismo voucher: ambos pasan la validación del servicio
     * y el índice único ux_pagos_recibos_voucher rechaza el segundo INSERT.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException e) {
        String detalle = String.valueOf(e.getMostSpecificCause().getMessage());
        String mensaje = detalle.contains(INDICE_VOUCHER_UNICO)
                ? "El voucher ya fue registrado"
                : "La operación viola una restricción de integridad de datos";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", mensaje));
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status)
                .body(Map.of("error", Objects.toString(e.getMessage(), status.getReasonPhrase())));
    }
}
