package com.giaccneto.triagem_suporte.model;

import jakarta.persistence.*;

@Entity
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String ticketId;
    private String customerId;
    @Column(columnDefinition = "text")
    private String message;
    @Enumerated(EnumType.STRING)
    private CategoriaChamado categoria;
    @Enumerated(EnumType.STRING)
    private PrioridadeChamado prioridade;
    @Enumerated(EnumType.STRING)
    private StatusChamado status;
    @Column(columnDefinition = "text")
    private String justificativa;
    @Enumerated(EnumType.STRING)
    private EquipeDestino equipeDestino;
    private String idExterno;

    protected Chamado() {
    }

    public Chamado(String ticketId, String customerId, String message) {
        this.ticketId = ticketId;
        this.customerId = customerId;
        this.message = message;
    }

    public void registrarRecebimento() {
        status = StatusChamado.RECEBIDO;
    }

    public void aplicarClassificacao(Classificacao classificacao) {
        categoria = classificacao.categoria();
        prioridade = classificacao.prioridade();
        equipeDestino = classificacao.equipeDestino();
        justificativa = classificacao.justificativa();
        status = equipeDestino == EquipeDestino.REVISAO_HUMANA
                ? StatusChamado.REVISAO_PENDENTE : StatusChamado.EM_ANALISE;
    }

    public void solicitarRevisao(String motivo) {
        status = StatusChamado.REVISAO_PENDENTE;
        equipeDestino = EquipeDestino.REVISAO_HUMANA;
        prioridade = null;
        justificativa = motivo;
    }

    public void registrarEncaminhamento(String idCartao) {
        if (idCartao == null || idCartao.isBlank() || idCartao.length() > 255 || idCartao.indexOf(0) >= 0) {
            throw new IllegalStateException("Identificador externo inválido");
        }
        idExterno = idCartao;
        status = StatusChamado.ENCAMINHADO;
    }

    public void registrarFalhaDeEncaminhamento() {
        idExterno = null;
        status = StatusChamado.FALHA_ENCAMINHAMENTO;
    }

    public Long getId() {
        return id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getMessage() {
        return message;
    }

    public CategoriaChamado getCategoria() {
        return categoria;
    }

    public PrioridadeChamado getPrioridade() {
        return prioridade;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public EquipeDestino getEquipeDestino() {
        return equipeDestino;
    }

    public String getIdExterno() {
        return idExterno;
    }
}
