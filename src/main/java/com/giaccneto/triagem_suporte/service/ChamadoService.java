
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

import java.util.Optional;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final ClassificacaoService classificacaoService;
    private final ValidacaoClassificacaoService validacaoClassificacaoService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            ClassificacaoService classificacaoService,
            ValidacaoClassificacaoService validacaoClassificacaoService) {

        this.chamadoRepository = chamadoRepository;
        this.classificacaoService = classificacaoService;
        this.validacaoClassificacaoService = validacaoClassificacaoService;
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

        // 1. Registra o chamado
        Chamado chamadoSalvo = save(chamado);

        // 2. Consulta a IA
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

        // 3. Valida a resposta da IA
        boolean classificacaoValida =
                validacaoClassificacaoService.validar(resposta);

        if (!classificacaoValida) {
            chamadoSalvo.setStatus(StatusChamado.REVISAO_PENDENTE);
            chamadoSalvo.setJustificativa(
                    "Classificação inválida. Revisão humana necessária."
            );

            return chamadoRepository.save(chamadoSalvo);
        }

        // 4. Salva a categoria
        chamadoSalvo.setCategoria(
                CategoriaChamado.valueOf(resposta.getCategoria())
        );

        // 5. Salva a prioridade, quando informada
        if (resposta.getPrioridade() != null) {
            chamadoSalvo.setPrioridade(
                    PrioridadeChamado.valueOf(resposta.getPrioridade())
            );
        }

        // 6. Salva a equipe de destino
        chamadoSalvo.setEquipeDestino(
                EquipeDestino.valueOf(resposta.getEquipeDestino())
        );

        // 7. Salva a justificativa
        chamadoSalvo.setJustificativa(resposta.getJustificativa());

        // 8. Define o status
        if (Boolean.TRUE.equals(resposta.getRevisaoNecessaria())
                || EquipeDestino.REVISAO_HUMANA.equals(
                chamadoSalvo.getEquipeDestino())) {

            chamadoSalvo.setStatus(StatusChamado.REVISAO_PENDENTE);

        } else {
            chamadoSalvo.setStatus(StatusChamado.EM_ANALISE);
        }

        // 9. Atualiza o registro no banco
        return chamadoRepository.save(chamadoSalvo);
    }
}
