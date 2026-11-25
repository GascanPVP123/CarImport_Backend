package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.MovimientoRequest;
import com.sistemaGestion.api.dto.MovimientoResponseDTO;
import com.sistemaGestion.api.dto.ResumenCuentasDTO;
import com.sistemaGestion.api.dto.SaldoTiendaDTO;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cuentas-corrientes")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class CuentaCorrienteController {

    private final CuentaCorrienteRepository cuentaCorrienteRepository;
    private final TiendaAliadaRepository tiendaAliadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final NotaVentaRepository notaVentaRepository;
    private final PedidoRepository pedidoRepository;

    public CuentaCorrienteController(
            CuentaCorrienteRepository cuentaCorrienteRepository,
            TiendaAliadaRepository tiendaAliadaRepository,
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            NotaVentaRepository notaVentaRepository,
            PedidoRepository pedidoRepository) {
        this.cuentaCorrienteRepository = cuentaCorrienteRepository;
        this.tiendaAliadaRepository = tiendaAliadaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.notaVentaRepository = notaVentaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    // ========== SALDOS ==========

    @GetMapping("/saldos")
    public ResponseEntity<List<SaldoTiendaDTO>> obtenerSaldos() {
        try {
            List<VistaSaldoTienda> vistas = cuentaCorrienteRepository.findSaldosTiendas();
            List<SaldoTiendaDTO> saldos = vistas.stream().map(v -> {
                SaldoTiendaDTO dto = new SaldoTiendaDTO();
                dto.setTiendaId(v.getTiendaId());
                dto.setTiendaNombre(v.getTiendaNombre());
                dto.setTelefono(v.getTelefono());
                dto.setTotalDebe(v.getTotalDebe() != null ? v.getTotalDebe() : BigDecimal.ZERO);
                dto.setTotalHaber(v.getTotalHaber() != null ? v.getTotalHaber() : BigDecimal.ZERO);
                dto.setSaldoNeto(v.getSaldoNeto() != null ? v.getSaldoNeto() : BigDecimal.ZERO);
                dto.setEstadoSaldo(v.getEstadoSaldo());
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(saldos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/saldos/{tiendaId}")
    public ResponseEntity<?> obtenerSaldoPorTienda(@PathVariable Long tiendaId) {
        try {
            VistaSaldoTienda v = cuentaCorrienteRepository.findSaldoByTiendaId(tiendaId);
            if (v == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Tienda no encontrada"));
            }

            SaldoTiendaDTO dto = new SaldoTiendaDTO();
            dto.setTiendaId(v.getTiendaId());
            dto.setTiendaNombre(v.getTiendaNombre());
            dto.setTelefono(v.getTelefono());
            dto.setTotalDebe(v.getTotalDebe() != null ? v.getTotalDebe() : BigDecimal.ZERO);
            dto.setTotalHaber(v.getTotalHaber() != null ? v.getTotalHaber() : BigDecimal.ZERO);
            dto.setSaldoNeto(v.getSaldoNeto() != null ? v.getSaldoNeto() : BigDecimal.ZERO);
            dto.setEstadoSaldo(v.getEstadoSaldo());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumen() {
        try {
            var resumen = cuentaCorrienteRepository.findResumenGeneral();
            ResumenCuentasDTO dto = new ResumenCuentasDTO();
            dto.setTotalDebe(resumen.getTotalDebe());
            dto.setTotalHaber(resumen.getTotalHaber());
            dto.setSaldoNeto(resumen.getSaldoNeto());
            dto.setTotalTiendas(resumen.getTotalTiendas());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== MOVIMIENTOS ==========

    @GetMapping("/movimientos/{tiendaId}")
    public ResponseEntity<List<MovimientoResponseDTO>> obtenerMovimientos(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(cuentaCorrienteRepository.findByTiendaIdOrderByFechaDesc(tiendaId)
                .stream().map(MovimientoResponseDTO::from).toList());
    }

    @GetMapping("/movimientos/cliente/{clienteId}")
    public ResponseEntity<List<MovimientoResponseDTO>> obtenerMovimientosCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cuentaCorrienteRepository.findByClienteIdOrderByFechaDesc(clienteId)
                .stream().map(MovimientoResponseDTO::from).toList());
    }

    @PostMapping("/movimientos")
    public ResponseEntity<?> crearMovimiento(@RequestBody MovimientoRequest request) {
        try {
            if (request.getTiendaId() == null && request.getClienteId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe indicar tiendaId o clienteId"));
            }

            // TODO: tomar el usuario autenticado del SecurityContext en lugar del id fijo
            Usuario usuario = usuarioRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            CuentaCorriente movimiento = new CuentaCorriente();
            if (request.getTiendaId() != null) {
                movimiento.setTienda(tiendaAliadaRepository.findById(request.getTiendaId())
                        .orElseThrow(() -> new RuntimeException("Tienda no encontrada")));
            }
            if (request.getClienteId() != null) {
                movimiento.setCliente(clienteRepository.findById(request.getClienteId())
                        .orElseThrow(() -> new RuntimeException("Cliente no encontrado")));
            }
            if (request.getNotaVentaId() != null) {
                movimiento.setNotaVenta(notaVentaRepository.getReferenceById(request.getNotaVentaId()));
            }
            if (request.getPedidoId() != null) {
                movimiento.setPedido(pedidoRepository.getReferenceById(request.getPedidoId()));
            }
            movimiento.setTipo(CuentaCorriente.Tipo.valueOf(request.getTipo()));
            movimiento.setOrigen(CuentaCorriente.Origen.valueOf(request.getOrigen()));
            movimiento.setConcepto(request.getConcepto());
            movimiento.setMonto(request.getMonto());
            movimiento.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
            movimiento.setFechaVencimiento(request.getFechaVencimiento());
            movimiento.setReferencia(request.getReferencia());
            movimiento.setDocumentoRef(request.getDocumentoRef());
            movimiento.setObservaciones(request.getObservaciones());
            movimiento.setUsuario(usuario);

            CuentaCorriente guardado = cuentaCorrienteRepository.save(movimiento);
            return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoResponseDTO.from(guardado));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo u origen inválido: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}