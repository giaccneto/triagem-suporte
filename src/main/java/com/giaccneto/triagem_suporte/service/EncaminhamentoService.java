package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.model.Chamado;

public interface EncaminhamentoService {
    String encaminhar(Chamado chamado);
}
