package com.giaccneto.triagem_suporte.controller;

import com.giaccneto.triagem_suporte.config.CorrelacaoFilter;
import com.giaccneto.triagem_suporte.exception.ChamadoDuplicadoException;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.service.ChamadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChamadoController.class)
@Import(CorrelacaoFilter.class)
class ChamadoControllerTest {

    private static final String ENTRADA = """
            {"ticketId":"TESTE-1","customerId":"CLIENTE-TESTE","message":"Mensagem de teste"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChamadoService service;

    @Test
    void deveCriarComContratoPublicoECorrelacao() throws Exception {
        when(service.processarChamado(any())).thenAnswer(invocation -> {
            Chamado chamado = invocation.getArgument(0);
            chamado.solicitarRevisao("Classificação inconclusiva");
            return chamado;
        });

        mvc.perform(post("/chamados").contentType(APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.ticketId").value("TESTE-1"))
                .andExpect(jsonPath("$.status").value("REVISAO_PENDENTE"))
                .andExpect(jsonPath("$.id").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.customerId").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.message").doesNotHaveJsonPath());
    }

    @Test
    void deveRejeitarMensagemVazia() throws Exception {
        mvc.perform(post("/chamados").contentType(APPLICATION_JSON)
                        .content(ENTRADA.replace("Mensagem de teste", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.message").exists());
        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarJsonInvalidoComErroPadronizado() throws Exception {
        mvc.perform(post("/chamados").contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Corpo JSON inválido"));
        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarConflitoParaTicketDuplicado() throws Exception {
        when(service.processarChamado(any())).thenThrow(new ChamadoDuplicadoException("Ticket duplicado"));

        mvc.perform(post("/chamados").contentType(APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Ticket duplicado"));
    }

    @Test
    void deveRetornarErroPadronizadoQuandoTicketNaoExiste() throws Exception {
        when(service.buscarPorTicketId("AUSENTE")).thenReturn(Optional.empty());

        mvc.perform(get("/chamados/{ticketId}", "AUSENTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Chamado não encontrado"));
    }

    @Test
    void deveConsultarTicket() throws Exception {
        var chamado = new Chamado("TESTE-1", "CLIENTE-TESTE", "Mensagem de teste");
        when(service.buscarPorTicketId("TESTE-1")).thenReturn(Optional.of(chamado));

        mvc.perform(get("/chamados/{ticketId}", "TESTE-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TESTE-1"));
    }
}
