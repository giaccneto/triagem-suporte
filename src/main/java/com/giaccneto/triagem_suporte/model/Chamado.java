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
    private String message;
    @Enumerated(EnumType.STRING)
    private CategoriaChamado categoria;
    @Enumerated(EnumType.STRING)
    private PrioridadeChamado prioridade;
    @Enumerated(EnumType.STRING)
    private StatusChamado status;
    private String justificativa;
    @Enumerated(EnumType.STRING)
    private EquipeDestino equipeDestino;
    private String idExterno;

    public Chamado() {
    }

    public Chamado(Long id, String ticketId, String customerId, String message, CategoriaChamado categoria, PrioridadeChamado prioridade, StatusChamado status, String justificativa, EquipeDestino equipeDestino, String idExterno) {
        this.ticketId = ticketId;
        this.customerId = customerId;
        this.message = message;
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.status = status;
        this.justificativa = justificativa;
        this.equipeDestino = equipeDestino;
        this.idExterno = idExterno;
    }

    public Long getId() {
        return id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CategoriaChamado getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaChamado categoria) {
        this.categoria = categoria;
    }

    public PrioridadeChamado getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(PrioridadeChamado prioridade) {
        this.prioridade = prioridade;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public void setStatus(StatusChamado status) {
        this.status = status;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public EquipeDestino getEquipeDestino() {
        return equipeDestino;
    }

    public void setEquipeDestino(EquipeDestino equipeDestino) {
        this.equipeDestino = equipeDestino;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public void setIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }
}
