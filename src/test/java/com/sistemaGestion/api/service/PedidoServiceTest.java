package com.sistemaGestion.api.service;

import com.sistemaGestion.api.exception.ConflictoException;
import com.sistemaGestion.api.exception.NoAutenticadoException;
import com.sistemaGestion.api.exception.RecursoNoEncontradoException;
import com.sistemaGestion.api.exception.ReglaNegocioException;
import com.sistemaGestion.api.model.*;
import com.sistemaGestion.api.model.Pedido.EstadoPedido;
import com.sistemaGestion.api.repository.CotizacionRepository;
import com.sistemaGestion.api.repository.PedidoRepository;
import com.sistemaGestion.api.repository.UsuarioRepository;
import com.sistemaGestion.api.security.UsuarioActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock CotizacionRepository cotizacionRepository;
    @Mock UsuarioRepository usuarioRepository;

    PedidoService service;
    Usuario vendedor;
    Cotizacion cotizacion;

    @BeforeEach
    void setUp() {
        service = new PedidoService(pedidoRepository, cotizacionRepository, new UsuarioActual(usuarioRepository));

        vendedor = new Usuario();
        vendedor.setId(7L);
        vendedor.setUsername("vendedor1");
        vendedor.setActivo(true);
        when(usuarioRepository.findByUsername("vendedor1")).thenReturn(Optional.of(vendedor));
        autenticar("vendedor1");

        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setNombre("Cliente Test");

        cotizacion = new Cotizacion();
        cotizacion.setId(10L);
        cotizacion.setNumero("COT-000010");
        cotizacion.setCliente(cliente);
        cotizacion.setEstado("APROBADA");
        cotizacion.setCondicionPago("CREDITO");
        cotizacion.setMoneda("SOLES");
        cotizacion.setTotal(new BigDecimal("1500.00"));
        when(cotizacionRepository.findById(10L)).thenReturn(Optional.of(cotizacion));

        when(pedidoRepository.findByCotizacionId(10L)).thenReturn(Optional.empty());
        when(pedidoRepository.findSiguienteCorrelativo()).thenReturn(5);
        when(pedidoRepository.save(any())).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            if (p.getId() == null) p.setId(99L);
            return p;
        });
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticar(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    @Test
    void creaPedidoDesdeCotizacionAprobadaConUsuarioAutenticado() {
        Pedido pedido = service.crearDesdeCotizacion(10L);

        assertThat(pedido.getId()).isEqualTo(99L);
        assertThat(pedido.getNumero()).isEqualTo("PED-000005");
        assertThat(pedido.getCorrelativo()).isEqualTo(5);
        assertThat(pedido.getCotizacion()).isSameAs(cotizacion);
        assertThat(pedido.getCliente()).isSameAs(cotizacion.getCliente());
        assertThat(pedido.getUsuario()).isSameAs(vendedor);
        assertThat(pedido.getTotal()).isEqualByComparingTo("1500.00");
        assertThat(pedido.getCondicionPago()).isEqualTo("CREDITO");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void rechazaCotizacionNoAprobada() {
        cotizacion.setEstado("BORRADOR");
        assertThatThrownBy(() -> service.crearDesdeCotizacion(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("APROBADAS");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rechazaCotizacionYaConvertida() {
        Pedido existente = new Pedido();
        existente.setNumero("PED-000001");
        when(pedidoRepository.findByCotizacionId(10L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.crearDesdeCotizacion(10L))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("PED-000001");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cotizacionInexistenteDa404() {
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.crearDesdeCotizacion(1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void sinTokenNoCreaPedido() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> service.crearDesdeCotizacion(10L))
                .isInstanceOf(NoAutenticadoException.class);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void usuarioInactivoNoAutenticado() {
        vendedor.setActivo(false);
        assertThatThrownBy(() -> service.crearDesdeCotizacion(10L))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void transicionesDeEstado() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThat(service.cambiarEstado(1L, "confirmado").estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(service.cambiarEstado(1L, "DESPACHADO").estado()).isEqualTo(EstadoPedido.DESPACHADO);

        // Un pedido despachado ya no puede anularse
        assertThatThrownBy(() -> service.anular(1L)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> service.cambiarEstado(1L, "INEXISTENTE")).isInstanceOf(ReglaNegocioException.class);

        assertThat(service.cambiarEstado(1L, "ENTREGADO").estado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThatThrownBy(() -> service.cambiarEstado(1L, "PENDIENTE")).isInstanceOf(ReglaNegocioException.class);
    }
}
