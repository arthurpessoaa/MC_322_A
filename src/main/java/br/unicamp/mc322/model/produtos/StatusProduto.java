package br.unicamp.mc322.model.produtos;

/**
 * Etapas pelas quais um instrumento passa na linha de produção.
 *
 * @author Arthur Lopes Pessoa
 */
public enum StatusProduto {
    /**
     * Criado e ainda não processado.
     */
    AGUARDANDO("Aguardando"),
    /**
     * Montado na máquina de montagem.
     */
    PROCESSADO("Processado"),
    /**
     * Embalado na máquina de embalagem.
     */
    EMBALADO("Embalado"),
    /**
     * Aprovado na inspeção sonora; vai para o armazém.
     */
    APROVADO("Aprovado"),
    /**
     * Reprovado na inspeção sonora; é descartado.
     */
    REJEITADO("Rejeitado");

    private final String descricao;

    StatusProduto(String descricao) {
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
