package com.umc.pediupatrao.dto.dashboard;

import java.util.List;

public record VendasPeriodoResponse(
        String agrupamento,
        String dataInicio,
        String dataFim,
        List<PontoVendaResponse> serie
) {
}
