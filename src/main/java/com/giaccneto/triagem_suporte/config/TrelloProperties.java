package com.giaccneto.triagem_suporte.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("trello")
public record TrelloProperties(
        @NotNull URI baseUrl,
        @NotBlank String apiKey,
        @NotBlank String token,
        @NotNull @Valid Lists lists,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
    public record Lists(@NotBlank String financeiro, @NotBlank String tecnico, @NotBlank String revisaoHumana) {
    }
}
