package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.model.CategoriaChamado;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.EquipeDestino;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste manual: cria um cartao real no Trello e exige credenciais validas.
 * Nao faz parte da suite automatizada para evitar efeitos externos.
 */
@Disabled("Teste manual de integracao com Trello; habilitar conscientemente no ambiente de testes")
@SpringBootTest
class TrelloEncaminhamentoServiceTest {
    @Autowired
    private EncaminhamentoService encaminhamentoService;

    @Test
    void deveCriarCartaoNoTrello() {
        Chamado chamado = new Chamado();
        chamado.setTicketId("TESTE-TRELLO-001");
        chamado.setCategoria(CategoriaChamado.COBRANCA);
        chamado.setEquipeDestino(EquipeDestino.FINANCEIRO);
        String idExterno = encaminhamentoService.encaminhar(chamado);
        assertNotNull(idExterno);
    }
}
