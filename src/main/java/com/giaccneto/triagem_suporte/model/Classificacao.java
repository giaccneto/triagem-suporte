package com.giaccneto.triagem_suporte.model;

public record Classificacao(
        CategoriaChamado categoria,
        PrioridadeChamado prioridade,
        EquipeDestino equipeDestino,
        String justificativa
) {
}
