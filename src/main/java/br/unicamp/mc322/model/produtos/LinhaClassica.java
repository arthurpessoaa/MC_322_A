package br.unicamp.mc322.model.produtos;
/**
 * Instrumento da Linha Clássica, com qualidade 0,5. O tempo de produção é 1 vez o tempo-base do instrumento.
 *
 * @author Arthur Lopes Pessoa
 */
public class LinhaClassica extends Produto{
    /**
     * Cria um instrumento da Linha Clássica.
     *
     * @param id identificador do produto
     * @param nome nome do instrumento
     * @param status status inicial
     * @param quantidadeMateriaPrimaPorUnidade matéria-prima gasta neste produto
     * @param tempoProducao tempo-base de produção do instrumento
     */
    public LinhaClassica (
            String id,
            String nome,
            StatusProduto status,
            double quantidadeMateriaPrimaPorUnidade,
            double tempoProducao
    ) {
        super(id, nome, status, quantidadeMateriaPrimaPorUnidade, 0.5, tempoProducao);
    }

    /**
     * Realiza a montagem e o acabamento clássico e marca o produto como
     * {@code PROCESSADO}.
     */
    @Override
    public void processar() {
        System.out.println(
                "Realizando montagem e acabamento clássico de " + getNome()
        );
        setStatus(StatusProduto.PROCESSADO);
    }

    /**
     * Nome da linha, usado nos relatórios e no armazém.
     *
     * @return {@code "Linha Clássica"}
     */
    @Override
    public String getTipo() {
        return "Linha Clássica";
    }

    /**
     * Calcula o tempo de produção: 1 vez o tempo-base.
     *
     * @return tempo de produção
     */
    @Override
    public double calcularTempoProducao() {
        return 1 * getTempoProducao();
    }

}
