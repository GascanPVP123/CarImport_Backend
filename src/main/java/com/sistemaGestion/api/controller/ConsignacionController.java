package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.ConsignacionRequest;
import com.sistemaGestion.api.dto.DevolucionConsignacionRequest;
import com.sistemaGestion.api.dto.VentaConsignacionRequest;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consignaciones")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ConsignacionController {

    private final ConsignacionRepository consignacionRepository;
    private final TiendaAliadaRepository tiendaAliadaRepository;
    private final ProductoRepository productoRepository;
    private final VentaConsignacionRepository ventaConsignacionRepository;
    private final DevolucionConsignacionRepository devolucionConsignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaCorrienteRepository cuentaCorrienteRepository;

    public ConsignacionController(
            ConsignacionRepository consignacionRepository,
            TiendaAliadaRepository tiendaAliadaRepository,
            ProductoRepository productoRepository,
            VentaConsignacionRepository ventaConsignacionRepository,
            DevolucionConsignacionRepository devolucionConsignacionRepository,
            UsuarioRepository usuarioRepository,
            CuentaCorrienteRepository cuentaCorrienteRepository) {
        this.consignacionRepository = consignacionRepository;
        this.tiendaAliadaRepository = tiendaAliadaRepository;
        this.productoRepository = productoRepository;
        this.ventaConsignacionRepository = ventaConsignacionRepository;
        this.devolucionConsignacionRepository = devolucionConsignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuentaCorrienteRepository = cuentaCorrienteRepository;
    }

    // ==================== CONSULTAS ====================

    @GetMapping
    public ResponseEntity<List<Consignacion>> obtenerTodos() {
        try {
            return ResponseEntity.ok(consignacionRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/activas")
    public ResponseEntity<List<Consignacion>> obtenerActivas() {
        try {
            return ResponseEntity.ok(consignacionRepository.findConsignacionesActivas());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            Consignacion consignacion = consignacionRepository.findByIdWithDetalles(id);
            if (consignacion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Consignación no encontrada con ID: " + id));
            }
            return ResponseEntity.ok(consignacion);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener consignación: " + e.getMessage()));
        }
    }

    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<List<Consignacion>> obtenerPorTienda(@PathVariable Long tiendaId) {
        try {
            return ResponseEntity.ok(consignacionRepository.findByTiendaId(tiendaId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== CREAR CONSIGNACIÓN ====================

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ConsignacionRequest request) {
        try {
            // Validar tienda
            TiendaAliada tienda = tiendaAliadaRepository.findById(request.getTiendaId())
                    .orElseThrow(() -> new RuntimeException("Tienda no encontrada con ID: " + request.getTiendaId()));

            // Validar detalles
            if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Debe agregar al menos un producto"));
            }

            // Generar número de consignación
            String ultimoNumero = consignacionRepository.findUltimoNumero();
            int correlativo = 1;
            if (ultimoNumero != null && ultimoNumero.startsWith("CONS-")) {
                correlativo = Integer.parseInt(ultimoNumero.substring(5)) + 1;
            }
            String numero = String.format("CONS-%06d", correlativo);

            // Obtener usuario
            Usuario usuario = usuarioRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Crear consignación
            Consignacion consignacion = new Consignacion();
            consignacion.setNumeroConsignacion(numero);
            consignacion.setTienda(tienda);
            consignacion.setFechaEnvio(request.getFechaEnvio() != null ? request.getFechaEnvio() : LocalDate.now());
            consignacion.setObservaciones(request.getObservaciones());
            consignacion.setEstado(Consignacion.EstadoConsignacion.ENVIADA);
            consignacion.setUsuario(usuario);

            BigDecimal valorTotal = BigDecimal.ZERO;

            // Agregar detalles
            for (ConsignacionRequest.DetalleConsignacionDTO detalleDTO : request.getDetalles()) {
                Producto producto = productoRepository.findById(detalleDTO.getProductoId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleDTO.getProductoId()));

                if (detalleDTO.getCantidad() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "La cantidad debe ser mayor a 0"));
                }

                DetalleConsignacion detalle = new DetalleConsignacion();
                detalle.setConsignacion(consignacion);
                detalle.setProducto(producto);
                detalle.setCantidadEnviada(detalleDTO.getCantidad());
                detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario() != null ?
                        detalleDTO.getPrecioUnitario() : BigDecimal.ZERO);
                detalle.setSubtotal(detalle.getPrecioUnitario().multiply(new BigDecimal(detalleDTO.getCantidad())));

                consignacion.getDetalles().add(detalle);
                valorTotal = valorTotal.add(detalle.getSubtotal());
            }

            consignacion.setValorTotal(valorTotal);
            Consignacion guardada = consignacionRepository.save(consignacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear consignación: " + e.getMessage()));
        }
    }

    // ==================== REGISTRAR VENTA ====================

    @PostMapping("/{id}/venta")
    public ResponseEntity<?> registrarVenta(@PathVariable Long id, @RequestBody VentaConsignacionRequest request) {
        try {
            Consignacion consignacion = consignacionRepository.findByIdWithDetalles(id);
            if (consignacion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Consignación no encontrada con ID: " + id));
            }

            // Validar estado
            if (consignacion.getEstado() != Consignacion.EstadoConsignacion.ENVIADA &&
                    consignacion.getEstado() != Consignacion.EstadoConsignacion.PARCIAL) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "La consignación está en estado " + consignacion.getEstado() +
                                " y no se pueden registrar ventas"));
            }

            Usuario usuario = usuarioRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            LocalDate fechaVenta = request.getFechaVenta() != null ? request.getFechaVenta() : LocalDate.now();
            BigDecimal comisionPorcentaje = request.getComisionPorcentaje() != null ?
                    request.getComisionPorcentaje() : BigDecimal.ZERO;
            BigDecimal totalVenta = BigDecimal.ZERO;

            for (VentaConsignacionRequest.DetalleVentaDTO ventaDTO : request.getDetalles()) {
                DetalleConsignacion detalle = consignacion.getDetalles().stream()
                        .filter(d -> d.getId().equals(ventaDTO.getDetalleConsignacionId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado: " +
                                ventaDTO.getDetalleConsignacionId()));

                // Validar cantidad pendiente
                int pendiente = detalle.getCantidadPendiente();
                if (ventaDTO.getCantidad() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "La cantidad a vender debe ser mayor a 0"));
                }
                if (ventaDTO.getCantidad() > pendiente) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Cantidad excede el pendiente. Pendiente: " + pendiente));
                }

                BigDecimal precio = ventaDTO.getPrecioUnitario() != null ?
                        ventaDTO.getPrecioUnitario() : detalle.getPrecioUnitario();
                BigDecimal subtotalVenta = precio.multiply(new BigDecimal(ventaDTO.getCantidad()));

                // Registrar venta
                VentaConsignacion venta = new VentaConsignacion();
                venta.setConsignacion(consignacion);
                venta.setDetalleConsignacion(detalle);
                venta.setCantidad(ventaDTO.getCantidad());
                venta.setPrecioUnitario(precio);
                venta.setTotal(subtotalVenta);
                venta.setFechaVenta(fechaVenta);
                venta.setComisionPorcentaje(comisionPorcentaje);

                if (comisionPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal comisionMonto = subtotalVenta.multiply(comisionPorcentaje).divide(new BigDecimal(100));
                    venta.setComisionMonto(comisionMonto);
                    venta.setMontoAPagar(subtotalVenta.subtract(comisionMonto));
                } else {
                    venta.setComisionMonto(BigDecimal.ZERO);
                    venta.setMontoAPagar(subtotalVenta);
                }

                venta.setObservaciones(request.getObservaciones());
                venta.setUsuario(usuario);
                ventaConsignacionRepository.save(venta);

                // Actualizar detalle
                detalle.setCantidadVendida(detalle.getCantidadVendida() + ventaDTO.getCantidad());
                totalVenta = totalVenta.add(subtotalVenta);

                // 🟢 GENERAR DÉBITO EN CUENTA CORRIENTE (la tienda te debe)
                CuentaCorriente debito = new CuentaCorriente();
                debito.setTienda(consignacion.getTienda());
                debito.setTipo(CuentaCorriente.Tipo.DEBITO);
                debito.setOrigen(CuentaCorriente.Origen.CONSIGNACION_VENTA);
                debito.setConcepto("Venta " + ventaDTO.getCantidad() + " und. - " +
                        detalle.getProducto().getNombre() + " (" + consignacion.getNumeroConsignacion() + ")");
                debito.setConsignacion(consignacion);
                debito.setMonto(subtotalVenta);
                debito.setFecha(fechaVenta);
                debito.setUsuario(usuario);
                cuentaCorrienteRepository.save(debito);

                // 🟢 GENERAR CRÉDITO EN CUENTA CORRIENTE (comisión de la tienda)
                if (comisionPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal comisionMonto = subtotalVenta.multiply(comisionPorcentaje).divide(new BigDecimal(100));
                    CuentaCorriente credito = new CuentaCorriente();
                    credito.setTienda(consignacion.getTienda());
                    credito.setTipo(CuentaCorriente.Tipo.CREDITO);
                    credito.setOrigen(CuentaCorriente.Origen.CONSIGNACION_COMISION);
                    credito.setConcepto("Comisión " + comisionPorcentaje + "% - " +
                            consignacion.getNumeroConsignacion());
                    credito.setConsignacion(consignacion);
                    credito.setMonto(comisionMonto);
                    credito.setFecha(fechaVenta);
                    credito.setUsuario(usuario);
                    cuentaCorrienteRepository.save(credito);
                }
            }

            // Actualizar consignación
            consignacion.setValorVendido(consignacion.getValorVendido().add(totalVenta));

            // Cambiar estado
            boolean todosVendidos = consignacion.getDetalles().stream()
                    .allMatch(d -> d.getCantidadPendiente() == 0);
            if (todosVendidos) {
                consignacion.setEstado(Consignacion.EstadoConsignacion.COMPLETADA);
            } else {
                consignacion.setEstado(Consignacion.EstadoConsignacion.PARCIAL);
            }

            consignacionRepository.save(consignacion);
            return ResponseEntity.ok(consignacion);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar venta: " + e.getMessage()));
        }
    }

    // ==================== REGISTRAR DEVOLUCIÓN ====================

    @PostMapping("/{id}/devolucion")
    public ResponseEntity<?> registrarDevolucion(@PathVariable Long id, @RequestBody DevolucionConsignacionRequest request) {
        try {
            Consignacion consignacion = consignacionRepository.findByIdWithDetalles(id);
            if (consignacion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Consignación no encontrada con ID: " + id));
            }

            // Validar estado
            if (consignacion.getEstado() != Consignacion.EstadoConsignacion.ENVIADA &&
                    consignacion.getEstado() != Consignacion.EstadoConsignacion.PARCIAL) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "La consignación está en estado " + consignacion.getEstado() +
                                " y no se pueden registrar devoluciones"));
            }

            Usuario usuario = usuarioRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            LocalDate fechaDevolucion = request.getFechaDevolucion() != null ?
                    request.getFechaDevolucion() : LocalDate.now();
            BigDecimal totalDevuelto = BigDecimal.ZERO;

            for (DevolucionConsignacionRequest.DetalleDevolucionDTO devolucionDTO : request.getDetalles()) {
                DetalleConsignacion detalle = consignacion.getDetalles().stream()
                        .filter(d -> d.getId().equals(devolucionDTO.getDetalleConsignacionId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado: " +
                                devolucionDTO.getDetalleConsignacionId()));

                // Validar cantidad
                int pendiente = detalle.getCantidadPendiente();
                if (devolucionDTO.getCantidad() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "La cantidad a devolver debe ser mayor a 0"));
                }
                if (devolucionDTO.getCantidad() > pendiente) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Cantidad excede el pendiente. Pendiente: " + pendiente));
                }

                BigDecimal subtotalDevuelto = detalle.getPrecioUnitario()
                        .multiply(new BigDecimal(devolucionDTO.getCantidad()));

                // Registrar devolución
                DevolucionConsignacion devolucion = new DevolucionConsignacion();
                devolucion.setConsignacion(consignacion);
                devolucion.setDetalleConsignacion(detalle);
                devolucion.setCantidad(devolucionDTO.getCantidad());
                devolucion.setMotivo(request.getMotivo());
                devolucion.setFechaDevolucion(fechaDevolucion);
                devolucion.setUsuario(usuario);
                devolucionConsignacionRepository.save(devolucion);

                // Actualizar detalle
                detalle.setCantidadDevuelta(detalle.getCantidadDevuelta() + devolucionDTO.getCantidad());
                detalle.setDevuelto(true);
                totalDevuelto = totalDevuelto.add(subtotalDevuelto);

                // 🟢 GENERAR CRÉDITO EN CUENTA CORRIENTE (devolución = ajuste a favor de la tienda)
                CuentaCorriente credito = new CuentaCorriente();
                credito.setTienda(consignacion.getTienda());
                credito.setTipo(CuentaCorriente.Tipo.CREDITO);
                credito.setOrigen(CuentaCorriente.Origen.CONSIGNACION_DEVOLUCION);
                credito.setConcepto("Devolución " + devolucionDTO.getCantidad() + " und. - " +
                        detalle.getProducto().getNombre() + " (" + consignacion.getNumeroConsignacion() + ")");
                credito.setConsignacion(consignacion);
                credito.setMonto(subtotalDevuelto);
                credito.setFecha(fechaDevolucion);
                credito.setObservaciones(request.getMotivo());
                credito.setUsuario(usuario);
                cuentaCorrienteRepository.save(credito);
            }

            // Actualizar consignación
            consignacion.setValorDevuelto(consignacion.getValorDevuelto().add(totalDevuelto));
            consignacion.setFechaDevolucion(fechaDevolucion);

            // Cambiar estado
            boolean todosProcesados = consignacion.getDetalles().stream()
                    .allMatch(d -> d.getCantidadPendiente() == 0);
            if (todosProcesados) {
                if (consignacion.getValorVendido().compareTo(BigDecimal.ZERO) > 0) {
                    consignacion.setEstado(Consignacion.EstadoConsignacion.COMPLETADA);
                } else {
                    consignacion.setEstado(Consignacion.EstadoConsignacion.DEVUELTA);
                }
            } else if (consignacion.getValorDevuelto().compareTo(BigDecimal.ZERO) > 0) {
                consignacion.setEstado(Consignacion.EstadoConsignacion.DEVUELTA);
            }

            consignacionRepository.save(consignacion);
            return ResponseEntity.ok(consignacion);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar devolución: " + e.getMessage()));
        }
    }

    // ==================== ELIMINAR ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            if (!consignacionRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Consignación no encontrada con ID: " + id));
            }
            consignacionRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Consignación eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar consignación: " + e.getMessage()));
        }
    }
}