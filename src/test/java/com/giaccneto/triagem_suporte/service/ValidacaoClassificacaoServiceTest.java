package com.giaccneto.triagem_suporte.service;


import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.service.ValidacaoClassificacaoService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidacaoClassificacaoServiceTest {

    private final ValidacaoClassificacaoService service =
            new ValidacaoClassificacaoService();

    @Test
    void deveRejeitarCategoriaInvalida() {

        ClassificacaoResposta resposta = new ClassificacaoResposta();
        resposta.setCategoria("DESCONHECIDA");

        boolean resultado = service.validar(resposta).isPresent();

        assertFalse(resultado);
    }

    @Test
    void deveAceitarClassificacaoValida() {

        ClassificacaoResposta resposta = new ClassificacaoResposta();

        resposta.setCategoria("COBRANCA");
        resposta.setPrioridade("NORMAL");
        resposta.setEquipeDestino("FINANCEIRO");
        resposta.setRevisaoNecessaria(false);
        resposta.setJustificativa("Solicitação de segunda via de fatura.");

        boolean resultado = service.validar(resposta).isPresent();

        assertTrue(resultado);
    }
    @Test
    void deveAceitarRevisaoHumana() {

        ClassificacaoResposta resposta = new ClassificacaoResposta();

        resposta.setCategoria("PROBLEMA_TECNICO");
        resposta.setPrioridade(null);
        resposta.setEquipeDestino("REVISAO_HUMANA");
        resposta.setRevisaoNecessaria(true);

        boolean resultado = service.validar(resposta).isPresent();

        assertTrue(resultado);
    }

    @Test
    void deveRejeitarEquipeIncorreta() {

        ClassificacaoResposta resposta = new ClassificacaoResposta();

        resposta.setCategoria("COBRANCA");
        resposta.setPrioridade("NORMAL");
        resposta.setEquipeDestino("TECNICO");
        resposta.setRevisaoNecessaria(false);

        boolean resultado = service.validar(resposta).isPresent();

        assertFalse(resultado);
    }





}

