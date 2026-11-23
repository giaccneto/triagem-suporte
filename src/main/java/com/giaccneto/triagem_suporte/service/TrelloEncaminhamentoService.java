package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.model.Chamado;
import org.springframework.stereotype.Service;

@Service
public class TrelloEncaminhamentoService
        implements EncaminhamentoService {

    @Override
    public String encaminhar(Chamado chamado) {
        throw new UnsupportedOperationException(
                "Integração com Trello ainda não implementada"
        );
    }
}