package br.unicamp.mc322.model.maquinas;
import br.unicamp.mc322.model.produtos.Produto;

/**
 * Máquina de inspeção sonora: última etapa da linha, que aprova ou rejeita o produto.
 * Se a própria máquina falhar, o resultado da inspeção é invertido.
 *
 * @author Arthur Lopes Pessoa
 */
public class MaquinaInspecaoSonora extends Maquina {
    /**
     * Cria uma máquina de inspeção sonora.
     *
     * @param nome nome da máquina
     * @param ligada {@code true} se começa ligada
     * @param capacidadeMaxima quantidade máxima de instrumentos por lote
     * @param probabilidadeFalha probabilidade-base de falha, entre 0 e 1
     * @param custoOperacao custo, em reais, de operar a máquina em um instrumento
     */
    public MaquinaInspecaoSonora(
            String nome,
            boolean ligada,
            double capacidadeMaxima,
            double probabilidadeFalha,
            double custoOperacao
    ) {
        super(nome, ligada, capacidadeMaxima, probabilidadeFalha, custoOperacao, "Sonda", 3);
    }

    /**
     * Inspeciona o produto. A chance de rejeição é a probabilidade de falha acumulada do produto
     * somada a 10% da sua qualidade, limitada a 1. Se a máquina falhar, o resultado é invertido.
     *
     * @param produto produto a ser inspecionado
     * @return {@code true} se o produto foi aprovado
     */
    @Override
    protected boolean executarProcessamento(Produto produto) {
        double chanceRejeicao = produto.getProbabilidadeFalhaAcumulada() + produto.getQualidade() * 0.1;

        if (chanceRejeicao > 1.0) {
            chanceRejeicao = 1.0;
        }

        boolean aprovado = !sortearEvento(chanceRejeicao);

        if (verificarFalha()) {
            System.out.println("A máquina de inspeção falhou! O resultado da inspeção de " + produto.getNome() + " foi invertido.");
            aprovado = !aprovado;
        }

        return aprovado;
    }

    /**
     * Tipo da máquina, usado nos relatórios de auditoria.
     *
     * @return {@code "Máquina de Inspeção Sonora"}
     */
    @Override
    public String getTipo() {
        return "Máquina de Inspeção Sonora";
    }
}
