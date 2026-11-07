package com.umc.pediupatrao.dto.dashboard;

public record SituacaoStatusResponse(
        String status,
        long quantidade,
        double percentual
) {
}
