
package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.model.CategoriaChamado;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.EquipeDestino;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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

