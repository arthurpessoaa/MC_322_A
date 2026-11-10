package br.unicamp.mc322.model.maquinas;
import br.unicamp.mc322.model.produtos.Produto;
import br.unicamp.mc322.model.produtos.StatusProduto;

/**
 * Máquina de embalagem: segunda etapa da linha. Marca o produto como embalado e, se falhar,
 * deixa a embalagem mal feita, aumentando a probabilidade de falha acumulada do produto.
 *
 * @author Arthur Lopes Pessoa
 */
public class MaquinaEmbalagem extends Maquina {
    /**
     * Cria uma máquina de embalagem.
     *
     * @param nome nome da máquina
     * @param ligada {@code true} se começa ligada
     * @param capacidadeMaxima quantidade máxima de instrumentos por lote
     * @param probabilidadeFalha probabilidade-base de falha, entre 0 e 1
     * @param custoOperacao custo, em reais, de operar a máquina em um instrumento
     */
    public MaquinaEmbalagem(
            String nome,
            boolean ligada,
            double capacidadeMaxima,
            double probabilidadeFalha,
            double custoOperacao
    ) {
        super(nome, ligada, capacidadeMaxima, probabilidadeFalha, custoOperacao, "Selador", 2);
    }

    /**
     * Tipo da máquina, usado nos relatórios de auditoria.
     *
     * @return {@code "Maquina de Embalagem"}
     */
    @Override
    public String getTipo() {
        return "Maquina de Embalagem";
    }

    /**
     * Embala o produto. Se ocorrer falha, a probabilidade de falha acumulada do produto
     * aumenta em 0,1.
     *
     * @param produto produto a ser embalado
     * @return sempre {@code true}: a embalagem não reprova produtos
     */
    @Override
    protected boolean executarProcessamento(Produto produto) {
        if (verificarFalha()) {
            produto.aumentarProbabilidadeFalha(0.1);
            System.out.println("A embalagem de " + produto.getNome() + " ficou mal feita. A probabilidade de falha do produto aumentou em 0.1.");
        }

        produto.setStatus(StatusProduto.EMBALADO);
        System.out.println("Produto " + produto.getNome() + " embalado na máquina de embalagem.");

        return true;
    }
}
