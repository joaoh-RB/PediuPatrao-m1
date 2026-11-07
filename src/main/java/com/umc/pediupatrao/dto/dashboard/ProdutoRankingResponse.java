package com.umc.pediupatrao.dto.dashboard;

public record ProdutoRankingResponse(
        String nome,
        String tipo,
        long quantidade,
        double faturamento
) {
}
