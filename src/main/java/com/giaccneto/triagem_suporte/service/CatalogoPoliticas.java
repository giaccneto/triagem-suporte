package com.giaccneto.triagem_suporte.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CatalogoPoliticas {

    private final String texto;

    public CatalogoPoliticas() throws IOException {
        var recurso = new ClassPathResource("policies/politicas-atendimento.json");
        try (var entrada = recurso.getInputStream()) {
            texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String texto() {
        return texto;
    }
}
