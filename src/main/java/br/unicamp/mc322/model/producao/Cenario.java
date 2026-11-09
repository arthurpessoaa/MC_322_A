package br.unicamp.mc322.model.producao;

/**
 * Cenários de operação da fábrica. Cada um define o orçamento inicial e os multiplicadores
 * de probabilidade de falha e de desgaste das máquinas.
 *
 * @author Arthur Lopes Pessoa
 */
public enum Cenario {
    /**
     * Orçamento farto (R$ 5000) e poucas falhas (multiplicadores 0,5).
     */
    IDEAL("Ideal", 5000.0, 0.5, 0.5),
    /**
     * Orçamento curto (R$ 500) e máquinas instáveis (multiplicadores 2,0).
     */
    APOCALIPTICO("Apocalíptico", 500.0, 2.0, 2.0);

    private final String nome;
    private final double budgetInicial;
    private final double multiplicadorFalha;
    private final double multiplicadorDesgaste;

    Cenario(String nome, double budgetInicial, double multiplicadorFalha, double multiplicadorDesgaste) {
        this.nome = nome;
        this.budgetInicial = budgetInicial;
        this.multiplicadorFalha = multiplicadorFalha;
        this.multiplicadorDesgaste = multiplicadorDesgaste;
    }

    /**
     * Nome do cenário mostrado no menu.
     *
     * @return nome do cenário
     */
    public String getNome() {
        return nome;
    }
    /**
     * Orçamento com que a fábrica começa neste cenário.
     *
     * @return orçamento inicial em reais
     */
    public double getBudgetInicial() {
        return budgetInicial;
    }
    /**
     * Fator aplicado à chance de falha das máquinas: abaixo de 1 reduz as falhas e acima de 1 aumenta.
     *
     * @return multiplicador de falha
     */
    public double getMultiplicadorFalha() {
        return multiplicadorFalha;
    }
    /**
     * Fator aplicado ao desgaste das máquinas: abaixo de 1 desgasta menos e acima de 1 desgasta mais.
     *
     * @return multiplicador de desgaste
     */
    public double getMultiplicadorDesgaste() {
        return multiplicadorDesgaste;
    }
}
