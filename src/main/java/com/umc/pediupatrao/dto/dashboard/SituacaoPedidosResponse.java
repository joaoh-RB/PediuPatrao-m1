package com.umc.pediupatrao.dto.dashboard;

import java.util.List;

public record SituacaoPedidosResponse(
        long total,
        String dataInicio,
        String dataFim,
        List<SituacaoStatusResponse> porStatus
) {
}
