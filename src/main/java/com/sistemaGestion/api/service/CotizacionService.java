package com.sistemaGestion.api.service;

import com.sistemaGestion.api.dto.CotizacionRequest;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Cotizacion> listar() {
        return cotizacionRepository.findAll();
    }

    public Cotizacion obtener(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));
    }

    @Transactional
    public Cotizacion crear(CotizacionRequest request, String username) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setUsuario(usuario);
        cotizacion.setFechaEmision(LocalDate.now());
        cotizacion.setFechaVencimiento(request.getFechaVencimiento());
        cotizacion.setCondicionPago(request.getCondicionPago());
        cotizacion.setMoneda(request.getMoneda());
        cotizacion.setObservaciones(request.getObservaciones());
        cotizacion.setEstado("BORRADOR");

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;

        for (CotizacionRequest.DetalleRequest det : request.getDetalles()) {
            Producto producto = productoRepository.findById(det.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            DetalleCotizacion detalle = new DetalleCotizacion();
            detalle.setCotizacion(cotizacion);
            detalle.setProducto(producto);
            detalle.setCodigo(producto.getCodigoSku());
            detalle.setDescripcion(producto.getNombre());
            detalle.setCantidad(det.getCantidad());
            detalle.setUnidad(det.getUnidad() != null ? det.getUnidad() : "unidad");
            detalle.setPrecioUnitario(det.getPrecioUnitario());
            detalle.setDescuento(det.getDescuento() != null ? det.getDescuento() : BigDecimal.ZERO);

            BigDecimal importe = det.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(det.getCantidad()))
                    .subtract(detalle.getDescuento());
            detalle.setImporte(importe);

            subtotal = subtotal.add(det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad())));
            totalDescuento = totalDescuento.add(detalle.getDescuento());

            cotizacion.getDetalles().add(detalle);
        }

        BigDecimal total = subtotal.subtract(totalDescuento);
        cotizacion.setSubtotal(subtotal);
        cotizacion.setDescuento(totalDescuento);
        cotizacion.setIgv(BigDecimal.ZERO);
        cotizacion.setTotal(total);

        cotizacion.setCorrelativo((int) (cotizacionRepository.count() + 1));
        cotizacion.setNumero("COT-" + String.format("%06d", cotizacion.getCorrelativo()));

        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public Cotizacion actualizar(Long id, CotizacionRequest request) {
        Cotizacion cotizacion = obtener(id);

        cotizacion.getDetalles().clear();
        cotizacion.setFechaVencimiento(request.getFechaVencimiento());
        cotizacion.setCondicionPago(request.getCondicionPago());
        cotizacion.setMoneda(request.getMoneda());
        cotizacion.setObservaciones(request.getObservaciones());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;

        for (CotizacionRequest.DetalleRequest det : request.getDetalles()) {
            Producto producto = productoRepository.findById(det.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            DetalleCotizacion detalle = new DetalleCotizacion();
            detalle.setCotizacion(cotizacion);
            detalle.setProducto(producto);
            detalle.setCodigo(producto.getCodigoSku());
            detalle.setDescripcion(producto.getNombre());
            detalle.setCantidad(det.getCantidad());
            detalle.setUnidad(det.getUnidad() != null ? det.getUnidad() : "unidad");
            detalle.setPrecioUnitario(det.getPrecioUnitario());
            detalle.setDescuento(det.getDescuento() != null ? det.getDescuento() : BigDecimal.ZERO);

            BigDecimal importe = det.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(det.getCantidad()))
                    .subtract(detalle.getDescuento());
            detalle.setImporte(importe);

            subtotal = subtotal.add(det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad())));
            totalDescuento = totalDescuento.add(detalle.getDescuento());
            cotizacion.getDetalles().add(detalle);
        }

        BigDecimal total = subtotal.subtract(totalDescuento);
        cotizacion.setSubtotal(subtotal);
        cotizacion.setDescuento(totalDescuento);
        cotizacion.setIgv(BigDecimal.ZERO);
        cotizacion.setTotal(total);

        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public Cotizacion cambiarEstado(Long id, String nuevoEstado) {
        Cotizacion cotizacion = obtener(id);
        cotizacion.setEstado(nuevoEstado);
        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public Long convertirAPedido(Long id) {
        Cotizacion cotizacion = obtener(id);
        if (!cotizacion.getEstado().equals("APROBADA")) {
            throw new RuntimeException("Solo se pueden convertir cotizaciones APROBADAS");
        }
        return 1L;
    }

    @Transactional
    public Cotizacion duplicar(Long id) {
        Cotizacion original = obtener(id);

        Cotizacion duplicada = new Cotizacion();
        duplicada.setCliente(original.getCliente());
        duplicada.setUsuario(original.getUsuario());
        duplicada.setFechaEmision(LocalDate.now());
        duplicada.setFechaVencimiento(original.getFechaVencimiento());
        duplicada.setCondicionPago(original.getCondicionPago());
        duplicada.setMoneda(original.getMoneda());
        duplicada.setObservaciones(original.getObservaciones());
        duplicada.setEstado("BORRADOR");

        for (DetalleCotizacion det : original.getDetalles()) {
            DetalleCotizacion nuevoDetalle = new DetalleCotizacion();
            nuevoDetalle.setCotizacion(duplicada);
            nuevoDetalle.setProducto(det.getProducto());
            nuevoDetalle.setCodigo(det.getCodigo());
            nuevoDetalle.setDescripcion(det.getDescripcion());
            nuevoDetalle.setCantidad(det.getCantidad());
            nuevoDetalle.setUnidad(det.getUnidad());
            nuevoDetalle.setPrecioUnitario(det.getPrecioUnitario());
            nuevoDetalle.setDescuento(det.getDescuento());
            nuevoDetalle.setImporte(det.getImporte());
            duplicada.getDetalles().add(nuevoDetalle);
        }

        duplicada.setSubtotal(original.getSubtotal());
        duplicada.setDescuento(original.getDescuento());
        duplicada.setIgv(BigDecimal.ZERO);
        duplicada.setTotal(original.getTotal());
        duplicada.setCorrelativo((int) (cotizacionRepository.count() + 1));
        duplicada.setNumero("COT-" + String.format("%06d", duplicada.getCorrelativo()));

        return cotizacionRepository.save(duplicada);
    }

    @Transactional
    public void eliminar(Long id) {
        Cotizacion cotizacion = obtener(id);
        if (!cotizacion.getEstado().equals("BORRADOR")) {
            throw new RuntimeException("Solo se pueden eliminar cotizaciones en estado BORRADOR");
        }
        cotizacionRepository.deleteById(id);
    }
}