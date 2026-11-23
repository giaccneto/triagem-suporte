package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.config.TrelloProperties;
import com.giaccneto.triagem_suporte.model.Chamado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@EnableConfigurationProperties(TrelloProperties.class)
public class TrelloEncaminhamentoService implements EncaminhamentoService {

    private static final Logger log = LoggerFactory.getLogger(TrelloEncaminhamentoService.class);
    private final RestClient cliente;
    private final TrelloProperties.Lists listas;

    public TrelloEncaminhamentoService(TrelloProperties propriedades) {
        var http = HttpClient.newBuilder()
                .connectTimeout(propriedades.connectTimeout())
                .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(propriedades.readTimeout());
        cliente = RestClient.builder()
                .baseUrl(propriedades.baseUrl().toString())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\""
                                .formatted(propriedades.apiKey(), propriedades.token()))
                .build();
        listas = propriedades.lists();
    }

    @Override
    public String encaminhar(Chamado chamado) {
        String lista = switch (chamado.getEquipeDestino()) {
            case FINANCEIRO -> listas.financeiro();
            case TECNICO -> listas.tecnico();
            case REVISAO_HUMANA -> listas.revisaoHumana();
        };
        String nome = chamado.getTicketId() + " - " + chamado.getCategoria();
        String marcador = "triagem-ticket:" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(chamado.getTicketId().getBytes(StandardCharsets.UTF_8));

        try {
            var resposta = cliente.post()
                    .uri("/cards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("idList", lista, "name", nome, "desc", marcador))
                    .retrieve()
                    .toEntity(Cartao.class);
            if (resposta.getStatusCode().is2xxSuccessful() && possuiId(resposta.getBody())) {
                return resposta.getBody().id();
            }
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()
                    && exception.getStatusCode().value() != 408) {
                throw exception;
            }
        } catch (RestClientException exception) {
            // O servidor pode ter recebido o POST antes da falha.
        }

        String id = reconciliar(lista, marcador)
                .orElseThrow(() -> new IllegalStateException("Trello não confirmou a criação do cartão"));
        log.info("encaminhamento_reconciliado chamadoId={}", chamado.getId());
        return id;
    }

    private Optional<String> reconciliar(String lista, String marcador) {
        List<Cartao> resposta = cliente.get()
                .uri(builder -> builder.path("/lists/{id}/cards")
                        .queryParam("fields", "id,desc")
                        .build(lista))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Cartao>>() {});
        if (resposta == null) {
            return Optional.empty();
        }
        var correspondencias = resposta.stream()
                .filter(cartao -> cartao != null && marcador.equals(cartao.desc()))
                .limit(2)
                .toList();
        return correspondencias.size() == 1 && possuiId(correspondencias.get(0))
                ? Optional.of(correspondencias.get(0).id()) : Optional.empty();
    }

    private boolean possuiId(Cartao cartao) {
        return cartao != null && cartao.id() != null && !cartao.id().isBlank()
                && cartao.id().length() <= 255 && cartao.id().indexOf(0) < 0;
    }

    private record Cartao(String id, String desc) {
    }
}
