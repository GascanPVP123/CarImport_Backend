package com.sistemaGestion.api.service;

import com.sistemaGestion.api.dto.CargoPendienteDTO;
import com.sistemaGestion.api.dto.PagoReciboRequest;
import com.sistemaGestion.api.dto.PagoReciboResponseDTO;
import com.sistemaGestion.api.exception.ConflictoException;
import com.sistemaGestion.api.exception.RecursoNoEncontradoException;
import com.sistemaGestion.api.exception.ReglaNegocioException;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.model.CuentaCorriente.EstadoDocumento;
import com.sistemaGestion.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tesorería: registro de pagos/vouchers de clientes y su aplicación a cargos de cuenta corriente.
 *
 * Reglas:
 * - Un voucher (numero_operacion_bancaria + cuenta_recaudadora) solo se registra una vez.
 * - Sin asignaciones explícitas, el importe se distribuye automáticamente (FIFO por vencimiento)
 *   entre los cargos DEBITO pendientes del cliente.
 * - Con asignaciones explícitas, cada una debe ser de un cargo DEBITO del mismo cliente y no
 *   superar su saldo pendiente; la suma no puede superar el importe del pago.
 * - El importe no asignado queda como saldo a favor del pago (montoSinAsignar).
 * - Cada cargo tocado pasa a 'Parcial' o 'Pagado'; al anular el pago se recalculan.
 * - El pago genera un movimiento CREDITO / PAGO_RECIBIDO en cuentas_corrientes
 *   (referencia "PAGO-{id}") para que el saldo del cliente refleje el cobro.
 */
@Service
@RequiredArgsConstructor
public class PagoReciboService {

    private static final String PREFIJO_REFERENCIA = "PAGO-";

    private final PagoReciboRepository pagoReciboRepository;
    private final PagoAsignacionRepository pagoAsignacionRepository;
    private final CuentaCorrienteRepository cuentaCorrienteRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    // =====================================================================
    // Registro
    // =====================================================================

    @Transactional
    public PagoReciboResponseDTO registrar(PagoReciboRequest request) {
        if (request == null || request.getClienteId() == null) {
            throw new ReglaNegocioException("El cliente es obligatorio");
        }
        BigDecimal importe = normalizarMonto(request.getImporteTotal(), "importeTotal");

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cliente no encontrado: " + request.getClienteId()));

        String numeroOperacion = textoONull(request.getNumeroOperacionBancaria());
        String cuentaRecaudadora = textoONull(request.getCuentaRecaudadora());
        if (numeroOperacion != null && pagoReciboRepository
                .existsByNumeroOperacionBancariaAndCuentaRecaudadora(numeroOperacion, cuentaRecaudadora)) {
            throw new ConflictoException("El voucher " + numeroOperacion
                    + (cuentaRecaudadora != null ? " de la cuenta " + cuentaRecaudadora : "")
                    + " ya fue registrado");
        }

        Usuario usuario = usuarioActual();

        PagoRecibo pago = new PagoRecibo();
        pago.setCliente(cliente);
        pago.setFechaAbono(request.getFechaAbono() != null ? request.getFechaAbono() : LocalDateTime.now());
        pago.setMedioPago(textoONull(request.getMedioPago()));
        pago.setNumeroOperacionBancaria(numeroOperacion);
        pago.setCuentaRecaudadora(cuentaRecaudadora);
        pago.setImporteTotal(importe);
        pago.setComprobanteUrl(textoONull(request.getComprobanteUrl()));

        List<Aplicacion> plan = (request.getAsignaciones() == null || request.getAsignaciones().isEmpty())
                ? distribuirAutomaticamente(cliente.getId(), importe)
                : distribuirManualmente(cliente.getId(), importe, request.getAsignaciones());

        for (Aplicacion ap : plan) {
            PagoAsignacion asignacion = new PagoAsignacion();
            asignacion.setCargo(ap.cargo());
            asignacion.setMontoAplicado(ap.monto());
            pago.agregarAsignacion(asignacion);
            ap.cargo().setEstadoDocumento(estadoPara(ap.cargo().getMonto(), ap.aplicadoPrevio().add(ap.monto())));
        }

        pagoReciboRepository.save(pago);
        registrarMovimientoCredito(pago, usuario);

        return PagoReciboResponseDTO.from(pago);
    }

    /** FIFO sobre los cargos pendientes del cliente (bloqueados durante la transacción). */
    private List<Aplicacion> distribuirAutomaticamente(Long clienteId, BigDecimal importe) {
        List<CuentaCorriente> cargos = cuentaCorrienteRepository.lockCargosPendientesByCliente(clienteId);
        Map<Long, BigDecimal> aplicado = montosAplicados(cargos);

        List<Aplicacion> plan = new ArrayList<>();
        BigDecimal restante = importe;
        for (CuentaCorriente cargo : cargos) {
            if (restante.signum() <= 0) break;
            BigDecimal previo = aplicado.getOrDefault(cargo.getId(), BigDecimal.ZERO);
            BigDecimal pendiente = cargo.getMonto().subtract(previo);
            if (pendiente.signum() <= 0) {
                // Estado desfasado: el cargo ya estaba cubierto
                cargo.setEstadoDocumento(EstadoDocumento.PAGADO);
                continue;
            }
            BigDecimal monto = restante.min(pendiente);
            plan.add(new Aplicacion(cargo, monto, previo));
            restante = restante.subtract(monto);
        }
        return plan;
    }

