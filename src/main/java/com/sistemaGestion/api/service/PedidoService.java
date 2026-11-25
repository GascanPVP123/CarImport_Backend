package com.sistemaGestion.api.service;

import com.sistemaGestion.api.dto.PedidoResponseDTO;
import com.sistemaGestion.api.exception.ConflictoException;
import com.sistemaGestion.api.exception.RecursoNoEncontradoException;
import com.sistemaGestion.api.exception.ReglaNegocioException;
import com.sistemaGestion.api.model.Cotizacion;
import com.sistemaGestion.api.model.Pedido;
import com.sistemaGestion.api.model.Pedido.EstadoPedido;
import com.sistemaGestion.api.repository.CotizacionRepository;
import com.sistemaGestion.api.repository.PedidoRepository;
import com.sistemaGestion.api.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pedidos de venta generados a partir de cotizaciones aprobadas. */
@Service
@RequiredArgsConstructor
public class PedidoService {

    static final String ESTADO_COTIZACION_APROBADA = "APROBADA";

    /** Transiciones de estado permitidas. ENTREGADO y ANULADO son finales. */
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.PENDIENTE, Set.of(EstadoPedido.CONFIRMADO, EstadoPedido.ANULADO),
            EstadoPedido.CONFIRMADO, Set.of(EstadoPedido.DESPACHADO, EstadoPedido.ANULADO),
            EstadoPedido.DESPACHADO, Set.of(EstadoPedido.ENTREGADO),
            EstadoPedido.ENTREGADO, Set.of(),
            EstadoPedido.ANULADO, Set.of()
    );

    private final PedidoRepository pedidoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final UsuarioActual usuarioActual;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listar(EstadoPedido estado) {
        List<Pedido> pedidos = estado == null
                ? pedidoRepository.findAllByOrderByFechaEmisionDesc()
                : pedidoRepository.findByEstadoOrderByFechaEmisionDesc(estado);
        return pedidos.stream().map(PedidoResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteIdOrderByFechaEmisionDesc(clienteId)
                .stream().map(PedidoResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO obtener(Long id) {
        return PedidoResponseDTO.from(buscar(id));
    }

    // ==================== CREACIÓN ====================

    /**
     * Crea el pedido de una cotización APROBADA. Una cotización solo genera un pedido.
     * El usuario del pedido es el autenticado (token JWT).
     */
    @Transactional
    public Pedido crearDesdeCotizacion(Long cotizacionId) {
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización no encontrada con ID: " + cotizacionId));

        if (!ESTADO_COTIZACION_APROBADA.equals(cotizacion.getEstado())) {
            throw new ReglaNegocioException("Solo se pueden convertir cotizaciones APROBADAS (estado actual: "
                    + cotizacion.getEstado() + ")");
        }
        pedidoRepository.findByCotizacionId(cotizacionId).ifPresent(existente -> {
            throw new ConflictoException("La cotización " + cotizacion.getNumero()
                    + " ya fue convertida en el pedido " + existente.getNumero());
        });

        Pedido pedido = new Pedido();
        Integer correlativo = pedidoRepository.findSiguienteCorrelativo();
        pedido.setCorrelativo(correlativo);
        pedido.setNumero(String.format("PED-%06d", correlativo));
        pedido.setCotizacion(cotizacion);
        pedido.setCliente(cotizacion.getCliente());
        pedido.setUsuario(usuarioActual.obtener());
        pedido.setFechaEmision(LocalDate.now());
        pedido.setCondicionPago(cotizacion.getCondicionPago());
        pedido.setMoneda(cotizacion.getMoneda());
        pedido.setTotal(cotizacion.getTotal());
        pedido.setObservaciones(cotizacion.getObservaciones());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        return pedidoRepository.save(pedido);
    }

    // ==================== ESTADO ====================

    @Transactional
    public PedidoResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        EstadoPedido destino = parseEstado(nuevoEstado);
        Pedido pedido = buscar(id);
        EstadoPedido actual = pedido.getEstado();

        if (actual == destino) {
            return PedidoResponseDTO.from(pedido);
        }
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(destino)) {
            throw new ReglaNegocioException("No se puede pasar un pedido de " + actual + " a " + destino);
        }
        pedido.setEstado(destino);
        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponseDTO anular(Long id) {
        return cambiarEstado(id, EstadoPedido.ANULADO.name());
    }

    // ==================== HELPERS ====================

    private Pedido buscar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con ID: " + id));
    }

    public static EstadoPedido parseEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("Debe indicar el estado");
        }
        try {
            return EstadoPedido.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Estado de pedido inválido: " + valor
                    + ". Valores permitidos: " + List.of(EstadoPedido.values()));
        }
    }
}
