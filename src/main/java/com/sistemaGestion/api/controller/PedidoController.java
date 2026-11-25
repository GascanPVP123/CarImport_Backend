package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.PedidoResponseDTO;
import com.sistemaGestion.api.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pedidos de venta. Se crean con POST /api/cotizaciones/{id}/convertir.
 *
 * GET /api/pedidos?estado=               listar (opcionalmente por estado)
 * GET /api/pedidos/{id}                  detalle
 * GET /api/pedidos/cliente/{clienteId}   pedidos de un cliente
 * PUT /api/pedidos/{id}/estado?estado=   cambiar estado (PENDIENTE→CONFIRMADO→DESPACHADO→ENTREGADO)
 * PUT /api/pedidos/{id}/anular           anular (desde PENDIENTE o CONFIRMADO)
 *
 * Errores en formato { "error": "..." } vía GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(pedidoService.listar(estado == null ? null : PedidoService.parseEstado(estado)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(pedidoService.listarPorCliente(clienteId));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, estado));
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<PedidoResponseDTO> anular(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.anular(id));
    }
}
