package com.giaccneto.triagem_suporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada da API, separado da entidade persistida.
 * Campos calculados (status, prioridade e idExterno) nao podem ser enviados pelo cliente.
 */
public record CriarChamadoRequest(
        @NotBlank(message = "ticketId é obrigatório") @Size(max = 255) @Pattern(regexp = "[^\\x00]*", message = "Caractere inválido") String ticketId,
        @NotBlank(message = "customerId é obrigatório") @Size(max = 255) @Pattern(regexp = "[^\\x00]*", message = "Caractere inválido") String customerId,
        @NotBlank(message = "message é obrigatória") @Size(max = 10000) @Pattern(regexp = "[^\\x00]*", message = "Caractere inválido") String message
) {
}
