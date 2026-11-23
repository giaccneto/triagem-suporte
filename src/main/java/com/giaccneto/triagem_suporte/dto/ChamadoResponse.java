package com.giaccneto.triagem_suporte.dto;

import com.giaccneto.triagem_suporte.model.*;

public record ChamadoResponse(
        String ticketId,
        CategoriaChamado categoria,
        PrioridadeChamado prioridade,
        StatusChamado status,
        String justificativa,
        EquipeDestino equipeDestino,
        String idExterno
) {
    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(chamado.getTicketId(), chamado.getCategoria(),
                chamado.getPrioridade(), chamado.getStatus(), chamado.getJustificativa(),
                chamado.getEquipeDestino(), chamado.getIdExterno());
    }
}
