
package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.exception.ChamadoDuplicadoException;
import com.giaccneto.triagem_suporte.model.CategoriaChamado;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.EquipeDestino;
import com.giaccneto.triagem_suporte.model.PrioridadeChamado;
import com.giaccneto.triagem_suporte.model.StatusChamado;
import com.giaccneto.triagem_suporte.repository.ChamadoRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class ChamadoService {
    private static final Logger log =
            LoggerFactory.getLogger(ChamadoService.class);

    private final ChamadoRepository chamadoRepository;
    private final ClassificacaoService classificacaoService;
    private final ValidacaoClassificacaoService validacaoClassificacaoService;
    private final EncaminhamentoService encaminhamentoService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            ClassificacaoService classificacaoService,
            ValidacaoClassificacaoService validacaoClassificacaoService, EncaminhamentoService encaminhamentoService) {

        this.chamadoRepository = chamadoRepository;
        this.classificacaoService = classificacaoService;
        this.validacaoClassificacaoService = validacaoClassificacaoService;
        this.encaminhamentoService = encaminhamentoService;
    }

    public Chamado save(Chamado chamado) {

        if (chamado == null) {
            throw new IllegalArgumentException(
                    "O chamado não pode ser nulo!");
        }

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

    public Optional<Chamado> buscarPorTicketId(String ticketId) {
        return chamadoRepository.findByTicketId(ticketId);
    }

    public Chamado processarChamado(Chamado chamado) {

        Chamado chamadoSalvo = save(chamado);

        ClassificacaoResposta resposta;

        try {
            resposta = classificacaoService.classificar(
                    chamadoSalvo.getMessage()
            );
        } catch (Exception e) {
            chamadoSalvo.setStatus(StatusChamado.REVISAO_PENDENTE);
            chamadoSalvo.setJustificativa(
                    "Falha na classificação automática. Revisão necessária."
            );

            return chamadoRepository.save(chamadoSalvo);
        }
        System.out.println("Categoria IA: " + resposta.getCategoria());
        System.out.println("Prioridade IA: " + resposta.getPrioridade());
        System.out.println("Equipe IA: " + resposta.getEquipeDestino());
        System.out.println("Revisão IA: " + resposta.getRevisaoNecessaria());

        boolean classificacaoValida =
                validacaoClassificacaoService.validar(resposta);

        if (!classificacaoValida) {
            chamadoSalvo.setStatus(StatusChamado.REVISAO_PENDENTE);
            chamadoSalvo.setJustificativa(
                    "Classificação inválida. Revisão humana necessária."
            );

            return chamadoRepository.save(chamadoSalvo);
        }

        chamadoSalvo.setCategoria(
                CategoriaChamado.valueOf(resposta.getCategoria())
        );

        if (resposta.getPrioridade() != null) {
            chamadoSalvo.setPrioridade(
                    PrioridadeChamado.valueOf(resposta.getPrioridade())
            );
        }

        chamadoSalvo.setEquipeDestino(
                EquipeDestino.valueOf(resposta.getEquipeDestino())
        );

        chamadoSalvo.setJustificativa(resposta.getJustificativa());

        // 8. Define o status
        if (Boolean.TRUE.equals(resposta.getRevisaoNecessaria())
                || EquipeDestino.REVISAO_HUMANA.equals(
                chamadoSalvo.getEquipeDestino())) {

            chamadoSalvo.setStatus(StatusChamado.REVISAO_PENDENTE);

        } else {
            chamadoSalvo.setStatus(StatusChamado.EM_ANALISE);

            try {
                String idExterno = encaminhamentoService.encaminhar(chamadoSalvo);

                chamadoSalvo.setIdExterno(idExterno);
                chamadoSalvo.setStatus(StatusChamado.ENCAMINHADO);

            } catch (Exception e) {
                log.error("Falha ao encaminhar chamado com ticketId: {}",
                        chamadoSalvo.getTicketId(), e);

                chamadoSalvo.setStatus(StatusChamado.FALHA_ENCAMINHAMENTO);
            }
        }

        return chamadoRepository.save(chamadoSalvo);
    }
}
