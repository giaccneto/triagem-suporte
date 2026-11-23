package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ValidacaoClassificacaoService {

    public Optional<Classificacao> validar(ClassificacaoResposta resposta) {
        if (resposta == null || resposta.getRevisaoNecessaria() == null) {
            return Optional.empty();
        }

        try {
            return validarValores(resposta);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Optional.empty();
        }
    }

    private Optional<Classificacao> validarValores(ClassificacaoResposta resposta) {
        var categoria = CategoriaChamado.valueOf(resposta.getCategoria());
        var equipe = EquipeDestino.valueOf(resposta.getEquipeDestino());
        String justificativa = resposta.getJustificativa();
        if (justificativa != null && (justificativa.length() > 2000 || justificativa.indexOf(0) >= 0)) {
            return Optional.empty();
        }

        if (resposta.getRevisaoNecessaria()) {
            if (equipe != EquipeDestino.REVISAO_HUMANA || resposta.getPrioridade() != null) {
                return Optional.empty();
            }
            return Optional.of(new Classificacao(categoria, null, equipe, justificativa));
        }

        var prioridade = PrioridadeChamado.valueOf(resposta.getPrioridade());
        if (justificativa == null || justificativa.isBlank()
                || (prioridade == PrioridadeChamado.ALTA && categoria != CategoriaChamado.PROBLEMA_TECNICO)
                || equipe != equipeDaCategoria(categoria)) {
            return Optional.empty();
        }
        return Optional.of(new Classificacao(categoria, prioridade, equipe, justificativa));
    }

    private EquipeDestino equipeDaCategoria(CategoriaChamado categoria) {
        return switch (categoria) {
            case COBRANCA -> EquipeDestino.FINANCEIRO;
            case PROBLEMA_TECNICO -> EquipeDestino.TECNICO;
            case OUTRO -> EquipeDestino.REVISAO_HUMANA;
        };
    }
}
