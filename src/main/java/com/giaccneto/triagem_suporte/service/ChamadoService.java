package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.exception.ChamadoDuplicadoException;
import com.giaccneto.triagem_suporte.model.*;
import com.giaccneto.triagem_suporte.repository.ChamadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ChamadoService {

    private static final Logger log = LoggerFactory.getLogger(ChamadoService.class);

    private final ChamadoRepository chamadoRepository;
    private final ClassificacaoService classificacaoService;
    private final ValidacaoClassificacaoService validacaoClassificacaoService;
    private final EncaminhamentoService encaminhamentoService;

    public ChamadoService(ChamadoRepository chamadoRepository,
                         ClassificacaoService classificacaoService,
                         ValidacaoClassificacaoService validacaoClassificacaoService,
                         EncaminhamentoService encaminhamentoService) {
        this.chamadoRepository = chamadoRepository;
        this.classificacaoService = classificacaoService;
        this.validacaoClassificacaoService = validacaoClassificacaoService;
        this.encaminhamentoService = encaminhamentoService;
    }

    public Chamado save(Chamado chamado) {
        if (chamado == null) {
            throw new IllegalArgumentException("O chamado não pode ser nulo!");
        }
        if (chamado.getMessage() == null || chamado.getMessage().isBlank()) {
            throw new IllegalArgumentException("A mensagem do chamado não pode estar vazia!");
        }
        String ticketId = chamado.getTicketId();
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("O ticketId não pode estar vazio!");
        }
        if (chamadoRepository.findByTicketId(ticketId).isPresent()) {
            throw new ChamadoDuplicadoException("Já existe um chamado com este ticketId: " + ticketId);
        }

        chamado.setStatus(StatusChamado.RECEBIDO);
        try {
            return chamadoRepository.save(chamado);
        } catch (DataIntegrityViolationException exception) {
            // A consulta anterior nao protege contra duas requisicoes simultaneas.
            // O indice unico do banco continua sendo a protecao definitiva.
            if (chamadoRepository.findByTicketId(ticketId).isPresent()) {
                throw new ChamadoDuplicadoException("Já existe um chamado com este ticketId: " + ticketId);
            }
            throw exception;
        }
    }

    public Optional<Chamado> buscarPorTicketId(String ticketId) {
        return chamadoRepository.findByTicketId(ticketId);
    }

    public Chamado processarChamado(Chamado chamado) {
        Chamado chamadoSalvo = save(chamado);

        final ClassificacaoResposta resposta;
        try {
            resposta = classificacaoService.classificar(chamadoSalvo.getMessage());
        } catch (Exception exception) {
            log.warn("Classificação automática falhou para ticketId={}", chamadoSalvo.getTicketId(), exception);
            return marcarRevisao(chamadoSalvo, "Falha na classificação automática. Revisão necessária.");
        }

        // A saida de modelos de IA nao e confiavel ate passar pela validacao.
        if (resposta == null || !validacaoClassificacaoService.validar(resposta)) {
            log.warn("Classificação inválida ou ausente para ticketId={}", chamadoSalvo.getTicketId());
            return marcarRevisao(chamadoSalvo, "Classificação inválida. Revisão humana necessária.");
        }

        // Converter somente valores ja validados, evitando acesso a enums invalidos.
        chamadoSalvo.setCategoria(CategoriaChamado.valueOf(resposta.getCategoria()));
        chamadoSalvo.setPrioridade(resposta.getPrioridade() == null
                ? null : PrioridadeChamado.valueOf(resposta.getPrioridade()));
        chamadoSalvo.setEquipeDestino(EquipeDestino.valueOf(resposta.getEquipeDestino()));
        chamadoSalvo.setJustificativa(resposta.getJustificativa());

        if (Boolean.TRUE.equals(resposta.getRevisaoNecessaria())
                || chamadoSalvo.getEquipeDestino() == EquipeDestino.REVISAO_HUMANA) {
            chamadoSalvo.setStatus(StatusChamado.REVISAO_PENDENTE);
            return chamadoRepository.save(chamadoSalvo);
        }

        chamadoSalvo.setStatus(StatusChamado.EM_ANALISE);
        // Persistir o estado intermediario antes da chamada externa; em uma
        // evolucao futura, uma fila/outbox tratara retries e reconciliacao.
        chamadoSalvo = chamadoRepository.save(chamadoSalvo);

        try {
            String idExterno = encaminhamentoService.encaminhar(chamadoSalvo);
            if (idExterno == null || idExterno.isBlank()) {
                throw new IllegalStateException("Encaminhamento sem identificador externo");
            }
            chamadoSalvo.setIdExterno(idExterno);
            chamadoSalvo.setStatus(StatusChamado.ENCAMINHADO);
        } catch (Exception exception) {
            // A falha pode ocorrer apos o sistema externo criar o cartao.
            // Nao tentar novamente automaticamente sem reconciliacao.
            log.error("Falha no encaminhamento de ticketId={}", chamadoSalvo.getTicketId(), exception);
            chamadoSalvo.setStatus(StatusChamado.FALHA_ENCAMINHAMENTO);
        }
        return chamadoRepository.save(chamadoSalvo);
    }

    private Chamado marcarRevisao(Chamado chamado, String justificativa) {
        chamado.setStatus(StatusChamado.REVISAO_PENDENTE);
        chamado.setEquipeDestino(EquipeDestino.REVISAO_HUMANA);
        chamado.setPrioridade(null);
        chamado.setJustificativa(justificativa);
        return chamadoRepository.save(chamado);
    }
}
