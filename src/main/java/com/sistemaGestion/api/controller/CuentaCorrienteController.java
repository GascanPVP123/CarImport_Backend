package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.MovimientoRequest;
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

    public CuentaCorrienteController(
            CuentaCorrienteRepository cuentaCorrienteRepository,
            TiendaAliadaRepository tiendaAliadaRepository,
            UsuarioRepository usuarioRepository) {
        this.cuentaCorrienteRepository = cuentaCorrienteRepository;
        this.tiendaAliadaRepository = tiendaAliadaRepository;
        this.usuarioRepository = usuarioRepository;
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
            Object[] resumen = (Object[]) cuentaCorrienteRepository.findResumenGeneral();
            Map<String, Object> result = new HashMap<>();
            result.put("totalDebe", resumen[0]);
            result.put("totalHaber", resumen[1]);
            result.put("saldoNeto", resumen[2]);
            result.put("totalTiendas", resumen[3]);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== MOVIMIENTOS ==========

    @GetMapping("/movimientos/{tiendaId}")
    public ResponseEntity<List<CuentaCorriente>> obtenerMovimientos(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(cuentaCorrienteRepository.findByTiendaIdOrderByFechaDesc(tiendaId));
    }

    @PostMapping("/movimientos")
    public ResponseEntity<?> crearMovimiento(@RequestBody MovimientoRequest request) {
        try {
            TiendaAliada tienda = tiendaAliadaRepository.findById(request.getTiendaId())
                    .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

            Usuario usuario = usuarioRepository.findById(1L).orElse(null);

            CuentaCorriente movimiento = new CuentaCorriente();
            movimiento.setTienda(tienda);
            movimiento.setTipo(CuentaCorriente.Tipo.valueOf(request.getTipo()));
            movimiento.setOrigen(CuentaCorriente.Origen.valueOf(request.getOrigen()));
            movimiento.setConcepto(request.getConcepto());
            movimiento.setMonto(request.getMonto());
            movimiento.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
            movimiento.setReferencia(request.getReferencia());
            movimiento.setObservaciones(request.getObservaciones());
            movimiento.setUsuario(usuario);

            CuentaCorriente guardado = cuentaCorrienteRepository.save(movimiento);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}