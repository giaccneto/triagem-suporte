package com.giaccneto.triagem_suporte.dto;

public class ClassificacaoResposta {

    private String categoria;
    private String prioridade;
    private String equipeDestino;
    private String justificativa;
    private Boolean revisaoNecessaria;

    public ClassificacaoResposta() {
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getEquipeDestino() {
        return equipeDestino;
    }

    public void setEquipeDestino(String equipeDestino) {
        this.equipeDestino = equipeDestino;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public Boolean getRevisaoNecessaria() {
        return revisaoNecessaria;
    }

    public void setRevisaoNecessaria(Boolean revisaoNecessaria) {
        this.revisaoNecessaria = revisaoNecessaria;
    }
}
