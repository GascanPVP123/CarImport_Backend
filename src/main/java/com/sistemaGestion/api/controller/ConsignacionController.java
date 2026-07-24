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

    public ConsignacionController(
            ConsignacionRepository consignacionRepository,
            TiendaAliadaRepository tiendaAliadaRepository,
            ProductoRepository productoRepository,
            VentaConsignacionRepository ventaConsignacionRepository,
            DevolucionConsignacionRepository devolucionConsignacionRepository,
            UsuarioRepository usuarioRepository) {
        this.consignacionRepository = consignacionRepository;
        this.tiendaAliadaRepository = tiendaAliadaRepository;
        this.productoRepository = productoRepository;
        this.ventaConsignacionRepository = ventaConsignacionRepository;
        this.devolucionConsignacionRepository = devolucionConsignacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<List<Consignacion>> obtenerTodos() {
        return ResponseEntity.ok(consignacionRepository.findAll());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<Consignacion>> obtenerActivas() {
        return ResponseEntity.ok(consignacionRepository.findConsignacionesActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        Consignacion consignacion = consignacionRepository.findByIdWithDetalles(id);
        if (consignacion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Consignación no encontrada"));
        }
        return ResponseEntity.ok(consignacion);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ConsignacionRequest request) {
        try {
            // Validar tienda
            TiendaAliada tienda = tiendaAliadaRepository.findById(request.getTiendaId())
                    .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

            // Generar número de consignación
            String ultimoNumero = consignacionRepository.findUltimoNumero();
            int correlativo = Integer.parseInt(ultimoNumero.substring(5)) + 1;
            String numero = String.format("CONS-%06d", correlativo);

            // Obtener usuario admin por defecto
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

                DetalleConsignacion detalle = new DetalleConsignacion();
                detalle.setConsignacion(consignacion);
                detalle.setProducto(producto);
                detalle.setCantidadEnviada(detalleDTO.getCantidad());
                detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario());
                detalle.setSubtotal(detalleDTO.getPrecioUnitario().multiply(new BigDecimal(detalleDTO.getCantidad())));

                consignacion.getDetalles().add(detalle);
                valorTotal = valorTotal.add(detalle.getSubtotal());
            }

            consignacion.setValorTotal(valorTotal);
            Consignacion guardada = consignacionRepository.save(consignacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/venta")
    public ResponseEntity<?> registrarVenta(@PathVariable Long id, @RequestBody VentaConsignacionRequest request) {
        try {
            Consignacion consignacion = consignacionRepository.findByIdWithDetalles(id);
            if (consignacion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Consignación no encontrada"));
            }

            Usuario usuario = usuarioRepository.findById(1L).orElse(null);
            BigDecimal totalVenta = BigDecimal.ZERO;

            for (VentaConsignacionRequest.DetalleVentaDTO ventaDTO : request.getDetalles()) {
                DetalleConsignacion detalle = consignacion.getDetalles().stream()
                        .filter(d -> d.getId().equals(ventaDTO.getDetalleConsignacionId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));

                // Validar cantidad
                int pendiente = detalle.getCantidadPendiente();
                if (ventaDTO.getCantidad() > pendiente) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Cantidad excede el pendiente. Pendiente: " + pendiente));
                }

                BigDecimal precio = ventaDTO.getPrecioUnitario() != null ?
                        ventaDTO.getPrecioUnitario() : detalle.getPrecioUnitario();
                BigDecimal subtotal = precio.multiply(new BigDecimal(ventaDTO.getCantidad()));

                // Registrar venta
                VentaConsignacion venta = new VentaConsignacion();
                venta.setConsignacion(consignacion);
                venta.setDetalleConsignacion(detalle);
                venta.setCantidad(ventaDTO.getCantidad());
                venta.setPrecioUnitario(precio);
                venta.setTotal(subtotal);
                venta.setFechaVenta(request.getFechaVenta() != null ? request.getFechaVenta() : LocalDate.now());
                venta.setComisionPorcentaje(request.getComisionPorcentaje() != null ? request.getComisionPorcentaje() : BigDecimal.ZERO);
                venta.setComisionMonto(subtotal.multiply(venta.getComisionPorcentaje()).divide(new BigDecimal(100)));
                venta.setMontoAPagar(subtotal.subtract(venta.getComisionMonto()));
                venta.setObservaciones(request.getObservaciones());
                venta.setUsuario(usuario);
                ventaConsignacionRepository.save(venta);

                // Actualizar detalle
                detalle.setCantidadVendida(detalle.getCantidadVendida() + ventaDTO.getCantidad());
                totalVenta = totalVenta.add(subtotal);
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

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/devolucion")
    public ResponseEntity<?> registrarDevolucion(@PathVariable Long id, @RequestBody DevolucionConsignacionRequest request) {
        try {
            Consignacion consignacion = consignacionRepository.findByIdWithDetalles(id);
            if (consignacion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Consignación no encontrada"));
            }

            Usuario usuario = usuarioRepository.findById(1L).orElse(null);
            BigDecimal totalDevuelto = BigDecimal.ZERO;

            for (DevolucionConsignacionRequest.DetalleDevolucionDTO devolucionDTO : request.getDetalles()) {
                DetalleConsignacion detalle = consignacion.getDetalles().stream()
                        .filter(d -> d.getId().equals(devolucionDTO.getDetalleConsignacionId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));

                int pendiente = detalle.getCantidadPendiente();
                if (devolucionDTO.getCantidad() > pendiente) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Cantidad excede el pendiente"));
                }

                BigDecimal subtotal = detalle.getPrecioUnitario()
                        .multiply(new BigDecimal(devolucionDTO.getCantidad()));

                // Registrar devolución
                DevolucionConsignacion devolucion = new DevolucionConsignacion();
                devolucion.setConsignacion(consignacion);
                devolucion.setDetalleConsignacion(detalle);
                devolucion.setCantidad(devolucionDTO.getCantidad());
                devolucion.setMotivo(request.getMotivo());
                devolucion.setFechaDevolucion(request.getFechaDevolucion() != null ?
                        request.getFechaDevolucion() : LocalDate.now());
                devolucion.setUsuario(usuario);
                devolucionConsignacionRepository.save(devolucion);

                // Actualizar detalle
                detalle.setCantidadDevuelta(detalle.getCantidadDevuelta() + devolucionDTO.getCantidad());
                detalle.setDevuelto(true);
                totalDevuelto = totalDevuelto.add(subtotal);
            }

            // Actualizar consignación
            consignacion.setValorDevuelto(consignacion.getValorDevuelto().add(totalDevuelto));

            boolean todosProcesados = consignacion.getDetalles().stream()
                    .allMatch(d -> d.getCantidadPendiente() == 0);
            if (todosProcesados && consignacion.getValorVendido().compareTo(BigDecimal.ZERO) > 0) {
                consignacion.setEstado(Consignacion.EstadoConsignacion.COMPLETADA);
            } else {
                consignacion.setEstado(Consignacion.EstadoConsignacion.DEVUELTA);
            }
            consignacion.setFechaDevolucion(request.getFechaDevolucion());

            consignacionRepository.save(consignacion);
            return ResponseEntity.ok(consignacion);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!consignacionRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Consignación no encontrada"));
        }
        consignacionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Consignación eliminada"));
    }
}