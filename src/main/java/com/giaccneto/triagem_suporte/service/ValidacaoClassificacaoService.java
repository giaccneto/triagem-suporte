
package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.model.CategoriaChamado;
import com.giaccneto.triagem_suporte.model.EquipeDestino;
import com.giaccneto.triagem_suporte.model.PrioridadeChamado;
import org.springframework.stereotype.Service;

@Service
public class ValidacaoClassificacaoService {

    public boolean validar(ClassificacaoResposta resposta) {

        if (resposta == null ||
                resposta.getRevisaoNecessaria() == null) {
            return false;
        }

        CategoriaChamado categoria;
        EquipeDestino equipe;

        try {
            categoria = CategoriaChamado.valueOf(
                    resposta.getCategoria()
            );

            equipe = EquipeDestino.valueOf(
                    resposta.getEquipeDestino()
            );

        } catch (IllegalArgumentException |
                 NullPointerException e) {
            return false;
        }

        // Revisão humana
        if (Boolean.TRUE.equals(resposta.getRevisaoNecessaria())) {
            return equipe == EquipeDestino.REVISAO_HUMANA
                    && resposta.getPrioridade() == null;
        }

        // Classificação automática exige justificativa
        if (resposta.getJustificativa() == null ||
                resposta.getJustificativa().isBlank()) {
            return false;
        }

        PrioridadeChamado prioridade;

        try {
            prioridade = PrioridadeChamado.valueOf(
                    resposta.getPrioridade()
            );
        } catch (IllegalArgumentException |
                 NullPointerException e) {
            return false;
        }

        // Prioridade ALTA somente para problemas técnicos
        if (prioridade == PrioridadeChamado.ALTA &&
                categoria != CategoriaChamado.PROBLEMA_TECNICO) {
            return false;
        }

        // Verifica a equipe correta
        return switch (categoria) {
            case COBRANCA ->
                    equipe == EquipeDestino.FINANCEIRO;

            case PROBLEMA_TECNICO ->
                    equipe == EquipeDestino.TECNICO;

            case OUTRO ->
                    equipe == EquipeDestino.REVISAO_HUMANA;
        };
    }
}
