package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.EquipeDestino;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.Map;


@Service
public class TrelloEncaminhamentoService implements EncaminhamentoService {

    @Value("${TRELLO_API_KEY}")
    private String apiKey;

    @Value("${TRELLO_TOKEN}")
    private String token;

    @Value("${trello.lists.financeiro}")
    private String listaFinanceiro;

    @Value("${trello.lists.tecnico}")
    private String listaTecnico;

    @Value("${trello.lists.revisao-humana}")
    private String listaRevisaoHumana;


    @Override
    public String encaminhar(Chamado chamado) {

        if (chamado == null || chamado.getEquipeDestino() == null) {
            throw new IllegalArgumentException(
                    "Chamado ou equipe de destino inválida"
            );
        }

        String idLista = obterIdLista(chamado.getEquipeDestino());

        String nomeCartao = chamado.getTicketId()
                + " - " + chamado.getCategoria();

        Map resposta = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("key", apiKey)
                        .queryParam("token", token)
                        .queryParam("idList", idLista)
                        .queryParam("name", nomeCartao)
                        .build())
                .retrieve()
                .body(Map.class);

        if (resposta == null || resposta.get("id") == null) {
            throw new IllegalStateException(
                    "Trello não confirmou a criação do cartão"
            );
        }

        return resposta.get("id").toString();
    }


    private String obterIdLista(EquipeDestino equipe) {

        return switch (equipe) {
            case FINANCEIRO -> listaFinanceiro;
            case TECNICO -> listaTecnico;
            case REVISAO_HUMANA -> listaRevisaoHumana;
        };
    }

    private final RestClient restClient = RestClient.create(
            "https://api.trello.com/1"
    );
}