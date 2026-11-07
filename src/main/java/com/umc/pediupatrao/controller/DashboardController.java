package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.dashboard.IndicadoresVendaResponse;
import com.umc.pediupatrao.dto.dashboard.RankingProdutosResponse;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidosResponse;
import com.umc.pediupatrao.dto.dashboard.VendasPeriodoResponse;
import com.umc.pediupatrao.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/indicadores")
    public IndicadoresVendaResponse indicadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return dashboardService.indicadores(dataInicio, dataFim);
    }

    @GetMapping("/vendas")
    public VendasPeriodoResponse vendas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false, defaultValue = "DIA") String agrupamento) {
        return dashboardService.vendas(dataInicio, dataFim, agrupamento);
    }

    @GetMapping("/pedidos/situacao")
    public SituacaoPedidosResponse situacao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return dashboardService.situacao(dataInicio, dataFim);
    }

    @GetMapping("/produtos/mais-vendidos")
    public RankingProdutosResponse maisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false, defaultValue = "5") Integer limite) {
        return dashboardService.maisVendidos(dataInicio, dataFim, tipo, limite);
    }
}
