package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.IndicadoresVendaResponse;
import com.umc.pediupatrao.dto.dashboard.RankingProdutosResponse;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidosResponse;
import com.umc.pediupatrao.dto.dashboard.VendasPeriodoResponse;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(pedidoRepository, produtoRepository);
    }

    @Test
    void indicadoresConsideramSomentePedidosFinalizadosNoFaturamento() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido("FINALIZADO", LocalDate.of(2026, 11, 1), 100.0, item("p1", "Mussarela", 2, 50.0)),
                pedido("CANCELADO", LocalDate.of(2026, 11, 1), 80.0, item("p1", "Mussarela", 1, 80.0)),
                pedido("RECEBIDO", LocalDate.of(2026, 11, 2), 40.0, item("p2", "Lata", 2, 20.0)),
                pedido("FINALIZADO", LocalDate.of(2026, 10, 1), 200.0, item("p1", "Mussarela", 1, 200.0))
        ));

        IndicadoresVendaResponse resposta = dashboardService.indicadores(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7));

        assertEquals(3, resposta.totalPedidos());
        assertEquals(1, resposta.pedidosFinalizados());
        assertEquals(1, resposta.pedidosCancelados());
        assertEquals(1, resposta.pedidosEmAberto());
        assertEquals(100.0, resposta.faturamento());
        assertEquals(100.0, resposta.ticketMedio());
        assertEquals(2, resposta.quantidadeItensVendidos());
    }

    @Test
    void vendasAgrupamPorDiaEPreenchemDiasSemMovimento() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido("FINALIZADO", LocalDate.of(2026, 11, 1), 50.0, item("p1", "Mussarela", 1, 50.0)),
                pedido("FINALIZADO", LocalDate.of(2026, 11, 3), 30.0, item("p1", "Mussarela", 1, 30.0))
        ));

        VendasPeriodoResponse resposta = dashboardService.vendas(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), "dia");

        assertEquals("DIA", resposta.agrupamento());
        assertEquals(3, resposta.serie().size());
        assertEquals(50.0, resposta.serie().get(0).faturamento());
        assertEquals(0.0, resposta.serie().get(1).faturamento());
        assertEquals(30.0, resposta.serie().get(2).faturamento());
        assertEquals(1, resposta.serie().get(2).quantidadePedidos());
    }

    @Test
    void situacaoInformaQuantidadeEPercentualDeCadaStatus() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido("FINALIZADO", LocalDate.of(2026, 11, 1), 10.0, item("p1", "Mussarela", 1, 10.0)),
                pedido("FINALIZADO", LocalDate.of(2026, 11, 1), 10.0, item("p1", "Mussarela", 1, 10.0)),
                pedido("CANCELADO", LocalDate.of(2026, 11, 1), 10.0, item("p1", "Mussarela", 1, 10.0)),
                pedido("PRONTO", LocalDate.of(2026, 11, 1), 10.0, item("p1", "Mussarela", 1, 10.0))
        ));

        SituacaoPedidosResponse resposta = dashboardService.situacao(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1));

        assertEquals(4, resposta.total());
        assertEquals(7, resposta.porStatus().size());
        assertEquals(2, quantidade(resposta, "FINALIZADO"));
        assertEquals(50.0, percentual(resposta, "FINALIZADO"));
        assertEquals(1, quantidade(resposta, "CANCELADO"));
        assertEquals(0, quantidade(resposta, "RECEBIDO"));
    }

    @Test
    void rankingIgnoraCanceladosEFiltraPorTipo() {
        Produto pizza = new Produto();
        pizza.setId("p1");
        pizza.setNome("Pizza Mussarela");
        pizza.setTipo("Pizza");
        Produto bebida = new Produto();
        bebida.setId("p2");
        bebida.setNome("Refrigerante Lata");
        bebida.setTipo("Bebida");
        when(produtoRepository.findAll()).thenReturn(List.of(pizza, bebida));
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido("FINALIZADO", LocalDate.of(2026, 11, 2), 90.0,
                        item("p1", "Pizza Mussarela", 2, 40.0),
                        item("p2", "Refrigerante Lata", 2, 10.0)),
                pedido("CANCELADO", LocalDate.of(2026, 11, 2), 40.0, item("p1", "Pizza Mussarela", 5, 40.0))
        ));

        RankingProdutosResponse resposta = dashboardService.maisVendidos(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7), "Pizza", 5);

        assertEquals(1, resposta.produtos().size());
        assertEquals("Pizza Mussarela", resposta.produtos().get(0).nome());
        assertEquals(2, resposta.produtos().get(0).quantidade());
        assertEquals(72.0, resposta.produtos().get(0).faturamento());
    }

    @Test
    void rejeitaPeriodoEAgrupamentoInvalidos() {
        IllegalArgumentException periodo = assertThrows(IllegalArgumentException.class,
                () -> dashboardService.indicadores(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)));
        assertTrue(periodo.getMessage().contains("data inicial"));

        IllegalArgumentException grupo = assertThrows(IllegalArgumentException.class,
                () -> dashboardService.vendas(null, null, "SEMANA"));
        assertTrue(grupo.getMessage().contains("DIA ou MES"));

        IllegalArgumentException limite = assertThrows(IllegalArgumentException.class,
                () -> dashboardService.maisVendidos(null, null, null, 0));
        assertTrue(limite.getMessage().contains("limite"));
    }

    private long quantidade(SituacaoPedidosResponse resposta, String status) {
        return resposta.porStatus().stream()
                .filter(item -> status.equals(item.status()))
                .findFirst()
                .orElseThrow()
                .quantidade();
    }

    private double percentual(SituacaoPedidosResponse resposta, String status) {
        return resposta.porStatus().stream()
                .filter(item -> status.equals(item.status()))
                .findFirst()
                .orElseThrow()
                .percentual();
    }

    private Pedido pedido(String status, LocalDate data, double total, ItemPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setDataHoraEntrada(data.atTime(12, 0));
        pedido.setValorTotal(total);
        pedido.setItens(List.of(itens));
        return pedido;
    }

    private ItemPedido item(String produtoId, String nome, int quantidade, double preco) {
        ItemPedido item = new ItemPedido();
        item.setProdutoId(produtoId);
        item.setNome(nome);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(preco);
        return item;
    }
}
