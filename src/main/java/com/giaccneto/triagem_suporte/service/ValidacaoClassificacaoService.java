package com.giaccneto.triagem_suporte.service;


import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.model.CategoriaChamado;
import com.giaccneto.triagem_suporte.model.PrioridadeChamado;
import org.springframework.stereotype.Service;
import com.giaccneto.triagem_suporte.model.EquipeDestino;

@Service
public class ValidacaoClassificacaoService {


    public boolean validar(ClassificacaoResposta resposta) {

        if (resposta == null) {
            return false;
        }

        if (resposta.getCategoria() == null ||
                resposta.getCategoria().isBlank()) {
            return false;
        }

        try {
            CategoriaChamado.valueOf(resposta.getCategoria());
        } catch (IllegalArgumentException e) {
            return false;
        }


        if (resposta.getRevisaoNecessaria() == null) {
            return false;
        }
        if (Boolean.FALSE.equals(resposta.getRevisaoNecessaria())) {

            if (resposta.getPrioridade() == null ||
                    resposta.getPrioridade().isBlank()) {
                return false;
            }

            try {
                PrioridadeChamado.valueOf(resposta.getPrioridade());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }


        if (resposta.getEquipeDestino() == null ||
                resposta.getEquipeDestino().isBlank()) {
            return false;
        }

        try {
            EquipeDestino.valueOf(resposta.getEquipeDestino());
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (Boolean.FALSE.equals(resposta.getRevisaoNecessaria())) {
            if (resposta.getCategoria().equals("PROBLEMA_TECNICO")
                    && !resposta.getEquipeDestino().equals("TECNICO")) {
                return false;
            }

            if (resposta.getCategoria().equals("COBRANCA")
                    && !resposta.getEquipeDestino().equals("FINANCEIRO")) {
                return false;
            }

            if (resposta.getCategoria().equals("OUTRO")
                    && !resposta.getEquipeDestino().equals("REVISAO_HUMANA")) {
                return false;
            }
        }

        if (Boolean.TRUE.equals(resposta.getRevisaoNecessaria())) {

            if (!resposta.getEquipeDestino().equals("REVISAO_HUMANA")) {
                return false;
            }
            if (resposta.getPrioridade() != null) {
                return false;
            }

        }
        return true;

    }

}
