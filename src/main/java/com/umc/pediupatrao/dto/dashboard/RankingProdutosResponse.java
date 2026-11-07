package com.umc.pediupatrao.dto.dashboard;

import java.util.List;

public record RankingProdutosResponse(
        int limite,
        String tipo,
        String dataInicio,
        String dataFim,
        List<ProdutoRankingResponse> produtos
) {
}
