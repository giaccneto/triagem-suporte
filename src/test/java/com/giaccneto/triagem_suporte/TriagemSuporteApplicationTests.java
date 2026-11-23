package com.giaccneto.triagem_suporte;

import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.exception.ChamadoDuplicadoException;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.StatusChamado;
import com.giaccneto.triagem_suporte.repository.ChamadoRepository;
import com.giaccneto.triagem_suporte.service.ChamadoService;
import com.giaccneto.triagem_suporte.service.ClassificacaoService;
import com.giaccneto.triagem_suporte.service.EncaminhamentoService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatModel;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.ai.ollama.base-url=http://127.0.0.1:1",
        "OLLAMA_MODEL=modelo-de-teste",
        "trello.base-url=http://127.0.0.1:1/1",
        "trello.api-key=chave-teste",
        "trello.token=token-teste",
        "trello.lists.financeiro=fin",
        "trello.lists.tecnico=tec",
        "trello.lists.revisao-humana=rev"
})
@ExtendWith(OutputCaptureExtension.class)
@EnabledIfEnvironmentVariable(named = "TRIAGEM_TEST_POSTGRES", matches = "true")
class TriagemSuporteApplicationTests {

    private static final String URL = "jdbc:postgresql://127.0.0.1:"
            + Integer.parseInt(System.getenv().getOrDefault("TRIAGEM_TEST_POSTGRES_PORT", "55432"))
            + "/triagem_teste";
    private static final String USUARIO = "triagem_teste";
    private static final String SENHA = "triagem_teste";

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> URL);
        registry.add("spring.datasource.username", () -> USUARIO);
        registry.add("spring.datasource.password", () -> SENHA);
    }

    @Autowired
    private ChamadoService service;

    @Autowired
    private ChamadoRepository repositorio;

    @Autowired
    private OllamaChatModel modelo;

    @MockitoBean
    private ClassificacaoService ia;

    @MockitoBean
    private EncaminhamentoService trello;

    @Test
    void deveConfigurarModeloPelaVariavelDeAmbiente() {
        assertThat(modelo.getOptions().getModel()).isEqualTo("modelo-de-teste");
    }

    @Test
    void devePersistirFalhaEEstadoIntermediarioSemTransacaoDuranteChamadas() {
        String ticketId = "TESTE-FALHA-" + UUID.randomUUID();
        when(ia.classificar(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(repositorio.findByTicketId(ticketId)).get()
                    .extracting(Chamado::getStatus).isEqualTo(StatusChamado.RECEBIDO);
            return classificacao();
        });
        when(trello.encaminhar(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(repositorio.findByTicketId(ticketId)).get()
                    .extracting(Chamado::getStatus).isEqualTo(StatusChamado.EM_ANALISE);
            throw new IllegalStateException("Falha simulada");
        });

        service.processarChamado(chamado(ticketId));

        assertThat(repositorio.findByTicketId(ticketId)).get().satisfies(registro -> {
            assertThat(registro.getStatus()).isEqualTo(StatusChamado.FALHA_ENCAMINHAMENTO);
            assertThat(registro.getIdExterno()).isNull();
            assertThat(registro.getMessage()).hasSize(1024);
        });
    }

    @Test
    void deveEncaminharUmaVezQuandoDuasRequisicoesDisputamMesmoTicket(CapturedOutput saida) throws Exception {
        when(ia.classificar(anyString())).thenReturn(classificacao());
        when(trello.encaminhar(any())).thenReturn("cartao-teste");
        String ticketId = "TESTE-CONCORRENTE-" + UUID.randomUUID();
        var iniciar = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> tentarProcessar(iniciar, ticketId));
            var segunda = executor.submit(() -> tentarProcessar(iniciar, ticketId));
            iniciar.countDown();
            assertThat(new boolean[]{primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS)})
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }
        assertThat(saida.getAll()).doesNotContain(ticketId);
        verify(ia).classificar(anyString());
        verify(trello).encaminhar(any());
        assertThat(repositorio.findByTicketId(ticketId)).get().satisfies(registro -> {
            assertThat(registro.getStatus()).isEqualTo(StatusChamado.ENCAMINHADO);
            assertThat(registro.getIdExterno()).isEqualTo("cartao-teste");
        });
    }

    @Test
    void devePreservarRegistroLegadoAoAplicarSegundaMigracao() {
        var dados = new DriverManagerDataSource(URL, USUARIO, SENHA);
        var jdbc = new JdbcTemplate(dados);
        String schema = "legado_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dados).schemas(schema).target("1").load().migrate();
            jdbc.update("INSERT INTO " + schema + ".chamado (ticket_id, message, status) VALUES (?, ?, ?)",
                    "LEGADO", "mensagem antiga", "RECEBIDO");
            jdbc.execute("DROP TABLE " + schema + ".flyway_schema_history");
            jdbc.execute("ALTER TABLE " + schema + ".chamado ADD UNIQUE (ticket_id)");
            var flyway = Flyway.configure().dataSource(dados).schemas(schema).baselineVersion("1").load();
            flyway.baseline();
            flyway.migrate();
            assertThat(jdbc.queryForObject("SELECT message FROM " + schema + ".chamado WHERE ticket_id = ?",
                    String.class, "LEGADO")).isEqualTo("mensagem antiga");
            assertThat(jdbc.update("UPDATE " + schema + ".chamado SET message = ?", "x".repeat(1024)))
                    .isEqualTo(1);
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private boolean tentarProcessar(CountDownLatch iniciar, String ticketId) throws InterruptedException {
        iniciar.await();
        try {
            service.processarChamado(chamado(ticketId));
            return true;
        } catch (ChamadoDuplicadoException exception) {
            return false;
        }
    }

    private Chamado chamado(String ticketId) {
        return new Chamado(ticketId, "CLIENTE-TESTE", "x".repeat(1024));
    }

    private ClassificacaoResposta classificacao() {
        var resposta = new ClassificacaoResposta();
        resposta.setCategoria("COBRANCA");
        resposta.setPrioridade("NORMAL");
        resposta.setEquipeDestino("FINANCEIRO");
        resposta.setJustificativa("Segunda via de fatura");
        resposta.setRevisaoNecessaria(false);
        return resposta;
    }
}