    /** Asignaciones elegidas por el usuario, validadas contra el saldo real de cada cargo. */
    private List<Aplicacion> distribuirManualmente(Long clienteId, BigDecimal importe,
                                                   List<PagoReciboRequest.Asignacion> asignaciones) {
        Map<Long, BigDecimal> solicitado = new LinkedHashMap<>();
        for (PagoReciboRequest.Asignacion a : asignaciones) {
            if (a == null || a.getCuentaCorrienteCargoId() == null) {
                throw new ReglaNegocioException("Cada asignación debe indicar cuentaCorrienteCargoId");
            }
            BigDecimal monto = normalizarMonto(a.getMontoAplicado(), "montoAplicado");
            if (solicitado.putIfAbsent(a.getCuentaCorrienteCargoId(), monto) != null) {
                throw new ReglaNegocioException("El cargo " + a.getCuentaCorrienteCargoId()
                        + " aparece más de una vez en las asignaciones");
            }
        }

        BigDecimal totalSolicitado = solicitado.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalSolicitado.compareTo(importe) > 0) {
            throw new ReglaNegocioException("La suma de asignaciones (" + totalSolicitado
                    + ") supera el importe del pago (" + importe + ")");
        }

        List<CuentaCorriente> cargos = cuentaCorrienteRepository.lockByIds(solicitado.keySet());
        if (cargos.size() != solicitado.size()) {
            Set<Long> encontrados = cargos.stream().map(CuentaCorriente::getId).collect(Collectors.toSet());
            List<Long> faltantes = solicitado.keySet().stream().filter(id -> !encontrados.contains(id)).toList();
            throw new RecursoNoEncontradoException("Cargos no encontrados: " + faltantes);
        }
        Map<Long, CuentaCorriente> porId = cargos.stream()
                .collect(Collectors.toMap(CuentaCorriente::getId, c -> c));
        Map<Long, BigDecimal> aplicado = montosAplicados(cargos);

