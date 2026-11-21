package com.giaccneto.triagem_suporte.service;


import com.giaccneto.triagem_suporte.exception.ChamadoDuplicadoException;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.StatusChamado;
import com.giaccneto.triagem_suporte.repository.ChamadoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;


    public ChamadoService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;

    }
    public Chamado save(Chamado chamado) {

        if (chamado.getMessage() == null ||
                chamado.getMessage().isBlank()) {
            throw new IllegalArgumentException(
                    "A mensagem do chamado não pode estar vazia!");
        }

        String ticket = chamado.getTicketId();

        if (ticket == null || ticket.isBlank()) {
            throw new IllegalArgumentException(
                    "O ticketId não pode estar vazio!");
        }

        if (chamadoRepository.findByTicketId(ticket).isPresent()) {
            throw new ChamadoDuplicadoException(
                    "Já existe um chamado com este ticketId: " + ticket);
        }

        chamado.setStatus(StatusChamado.RECEBIDO);

        return chamadoRepository.save(chamado);
    }

//    public Chamado save(Chamado chamado) {
//
//        String ticket = chamado.getTicketId();
//        chamadoRepository.findByTicketId(ticket);
//
//        if (chamado.getMessage() == null || chamado.getMessage().isBlank()) {
//            throw new IllegalArgumentException("A mensagem do chamado não pode estar vazia! ");
//        }
//        if (ticket == null || ticket.isPresent()) {
//            throw new IllegalArgumentException("Já existe um chamado com este ticketId" + chamado.getTicketId());
//        }
//        chamado.setStatus(StatusChamado.RECEBIDO);
//        return chamadoRepository.save(chamado);
//    }

    public Optional<Chamado> buscarPorTicketId(String ticketId) {

        return chamadoRepository.findByTicketId(ticketId);

    }

}
