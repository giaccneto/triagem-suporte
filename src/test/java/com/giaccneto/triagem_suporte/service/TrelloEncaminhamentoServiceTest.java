package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.config.TrelloProperties;
import com.giaccneto.triagem_suporte.model.CategoriaChamado;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.Classificacao;
import com.giaccneto.triagem_suporte.model.PrioridadeChamado;
import com.giaccneto.triagem_suporte.model.EquipeDestino;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrelloEncaminhamentoServiceTest {

    private final List<Requisicao> requisicoes = new CopyOnWriteArrayList<>();
    private HttpServer servidor;
    private TrelloEncaminhamentoService trello;
    private volatile int statusPost = 200;
    private volatile String corpoPost = "{\"id\":\"cartao-1\"}";
    private volatile String corpoGet = "[]";
    private volatile boolean interromperRespostaPost;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/1/", this::responder);
        servidor.start();
        var url = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/1");
        trello = new TrelloEncaminhamentoService(new TrelloProperties(url, "chave-teste", "token-teste",
                new TrelloProperties.Lists("fin", "tec", "rev"), Duration.ofSeconds(1), Duration.ofSeconds(2)));
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    @Test
    void deveCriarCartaoComCredenciaisNoCabecalhoEDadosMinimos() {
        assertThat(trello.encaminhar(chamado())).isEqualTo("cartao-1");
        assertThat(requisicoes).singleElement().satisfies(requisicao -> {
            assertThat(requisicao.metodo()).isEqualTo("POST");
            assertThat(requisicao.uri()).isEqualTo(URI.create("/1/cards"));
            assertThat(requisicao.autorizacao()).contains("chave-teste", "token-teste");
            assertThat(requisicao.corpo()).contains("T-1 - COBRANCA", "\"idList\":\"fin\"", "triagem-ticket:VC0x")
                    .doesNotContain("MENSAGEM PRIVADA");
        });
    }

    @Test
    void deveRecuperarCartaoPorMarcadorQuandoRespostaDoPostForInconclusiva() {
        statusPost = 503;
        corpoGet = """
                [{"id":"cartao-encontrado","desc":"triagem-ticket:VC0x"},
                 {"id":"outro","desc":"triagem-ticket:outro"}]
                """;

        assertThat(trello.encaminhar(chamado())).isEqualTo("cartao-encontrado");
        assertThat(requisicoes).extracting(Requisicao::metodo).containsExactly("POST", "GET");
        assertThat(requisicoes.get(1).uri().getPath()).isEqualTo("/1/lists/fin/cards");
    }

    @Test
    void deveRecuperarCartaoQuandoConexaoCaiDuranteConfirmacao() {
        interromperRespostaPost = true;
        corpoGet = """
                [{"id":"cartao-criado","desc":"triagem-ticket:VC0x"}]
                """;

        assertThat(trello.encaminhar(chamado())).isEqualTo("cartao-criado");
        assertThat(requisicoes).extracting(Requisicao::metodo).containsExactly("POST", "GET");
    }

    @Test
    void deveRegistrarFalhaQuandoReconciliacaoForAmbigua() {
        corpoPost = "{}";
        corpoGet = """
                [{"id":"cartao-1","desc":"triagem-ticket:VC0x"},
                 {"id":"cartao-2","desc":"triagem-ticket:VC0x"}]
                """;

        assertThatThrownBy(() -> trello.encaminhar(chamado())).isInstanceOf(IllegalStateException.class);
        assertThat(requisicoes).extracting(Requisicao::metodo).containsExactly("POST", "GET");
    }

    @Test
    void deveEvitarNovaChamadaQuandoTrelloRejeitaCredenciais() {
        statusPost = 401;
        assertThatThrownBy(() -> trello.encaminhar(chamado()))
                .isInstanceOf(org.springframework.web.client.RestClientResponseException.class);
        assertThat(requisicoes).extracting(Requisicao::metodo).containsExactly("POST");
    }

    private Chamado chamado() {
        var chamado = new Chamado("T-1", "CLIENTE-TESTE", "MENSAGEM PRIVADA");
        chamado.aplicarClassificacao(new Classificacao(CategoriaChamado.COBRANCA, PrioridadeChamado.NORMAL,
                EquipeDestino.FINANCEIRO, "Segunda via da fatura"));
        return chamado;
    }

    private void responder(HttpExchange exchange) throws IOException {
        try (exchange) {
            String metodo = exchange.getRequestMethod();
            requisicoes.add(new Requisicao(metodo, exchange.getRequestURI(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            boolean post = metodo.equals("POST");
            byte[] corpo = (post ? corpoPost : corpoGet).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            long tamanhoAnunciado = post && interromperRespostaPost ? corpo.length + 10 : corpo.length;
            exchange.sendResponseHeaders(post ? statusPost : 200, tamanhoAnunciado);
            exchange.getResponseBody().write(corpo);
        }
    }

    private record Requisicao(String metodo, URI uri, String autorizacao, String corpo) {
    }
}
