package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAuditoria;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PedidoService {

    private static final Set<String> STATUS_ANTES_SAIDA = Set.of("RECEBIDO", "EM_PREPARACAO", "PRONTO");
    private static final Set<String> STATUS_SAIDA = Set.of("SAIU_PARA_ENTREGA", "RETIRADO");
    private static final Map<String, Set<String>> TRANSICOES = Map.of(
            "RECEBIDO", Set.of("EM_PREPARACAO"),
            "EM_PREPARACAO", Set.of("PRONTO"),
            "PRONTO", Set.of("SAIU_PARA_ENTREGA", "RETIRADO"),
            "SAIU_PARA_ENTREGA", Set.of("FINALIZADO"),
            "RETIRADO", Set.of("FINALIZADO")
    );

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
            AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Pedido criarPedido(Pedido pedido) {
        if (pedido.getClienteId() == null || pedido.getClienteId().isBlank()) {
            throw new IllegalArgumentException("Cliente é obrigatório.");
        }
        List<ItemPedido> itensValidos = prepararItens(pedido.getItens());
        pedido.setItens(itensValidos);
        pedido.setDescontoPercentual(0.0);
        pedido.setValorTotal(calcularTotal(itensValidos, 0.0));
        pedido.setStatus("RECEBIDO");
        pedido.setResponsavelRegistro(usuarioAtual());
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);
        pedido.setJustificativaCancelamento(null);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("INCLUSÃO DE PEDIDO", "Pedido", salvo.getId(),
                List.of(
                        new CampoAuditoria("status", "", salvo.getStatus()),
                        new CampoAuditoria("valorTotal", "", String.valueOf(salvo.getValorTotal())),
                        new CampoAuditoria("responsavelRegistro", "", salvo.getResponsavelRegistro())
                ),
                null);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        validarTransicao(pedido, status);
        String statusAnterior = pedido.getStatus();
        pedido.setStatus(status);
        if (STATUS_SAIDA.contains(status)) {
            pedido.setDataHoraSaida(LocalDateTime.now());
            pedido.setResponsavelSaida(usuarioAtual());
        }
        Pedido salvo = pedidoRepository.save(pedido);
        List<CampoAuditoria> campos = new ArrayList<>();
        campos.add(new CampoAuditoria("status", statusAnterior, salvo.getStatus()));
        if (STATUS_SAIDA.contains(status)) {
            campos.add(new CampoAuditoria("dataHoraSaida", "", String.valueOf(salvo.getDataHoraSaida())));
            campos.add(new CampoAuditoria("responsavelSaida", "", salvo.getResponsavelSaida()));
        }
        auditoriaService.registrar("ALTERAÇÃO DE STATUS", "Pedido", salvo.getId(), campos, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("O cancelamento exige justificativa.");
        }
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        if ("CANCELADO".equals(pedido.getStatus())) {
            throw new IllegalArgumentException("Pedido já cancelado.");
        }
        if (!STATUS_ANTES_SAIDA.contains(pedido.getStatus())) {
            throw new IllegalArgumentException("Não é permitido cancelar após a saída para entrega ou retirada.");
        }
        String statusAnterior = pedido.getStatus();
        pedido.setStatus("CANCELADO");
        pedido.setJustificativaCancelamento(justificativa);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("CANCELAMENTO DE PEDIDO", "Pedido", salvo.getId(),
                List.of(new CampoAuditoria("status", statusAnterior, "CANCELADO")),
                justificativa);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, Double percentual) {
        if (percentual == null || percentual <= 0 || percentual > 20) {
            throw new IllegalArgumentException("Desconto percentual limitado a 20% do valor do pedido.");
        }
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        if ("CANCELADO".equals(pedido.getStatus()) || "FINALIZADO".equals(pedido.getStatus())) {
            throw new IllegalArgumentException("Não é permitido alterar pedido cancelado ou finalizado.");
        }
        if (!STATUS_ANTES_SAIDA.contains(pedido.getStatus())) {
            throw new IllegalArgumentException("Desconto somente antes da saída para entrega ou retirada.");
        }
        Double descontoAnterior = pedido.getDescontoPercentual();
        Double totalAnterior = pedido.getValorTotal();
        pedido.setDescontoPercentual(percentual);
        pedido.setValorTotal(calcularTotal(pedido.getItens(), percentual));
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("APLICAÇÃO DE DESCONTO", "Pedido", salvo.getId(),
                List.of(
                        new CampoAuditoria("descontoPercentual",
                                String.valueOf(descontoAnterior), String.valueOf(salvo.getDescontoPercentual())),
                        new CampoAuditoria("valorTotal",
                                String.valueOf(totalAnterior), String.valueOf(salvo.getValorTotal()))
                ),
                null);
        return salvo;
    }

    private void validarTransicao(Pedido pedido, String statusNovo) {
        if (statusNovo == null || statusNovo.isBlank()) {
            throw new IllegalArgumentException("Status é obrigatório.");
        }
        if ("CANCELADO".equals(pedido.getStatus()) || "FINALIZADO".equals(pedido.getStatus())) {
            throw new IllegalArgumentException("Não é permitido alterar o status após " + pedido.getStatus() + ".");
        }
        if ("CANCELADO".equals(statusNovo)) {
            throw new IllegalArgumentException("Cancelamento deve ser feito pela operação exclusiva do gerente.");
        }
        Set<String> permitidos = TRANSICOES.getOrDefault(pedido.getStatus(), Set.of());
        if (!permitidos.contains(statusNovo)) {
            throw new IllegalArgumentException("Transição inválida de " + pedido.getStatus() + " para " + statusNovo + ".");
        }
    }

    private List<ItemPedido> prepararItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter itens.");
        }
        List<ItemPedido> preparados = new ArrayList<>();
        for (ItemPedido item : itens) {
            if (item == null || item.getProdutoId() == null || item.getProdutoId().isBlank()) {
                continue;
            }
            if (item.getQuantidade() <= 0) {
                throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
            }
            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
            ItemPedido preparado = new ItemPedido();
            preparado.setProdutoId(produto.getId());
            preparado.setNome(produto.getNome());
            preparado.setQuantidade(item.getQuantidade());
            preparado.setPrecoUnitario(produto.getPreco());
            preparados.add(preparado);
        }
        if (preparados.isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter itens.");
        }
        return preparados;
    }

    private Double calcularTotal(List<ItemPedido> itens, Double descontoPercentual) {
        double subtotal = 0;
        for (ItemPedido item : itens) {
            double preco = item.getPrecoUnitario() == null ? 0 : item.getPrecoUnitario();
            subtotal += item.getQuantidade() * preco;
        }
        double desconto = descontoPercentual == null ? 0 : descontoPercentual;
        return Math.round(subtotal * (1 - (desconto / 100.0)) * 100.0) / 100.0;
    }

    private String usuarioAtual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
