package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.IndicadoresVendaResponse;
import com.umc.pediupatrao.dto.dashboard.PontoVendaResponse;
import com.umc.pediupatrao.dto.dashboard.ProdutoRankingResponse;
import com.umc.pediupatrao.dto.dashboard.RankingProdutosResponse;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidosResponse;
import com.umc.pediupatrao.dto.dashboard.SituacaoStatusResponse;
import com.umc.pediupatrao.dto.dashboard.VendasPeriodoResponse;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Service
public class DashboardService {

    static final List<String> STATUS_OPERACAO = List.of(
            "RECEBIDO",
            "EM_PREPARACAO",
            "PRONTO",
            "SAIU_PARA_ENTREGA",
            "RETIRADO",
            "FINALIZADO",
            "CANCELADO"
    );

    private static final DateTimeFormatter DIA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public DashboardService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public IndicadoresVendaResponse indicadores(LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodo(dataInicio, dataFim);
        List<Pedido> pedidos = filtrarPorPeriodo(dataInicio, dataFim);
        List<Pedido> finalizados = pedidos.stream().filter(p -> "FINALIZADO".equals(p.getStatus())).toList();

        long cancelados = pedidos.stream().filter(p -> "CANCELADO".equals(p.getStatus())).count();
        long emAberto = pedidos.size() - finalizados.size() - cancelados;
        double faturamento = arredondar(finalizados.stream().mapToDouble(this::valorPedido).sum());
        double ticket = finalizados.isEmpty() ? 0.0 : arredondar(faturamento / finalizados.size());
        long itens = finalizados.stream().mapToLong(this::quantidadeItens).sum();

        return new IndicadoresVendaResponse(
                pedidos.size(),
                finalizados.size(),
                cancelados,
                emAberto,
                faturamento,
                ticket,
                itens,
                texto(dataInicio),
                texto(dataFim)
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public VendasPeriodoResponse vendas(LocalDate dataInicio, LocalDate dataFim, String agrupamento) {
        validarPeriodo(dataInicio, dataFim);
        String grupo = normalizarAgrupamento(agrupamento);
        List<Pedido> finalizados = filtrarPorPeriodo(dataInicio, dataFim).stream()
                .filter(p -> "FINALIZADO".equals(p.getStatus()))
                .filter(p -> p.getDataHoraEntrada() != null)
                .toList();

        Map<String, double[]> acumulado = new LinkedHashMap<>();
        if ("DIA".equals(grupo) && dataInicio != null && dataFim != null) {
            for (LocalDate dia = dataInicio; !dia.isAfter(dataFim); dia = dia.plusDays(1)) {
                acumulado.put(dia.format(DIA), new double[2]);
            }
        } else if ("MES".equals(grupo) && dataInicio != null && dataFim != null) {
            YearMonth cursor = YearMonth.from(dataInicio);
            YearMonth limite = YearMonth.from(dataFim);
            while (!cursor.isAfter(limite)) {
                acumulado.put(cursor.format(MES), new double[2]);
                cursor = cursor.plusMonths(1);
            }
        }

        for (Pedido pedido : finalizados) {
            String chave = "MES".equals(grupo)
                    ? YearMonth.from(pedido.getDataHoraEntrada()).format(MES)
                    : pedido.getDataHoraEntrada().toLocalDate().format(DIA);
            double[] bucket = acumulado.computeIfAbsent(chave, k -> new double[2]);
            bucket[0] += valorPedido(pedido);
            bucket[1] += 1;
        }

        List<PontoVendaResponse> serie = acumulado.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new PontoVendaResponse(e.getKey(), arredondar(e.getValue()[0]), (long) e.getValue()[1]))
                .toList();

        return new VendasPeriodoResponse(grupo, texto(dataInicio), texto(dataFim), serie);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public SituacaoPedidosResponse situacao(LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodo(dataInicio, dataFim);
        List<Pedido> pedidos = filtrarPorPeriodo(dataInicio, dataFim);
        long total = pedidos.size();

        List<SituacaoStatusResponse> porStatus = new ArrayList<>();
        for (String status : STATUS_OPERACAO) {
            long quantidade = pedidos.stream().filter(p -> status.equals(p.getStatus())).count();
            double percentual = total == 0 ? 0.0 : arredondar((quantidade * 100.0) / total);
            porStatus.add(new SituacaoStatusResponse(status, quantidade, percentual));
        }
        return new SituacaoPedidosResponse(total, texto(dataInicio), texto(dataFim), porStatus);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public RankingProdutosResponse maisVendidos(LocalDate dataInicio, LocalDate dataFim, String tipo, Integer limite) {
        validarPeriodo(dataInicio, dataFim);
        int topo = limite == null ? 5 : limite;
        if (topo < 1 || topo > 20) {
            throw new IllegalArgumentException("O limite do ranking deve estar entre 1 e 20.");
        }
        String tipoFiltro = tipo == null || tipo.isBlank() ? null : tipo.trim();

        Map<String, Produto> produtosPorId = new LinkedHashMap<>();
        for (Produto produto : produtoRepository.findAll()) {
            if (produto.getId() != null) {
                produtosPorId.put(produto.getId(), produto);
            }
        }

        Map<String, AcumuladoProduto> ranking = new LinkedHashMap<>();
        for (Pedido pedido : filtrarPorPeriodo(dataInicio, dataFim)) {
            if (!"FINALIZADO".equals(pedido.getStatus()) || pedido.getItens() == null) {
                continue;
            }
            double bruto = pedido.getItens().stream().mapToDouble(this::valorBrutoItem).sum();
            double totalPedido = valorPedido(pedido);
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = item.getProdutoId() == null ? null : produtosPorId.get(item.getProdutoId());
                String tipoItem = produto != null && produto.getTipo() != null ? produto.getTipo() : "Não informado";
                if (tipoFiltro != null && !tipoFiltro.equalsIgnoreCase(tipoItem)) {
                    continue;
                }
                String nome = item.getNome() == null || item.getNome().isBlank() ? "Item sem nome" : item.getNome();
                double participacao = bruto == 0 ? 0.0 : (valorBrutoItem(item) / bruto) * totalPedido;
                ranking.computeIfAbsent(nome.toLowerCase(), k -> new AcumuladoProduto(nome, tipoItem))
                        .somar(item.getQuantidade(), participacao);
            }
        }

        List<ProdutoRankingResponse> produtos = ranking.values().stream()
                .sorted(Comparator.comparingLong(AcumuladoProduto::quantidade).reversed()
                        .thenComparing(AcumuladoProduto::nome))
                .limit(topo)
                .map(a -> new ProdutoRankingResponse(a.nome, a.tipo, a.quantidade, arredondar(a.faturamento)))
                .toList();

        return new RankingProdutosResponse(topo, tipoFiltro, texto(dataInicio), texto(dataFim), produtos);
    }

    private List<Pedido> filtrarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return pedidoRepository.findAll().stream()
                .filter(pedido -> dentroDoPeriodo(pedido, dataInicio, dataFim))
                .toList();
    }

    private boolean dentroDoPeriodo(Pedido pedido, LocalDate dataInicio, LocalDate dataFim) {
        if (pedido.getDataHoraEntrada() == null) {
            return dataInicio == null && dataFim == null;
        }
        LocalDate data = pedido.getDataHoraEntrada().toLocalDate();
        if (dataInicio != null && data.isBefore(dataInicio)) {
            return false;
        }
        return dataFim == null || !data.isAfter(dataFim);
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
    }

    private String normalizarAgrupamento(String agrupamento) {
        if (agrupamento == null || agrupamento.isBlank()) {
            return "DIA";
        }
        String normalizado = agrupamento.trim().toUpperCase();
        if (!"DIA".equals(normalizado) && !"MES".equals(normalizado)) {
            throw new IllegalArgumentException("Agrupamento inválido. Use DIA ou MES.");
        }
        return normalizado;
    }

    private double valorPedido(Pedido pedido) {
        if (pedido.getValorTotal() != null) {
            return pedido.getValorTotal();
        }
        if (pedido.getItens() == null) {
            return 0.0;
        }
        return pedido.getItens().stream().mapToDouble(this::valorBrutoItem).sum();
    }

    private double valorBrutoItem(ItemPedido item) {
        double preco = item.getPrecoUnitario() == null ? 0.0 : item.getPrecoUnitario();
        return Math.max(item.getQuantidade(), 0) * preco;
    }

    private long quantidadeItens(Pedido pedido) {
        if (pedido.getItens() == null) {
            return 0;
        }
        return pedido.getItens().stream().mapToLong(item -> Math.max(item.getQuantidade(), 0)).sum();
    }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String texto(LocalDate data) {
        return data == null ? null : data.format(DIA);
    }

    private static final class AcumuladoProduto {
        private final String nome;
        private final String tipo;
        private long quantidade;
        private double faturamento;

        private AcumuladoProduto(String nome, String tipo) {
            this.nome = nome;
            this.tipo = tipo;
        }

        private void somar(int quantidade, double faturamento) {
            this.quantidade += Math.max(quantidade, 0);
            this.faturamento += faturamento;
        }

        private String nome() {
            return nome;
        }

        private long quantidade() {
            return quantidade;
        }
    }
}
