package com.sistemaGestion.api.service;

import com.sistemaGestion.api.dto.NotaVentaRequest;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotaVentaService {

    private final NotaVentaRepository notaVentaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionRepository cotizacionRepository;

    /**
     * Crear una nueva Nota de Venta y descontar stock
     */
    @Transactional
    public NotaVenta crear(NotaVentaRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Usuario usuario = usuarioRepository.findByUsername("admin")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        NotaVenta nota = new NotaVenta();
        nota.setCliente(cliente);
        nota.setUsuario(usuario);
        nota.setFechaEmision(LocalDateTime.now());
        nota.setCondicionPago(request.getCondicionPago());
        nota.setMoneda(request.getMoneda());
        nota.setEstado("EMITIDA");

        if (request.getCotizacionId() != null) {
            Cotizacion cotizacion = cotizacionRepository.findById(request.getCotizacionId()).orElse(null);
            nota.setCotizacion(cotizacion);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (NotaVentaRequest.DetalleRequest det : request.getDetalles()) {
            Producto producto = productoRepository.findById(det.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // Validar y Descontar stock
            if (producto.getStock() < det.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
            }
            producto.setStock(producto.getStock() - det.getCantidad());
            productoRepository.save(producto);

            DetalleNotaVenta detalle = new DetalleNotaVenta();
            detalle.setNotaVenta(nota);
            detalle.setProducto(producto);
            detalle.setCodigo(producto.getCodigoSku());
            detalle.setDescripcion(producto.getNombre());
            detalle.setCantidad(det.getCantidad());
            detalle.setUnidad(det.getUnidad() != null ? det.getUnidad() : "unidad");
            detalle.setPrecioUnitario(det.getPrecioUnitario());
            detalle.setDescuento(det.getDescuento() != null ? det.getDescuento() : BigDecimal.ZERO);

            // Importe = precio * cantidad - descuento
            BigDecimal importe = det.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(det.getCantidad()))
                    .subtract(detalle.getDescuento());
            detalle.setImporte(importe);

            total = total.add(importe);
            nota.getDetalles().add(detalle);
        }

        nota.setTotal(total);
        nota.setCorrelativo((int) (notaVentaRepository.count() + 1));
        nota.setNumero("NV-" + String.format("%06d", nota.getCorrelativo()));

        return notaVentaRepository.save(nota);
    }

    /**
     * Obtener el historial de todas las notas de venta ordenadas desde la más reciente
     */
    @Transactional(readOnly = true)
    public List<NotaVenta> obtenerHistorial() {
        return notaVentaRepository.findAllByOrderByFechaEmisionDesc();
    }

    /**
     * Obtener una nota de venta específica por ID
     */
    @Transactional(readOnly = true)
    public NotaVenta obtenerPorId(Long id) {
        return notaVentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota de venta no encontrada con ID: " + id));
    }

    /**
     * Anular Nota de Venta y Restaurar el stock de los productos
     */
    @Transactional
    public void anular(Long id) {
        NotaVenta nota = obtenerPorId(id);

        if ("ANULADA".equals(nota.getEstado())) {
            throw new RuntimeException("La nota de venta ya se encuentra anulada.");
        }

        // Restablecer el stock a los productos
        for (DetalleNotaVenta detalle : nota.getDetalles()) {
            Producto producto = detalle.getProducto();
            if (producto != null) {
                producto.setStock(producto.getStock() + detalle.getCantidad());
                productoRepository.save(producto);
            }
        }

        nota.setEstado("ANULADA");
        notaVentaRepository.save(nota);
    }
}