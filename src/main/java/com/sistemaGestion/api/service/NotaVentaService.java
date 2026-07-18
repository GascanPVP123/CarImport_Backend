package com.sistemaGestion.api.service;

import com.sistemaGestion.api.dto.NotaVentaRequest;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class NotaVentaService {

    private final NotaVentaRepository notaVentaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionRepository cotizacionRepository;

    @Transactional
    public NotaVenta crear(NotaVentaRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Usuario usuario = usuarioRepository.findByUsername("admin")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        NotaVenta nota = new NotaVenta();
        nota.setCliente(cliente);
        nota.setUsuario(usuario);
        nota.setFechaEmision(LocalDate.now());
        nota.setCondicionPago(request.getCondicionPago());
        nota.setMoneda(request.getMoneda());

        if (request.getCotizacionId() != null) {
            Cotizacion cotizacion = cotizacionRepository.findById(request.getCotizacionId()).orElse(null);
            nota.setCotizacion(cotizacion);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (NotaVentaRequest.DetalleRequest det : request.getDetalles()) {
            Producto producto = productoRepository.findById(det.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // Descontar stock
            if (producto.getStock() < det.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para " + producto.getNombre());
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

            // Importe = precio * cantidad - descuento (SIN IGV adicional)
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
}