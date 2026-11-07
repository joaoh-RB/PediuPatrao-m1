package com.umc.pediupatrao.dto.dashboard;

public record IndicadoresVendaResponse(
        long totalPedidos,
        long pedidosFinalizados,
        long pedidosCancelados,
        long pedidosEmAberto,
        double faturamento,
        double ticketMedio,
        long quantidadeItensVendidos,
        String dataInicio,
        String dataFim
) {
}
