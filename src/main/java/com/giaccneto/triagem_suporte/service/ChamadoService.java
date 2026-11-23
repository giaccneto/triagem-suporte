package com.giaccneto.triagem_suporte.service;

import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.exception.ChamadoDuplicadoException;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.StatusChamado;
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
            throw new ChamadoDuplicadoException("Já existe um chamado com este ticketId: ");
        }

        chamado.registrarRecebimento();
        try {
            return chamadoRepository.save(chamado);
        } catch (DataIntegrityViolationException exception) {
            // A consulta anterior nao protege contra duas requisicoes simultaneas.
            // O indice unico do banco continua sendo a protecao definitiva.
            if (chamadoRepository.findByTicketId(ticketId).isPresent()) {
                throw new ChamadoDuplicadoException("Já existe um chamado com este ticketId: ");
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
        } catch (RuntimeException exception) {
            log.warn("classificacao_falhou chamadoId={} causa={}",
                    chamadoSalvo.getId(), exception.getClass().getSimpleName());
            return marcarRevisao(chamadoSalvo, "Falha na classificação automática. Revisão necessária.");
        }

        var classificacao = validacaoClassificacaoService.validar(resposta);
        if (classificacao.isEmpty()) {
            log.warn("classificacao_invalida chamadoId={}", chamadoSalvo.getId());
            return marcarRevisao(chamadoSalvo, "Classificação inválida. Revisão humana necessária.");
        }

        chamadoSalvo.aplicarClassificacao(classificacao.orElseThrow());
        chamadoSalvo = chamadoRepository.save(chamadoSalvo);
        if (chamadoSalvo.getStatus() == StatusChamado.REVISAO_PENDENTE) {
            return chamadoSalvo;
        }

        try {
            chamadoSalvo.registrarEncaminhamento(encaminhamentoService.encaminhar(chamadoSalvo));
        } catch (RuntimeException exception) {
            log.warn("encaminhamento_falhou chamadoId={} causa={}",
                    chamadoSalvo.getId(), exception.getClass().getSimpleName());
            chamadoSalvo.registrarFalhaDeEncaminhamento();
        }
        return chamadoRepository.save(chamadoSalvo);
    }

    private Chamado marcarRevisao(Chamado chamado, String justificativa) {
        chamado.solicitarRevisao(justificativa);
        return chamadoRepository.save(chamado);
    }
}
