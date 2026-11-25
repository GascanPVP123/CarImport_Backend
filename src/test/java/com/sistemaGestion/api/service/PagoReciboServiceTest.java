package com.sistemaGestion.api.service;

import com.sistemaGestion.api.dto.PagoReciboRequest;
import com.sistemaGestion.api.dto.PagoReciboResponseDTO;
import com.sistemaGestion.api.exception.ConflictoException;
import com.sistemaGestion.api.exception.ReglaNegocioException;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoReciboServiceTest {

    @Mock PagoReciboRepository pagoReciboRepository;
    @Mock PagoAsignacionRepository pagoAsignacionRepository;
    @Mock CuentaCorrienteRepository cuentaCorrienteRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock UsuarioRepository usuarioRepository;
    @InjectMocks PagoReciboService service;

    Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Test");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        when(pagoReciboRepository.save(any())).thenAnswer(inv -> {
            PagoRecibo p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void distribuyeFifoYActualizaEstados() {
        CuentaCorriente c1 = cargo(10L, "100.00");   // ya tiene 30 aplicados → pendiente 70
        CuentaCorriente c2 = cargo(11L, "200.00");
        when(cuentaCorrienteRepository.lockCargosPendientesByCliente(1L)).thenReturn(List.of(c1, c2));
        when(pagoAsignacionRepository.sumMontoAplicadoByCargos(any()))
                .thenReturn(List.of(aplicado(10L, "30.00")));

        PagoReciboResponseDTO r = service.registrar(request("150.00", "OP-1"));

        assertThat(r.asignaciones()).hasSize(2);
        assertThat(r.asignaciones().get(0).montoAplicado()).isEqualByComparingTo("70.00");
        assertThat(r.asignaciones().get(1).montoAplicado()).isEqualByComparingTo("80.00");
        assertThat(r.montoSinAsignar()).isEqualByComparingTo("0");
        assertThat(c1.getEstadoDocumento()).isEqualTo("Pagado");
        assertThat(c2.getEstadoDocumento()).isEqualTo("Parcial");

        ArgumentCaptor<CuentaCorriente> mov = ArgumentCaptor.forClass(CuentaCorriente.class);
        verify(cuentaCorrienteRepository).save(mov.capture());
        assertThat(mov.getValue().getTipo()).isEqualTo(CuentaCorriente.Tipo.CREDITO);
        assertThat(mov.getValue().getOrigen()).isEqualTo(CuentaCorriente.Origen.PAGO_RECIBIDO);
        assertThat(mov.getValue().getReferencia()).isEqualTo("PAGO-99");
        assertThat(mov.getValue().getMonto()).isEqualByComparingTo("150.00");
    }

    @Test
    void excedenteQuedaSinAsignar() {
        when(cuentaCorrienteRepository.lockCargosPendientesByCliente(1L)).thenReturn(List.of(cargo(10L, "50.00")));
        when(pagoAsignacionRepository.sumMontoAplicadoByCargos(any())).thenReturn(List.of());

        PagoReciboResponseDTO r = service.registrar(request("80.00", null));

        assertThat(r.montoAsignado()).isEqualByComparingTo("50.00");
        assertThat(r.montoSinAsignar()).isEqualByComparingTo("30.00");
    }

    @Test
    void rechazaVoucherDuplicado() {
        when(pagoReciboRepository.existsByNumeroOperacionBancariaAndCuentaRecaudadora("OP-1", "BCP-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request("10.00", " OP-1 ")))
                .isInstanceOf(ConflictoException.class);
        verify(pagoReciboRepository, never()).save(any());
    }

    @Test
    void asignacionManualNoPuedeSuperarSaldoPendiente() {
        when(cuentaCorrienteRepository.lockByIds(any())).thenReturn(List.of(cargo(10L, "100.00")));
        when(pagoAsignacionRepository.sumMontoAplicadoByCargos(any()))
                .thenReturn(List.of(aplicado(10L, "60.00")));

        PagoReciboRequest req = request("50.00", null);
        req.getAsignaciones().add(asignacion(10L, "50.00"));   // pendiente real: 40

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("supera su saldo pendiente");
        verify(pagoReciboRepository, never()).save(any());
    }

    @Test
    void asignacionesNoPuedenSuperarImporte() {
        PagoReciboRequest req = request("50.00", null);
        req.getAsignaciones().add(asignacion(10L, "30.00"));
        req.getAsignaciones().add(asignacion(11L, "30.00"));

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("supera el importe");
    }

    @Test
    void asignacionManualRechazaCargoDeOtroCliente() {
        CuentaCorriente ajeno = cargo(10L, "100.00");
        Cliente otro = new Cliente();
        otro.setId(2L);
        ajeno.setCliente(otro);
        when(cuentaCorrienteRepository.lockByIds(any())).thenReturn(List.of(ajeno));

        PagoReciboRequest req = request("50.00", null);
        req.getAsignaciones().add(asignacion(10L, "50.00"));

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no pertenece al cliente");
    }

    @Test
    void estadoPara() {
        assertThat(PagoReciboService.estadoPara(new BigDecimal("100"), BigDecimal.ZERO)).isEqualTo("Pendiente");
        assertThat(PagoReciboService.estadoPara(new BigDecimal("100"), new BigDecimal("40"))).isEqualTo("Parcial");
        assertThat(PagoReciboService.estadoPara(new BigDecimal("100"), new BigDecimal("100.00"))).isEqualTo("Pagado");
    }

    // ===== helpers =====

    private CuentaCorriente cargo(Long id, String monto) {
        CuentaCorriente c = new CuentaCorriente();
        c.setId(id);
        c.setCliente(cliente);
        c.setTipo(CuentaCorriente.Tipo.DEBITO);
        c.setOrigen(CuentaCorriente.Origen.VENTA_DIRECTA);
        c.setConcepto("Cargo " + id);
        c.setMonto(new BigDecimal(monto));
        c.setFecha(LocalDate.of(2026, 1, 1));
        return c;
    }

    private static PagoAsignacionRepository.MontoAplicadoView aplicado(Long cargoId, String monto) {
        return new PagoAsignacionRepository.MontoAplicadoView() {
            public Long getCargoId() { return cargoId; }
            public BigDecimal getMontoAplicado() { return new BigDecimal(monto); }
        };
    }

    private static PagoReciboRequest request(String importe, String numeroOperacion) {
        PagoReciboRequest r = new PagoReciboRequest();
        r.setClienteId(1L);
        r.setImporteTotal(new BigDecimal(importe));
        r.setMedioPago("TRANSFERENCIA");
        r.setNumeroOperacionBancaria(numeroOperacion);
        r.setCuentaRecaudadora("BCP-001");
        return r;
    }

    private static PagoReciboRequest.Asignacion asignacion(Long cargoId, String monto) {
        PagoReciboRequest.Asignacion a = new PagoReciboRequest.Asignacion();
        a.setCuentaCorrienteCargoId(cargoId);
        a.setMontoAplicado(new BigDecimal(monto));
        return a;
    }
}
