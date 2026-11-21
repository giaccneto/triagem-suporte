package com.giaccneto.triagem_suporte.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class PoliticasTool {

    @Tool(description = "Consulta as políticas de classificação, prioridade e encaminhamento de chamados de suporte")
    public String consultarPoliticas()throws IOException {

        System.out.println(">>> FERRAMENTA CONSULTAR POLITICAS EXECUTADA");

        ClassPathResource resource = new ClassPathResource("policies/politicas-atendimento.json");

        try(InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