        List<Aplicacion> plan = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : solicitado.entrySet()) {
            CuentaCorriente cargo = porId.get(entry.getKey());
            if (cargo.getCliente() == null || !clienteId.equals(cargo.getCliente().getId())) {
                throw new ReglaNegocioException("El cargo " + cargo.getId() + " no pertenece al cliente");
            }
            if (cargo.getTipo() != CuentaCorriente.Tipo.DEBITO) {
                throw new ReglaNegocioException("El movimiento " + cargo.getId() + " no es un cargo (DEBITO)");
            }
            BigDecimal previo = aplicado.getOrDefault(cargo.getId(), BigDecimal.ZERO);
            BigDecimal pendiente = cargo.getMonto().subtract(previo);
            if (entry.getValue().compareTo(pendiente) > 0) {
                throw new ReglaNegocioException("El monto aplicado al cargo " + cargo.getId() + " ("
                        + entry.getValue() + ") supera su saldo pendiente (" + pendiente.max(BigDecimal.ZERO) + ")");
            }
            plan.add(new Aplicacion(cargo, entry.getValue(), previo));
        }
        return plan;
    }

    private void registrarMovimientoCredito(PagoRecibo pago, Usuario usuario) {
        CuentaCorriente credito = new CuentaCorriente();
        credito.setCliente(pago.getCliente());
        credito.setTipo(CuentaCorriente.Tipo.CREDITO);
        credito.setOrigen(CuentaCorriente.Origen.PAGO_RECIBIDO);
        credito.setConcepto(conceptoPago(pago));
        credito.setMonto(pago.getImporteTotal());
        credito.setFecha(pago.getFechaAbono().toLocalDate());
        credito.setReferencia(PREFIJO_REFERENCIA + pago.getId());
        credito.setDocumentoRef(pago.getNumeroOperacionBancaria());
        credito.setEstadoDocumento(EstadoDocumento.PAGADO);
        credito.setUsuario(usuario);
        cuentaCorrienteRepository.save(credito);
    }

    // =====================================================================
    // Anulación
    // =====================================================================

    /** Elimina el pago, sus asignaciones y su movimiento CREDITO, y recalcula el estado de los cargos. */
    @Transactional
    public void anular(Long id) {
        PagoRecibo pago = pagoReciboRepository.findWithAsignacionesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + id));

        Set<Long> cargoIds = pago.getAsignaciones().stream()
                .map(a -> a.getCargo().getId())
                .collect(Collectors.toCollection(TreeSet::new));
        List<CuentaCorriente> cargos = cargoIds.isEmpty()
                ? List.of()
                : cuentaCorrienteRepository.lockByIds(cargoIds);

        cuentaCorrienteRepository
                .findFirstByOrigenAndReferencia(CuentaCorriente.Origen.PAGO_RECIBIDO, PREFIJO_REFERENCIA + id)
                .ifPresent(cuentaCorrienteRepository::delete);

        pagoReciboRepository.delete(pago);
        pagoReciboRepository.flush();

        Map<Long, BigDecimal> aplicado = montosAplicados(cargos);
        for (CuentaCorriente cargo : cargos) {
            cargo.setEstadoDocumento(estadoPara(cargo.getMonto(),
                    aplicado.getOrDefault(cargo.getId(), BigDecimal.ZERO)));
        }
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    @Transactional(readOnly = true)
    public List<PagoReciboResponseDTO> listar(Long clienteId, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("'desde' no puede ser posterior a 'hasta'");
        }
        List<PagoRecibo> pagos;
        if (clienteId != null) {
            pagos = pagoReciboRepository.findByClienteIdOrderByFechaAbonoDesc(clienteId);
        } else if (desde != null || hasta != null) {
            pagos = pagoReciboRepository.findByFechaAbonoBetweenOrderByFechaAbonoDesc(
                    inicioDe(desde), finDe(hasta));
        } else {
            pagos = pagoReciboRepository.findAllByOrderByFechaAbonoDesc();
        }

        LocalDateTime ini = inicioDe(desde);
        LocalDateTime fin = finDe(hasta);
        return pagos.stream()
                .filter(p -> !p.getFechaAbono().isBefore(ini) && !p.getFechaAbono().isAfter(fin))
                .map(PagoReciboResponseDTO::resumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagoReciboResponseDTO obtener(Long id) {
        return pagoReciboRepository.findWithAsignacionesById(id)
                .map(PagoReciboResponseDTO::from)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<CargoPendienteDTO> cargosPendientes(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNoEncontradoException("Cliente no encontrado: " + clienteId);
        }
        List<CuentaCorriente> cargos = cuentaCorrienteRepository.findCargosPendientesByCliente(clienteId);
        Map<Long, BigDecimal> aplicado = montosAplicados(cargos);

        return cargos.stream()
                .map(c -> {
                    BigDecimal ap = aplicado.getOrDefault(c.getId(), BigDecimal.ZERO);
                    return new CargoPendienteDTO(c.getId(), c.getConcepto(), c.getDocumentoRef(),
                            c.getFecha(), c.getFechaVencimiento(), c.getMonto(), ap,
                            c.getMonto().subtract(ap), c.getEstadoDocumento());
                })
                .filter(dto -> dto.saldoPendiente().signum() > 0)
                .toList();
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /** Cargo + monto a aplicar + lo que ya tenía aplicado antes de este pago. */
    private record Aplicacion(CuentaCorriente cargo, BigDecimal monto, BigDecimal aplicadoPrevio) {
    }

    private Map<Long, BigDecimal> montosAplicados(Collection<CuentaCorriente> cargos) {
        if (cargos.isEmpty()) return Map.of();
        List<Long> ids = cargos.stream().map(CuentaCorriente::getId).toList();
        return pagoAsignacionRepository.sumMontoAplicadoByCargos(ids).stream()
                .collect(Collectors.toMap(PagoAsignacionRepository.MontoAplicadoView::getCargoId,
                        PagoAsignacionRepository.MontoAplicadoView::getMontoAplicado));
    }

    static String estadoPara(BigDecimal montoCargo, BigDecimal aplicado) {
        if (aplicado == null || aplicado.signum() <= 0) return EstadoDocumento.PENDIENTE;
        return aplicado.compareTo(montoCargo) >= 0 ? EstadoDocumento.PAGADO : EstadoDocumento.PARCIAL;
    }

    /** Exige monto positivo con como máximo 2 decimales (DECIMAL(12,2)). */
    private static BigDecimal normalizarMonto(BigDecimal monto, String campo) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("'" + campo + "' debe ser mayor que cero");
        }
        try {
            return monto.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ReglaNegocioException("'" + campo + "' admite como máximo 2 decimales");
        }
    }

    private static String textoONull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static String conceptoPago(PagoRecibo pago) {
        StringBuilder sb = new StringBuilder("Pago recibido");
        if (pago.getMedioPago() != null) sb.append(" - ").append(pago.getMedioPago());
        if (pago.getNumeroOperacionBancaria() != null) sb.append(" Op. ").append(pago.getNumeroOperacionBancaria());
        return sb.length() > 200 ? sb.substring(0, 200) : sb.toString();
    }

    private static LocalDateTime inicioDe(LocalDate desde) {
        return desde != null ? desde.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
    }

    private static LocalDateTime finDe(LocalDate hasta) {
        return hasta != null ? hasta.plusDays(1).atStartOfDay().minusNanos(1) : LocalDateTime.of(9999, 12, 31, 23, 59);
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ReglaNegocioException("No hay usuario autenticado");
        }
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + auth.getName()));
    }
}
