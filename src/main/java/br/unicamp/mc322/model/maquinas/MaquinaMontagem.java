package br.unicamp.mc322.model.maquinas;
import br.unicamp.mc322.model.produtos.Produto;

/**
 * Máquina de montagem: primeira etapa da linha. Processa o produto e, se falhar, deixa
 * imperfeições que aumentam a probabilidade de falha acumulada do produto.
 *
 * @author Arthur Lopes Pessoa
 */
public class MaquinaMontagem extends Maquina {
    /**
     * Cria uma máquina de montagem.
     *
     * @param nome nome da máquina
     * @param ligada {@code true} se começa ligada
     * @param capacidadeMaxima quantidade máxima de instrumentos por lote
     * @param probabilidadeFalha probabilidade-base de falha, entre 0 e 1
     * @param custoOperacao custo, em reais, de operar a máquina em um instrumento
     */
    public MaquinaMontagem(
            String nome,
            boolean ligada,
            double capacidadeMaxima,
            double probabilidadeFalha,
            double custoOperacao
    ) {
        super(nome, ligada, capacidadeMaxima, probabilidadeFalha, custoOperacao, "Garra", 3);
    }

    /**
     * Tipo da máquina, usado nos relatórios de auditoria.
     *
     * @return {@code "Maquina de Montagem"}
     */
    @Override
    public String getTipo() {
        return "Maquina de Montagem";
    }

    /**
     * Monta o produto (chama {@link Produto#processar()}). Se ocorrer falha, a probabilidade
     * de falha acumulada do produto aumenta em 0,3.
     *
     * @param produto produto a ser montado
     * @return sempre {@code true}: a montagem não reprova produtos
     */
    @Override
    protected boolean executarProcessamento(Produto produto){
        produto.processar();

        if (verificarFalha()) {
            produto.aumentarProbabilidadeFalha(0.3);
            System.out.println("A montagem de " + produto.getNome() + " deixou imperfeições. A probabilidade de falha do produto aumentou em 0.3.");
        }

        System.out.println("Produto " + produto.getNome() + " processado na máquina de montagem.");

        return true;
    }
}
