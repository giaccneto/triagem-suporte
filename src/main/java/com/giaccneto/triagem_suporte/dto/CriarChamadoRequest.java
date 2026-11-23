package com.giaccneto.triagem_suporte.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Contrato de entrada da API, separado da entidade persistida.
 * Campos calculados (status, prioridade e idExterno) nao podem ser enviados pelo cliente.
 */
public record CriarChamadoRequest(
        @NotBlank(message = "ticketId é obrigatório") String ticketId,
        @NotBlank(message = "customerId é obrigatório") String customerId,
        @NotBlank(message = "message é obrigatória") String message
) {
}
