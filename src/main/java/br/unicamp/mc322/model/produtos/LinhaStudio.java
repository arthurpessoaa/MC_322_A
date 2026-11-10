package br.unicamp.mc322.model.produtos;
/**
 * Instrumento da Linha Studio, com qualidade 0,9. O tempo de produção é 2 vezes o tempo-base do instrumento.
 *
 * @author Arthur Lopes Pessoa
 */
public class LinhaStudio extends Produto{
    /**
     * Cria um instrumento da Linha Studio.
     *
     * @param id identificador do produto
     * @param nome nome do instrumento
     * @param status status inicial
     * @param quantidadeMateriaPrimaPorUnidade matéria-prima gasta neste produto
     * @param tempoProducao tempo-base de produção do instrumento
     */
    public LinhaStudio(
            String id,
            String nome,
            StatusProduto status,
            double quantidadeMateriaPrimaPorUnidade,
            double tempoProducao
    ) {
        super(id, nome, status, quantidadeMateriaPrimaPorUnidade, 0.9, tempoProducao);
    }

    /**
     * Realiza a montagem e o acabamento de estúdio e marca o produto como
     * {@code PROCESSADO}.
     */
    @Override
    public void processar() {
        System.out.println(
                "Realizando montagem e acabamento de estúdio de " + getNome()
        );
        setStatus(StatusProduto.PROCESSADO);
    }

    /**
     * Nome da linha, usado nos relatórios e no armazém.
     *
     * @return {@code "Linha Studio"}
     */
    @Override
    public String getTipo() {
        return "Linha Studio";
    }

    /**
     * Calcula o tempo de produção: 2 vezes o tempo-base.
     *
     * @return tempo de produção
     */
    @Override
    public double calcularTempoProducao() {
        return 2 * getTempoProducao();
    }

}
