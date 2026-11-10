package br.unicamp.mc322.model.produtos;

/**
 * Linhas de produção da fábrica. Linhas mais sofisticadas gastam mais matéria-prima.
 *
 * @author Arthur Lopes Pessoa
 */
public enum LinhaProduto {
    /**
     * Linha Studio: acabamento de estúdio (fator de matéria-prima 2,0).
     */
    STUDIO("Linha Studio", 2.0),
    /**
     * Linha Palco: feita para shows e ensaios (fator 1,5).
     */
    PALCO("Linha Palco", 1.5),
    /**
     * Linha Clássica: simples e resistente (fator 1,0).
     */
    CLASSICA("Linha Clássica", 1.0);

    private final String nome;
    private final double fatorMateriaPrima;

    LinhaProduto(String nome, double fatorMateriaPrima) {
        this.nome = nome;
        this.fatorMateriaPrima = fatorMateriaPrima;
    }

    /**
     * Nome da linha mostrado nos menus.
     *
     * @return nome da linha
     */
    public String getNome() {
        return nome;
    }
    /**
     * Fator que multiplica o consumo de matéria-prima do instrumento: linhas mais sofisticadas gastam mais.
     *
     * @return fator de consumo
     */
    public double getFatorMateriaPrima() {
        return fatorMateriaPrima;
    }
}
