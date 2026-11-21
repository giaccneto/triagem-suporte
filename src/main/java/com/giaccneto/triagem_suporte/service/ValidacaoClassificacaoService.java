package com.giaccneto.triagem_suporte.service;


import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.model.CategoriaChamado;
import org.springframework.stereotype.Service;

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

        return true;
    }

}
