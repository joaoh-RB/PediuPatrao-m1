package com.umc.pediupatrao.dto.dashboard;

public record PontoVendaResponse(
        String periodo,
        double faturamento,
        long quantidadePedidos
) {
}
