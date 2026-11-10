package br.unicamp.mc322.model.producao;

/**
 * Estados possíveis de uma demanda de instrumentos.
 *
 * @author Arthur Lopes Pessoa
 */
public enum StatusDemanda {
    /**
     * Aguardando produção.
     */
    PENDENTE("Pendente"),
    /**
     * Sendo fabricada neste momento.
     */
    EM_PRODUCAO("Em produção"),
    /**
     * Totalmente atendida.
     */
    CONCLUIDA("Concluída"),
    /**
     * Cancelada.
     */
    CANCELADA("Cancelada");

    private final String descricao;

    StatusDemanda(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Texto do status mostrado ao usuário.
     *
     * @return descrição do status
     */
    public String getDescricao() {
        return descricao;
    }
}
