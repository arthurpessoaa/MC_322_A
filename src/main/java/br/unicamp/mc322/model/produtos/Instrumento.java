package br.unicamp.mc322.model.produtos;

/**
 * Instrumentos fabricados pela Harmonia, cada um com seus dados de produção:
 * matéria-prima usada, consumo por unidade e tempo de produção.
 *
 * @author Arthur Lopes Pessoa
 */
public enum Instrumento {
    /**
     * Violão, feito de mogno.
     */
    VIOLAO   ("Violão",   "mogno",        4.0,         3.0),
    /**
     * Guitarra, feita de cedro.
     */
    GUITARRA ("Guitarra", "cedro",        5.0,         5.0),
    /**
     * Ukulele, feito de cordas de nylon.
     */
    UKULELE  ("Ukulele",  "nylon",        2.0,         2.0),
    /**
     * Bateria, feita de alumínio.
     */
    BATERIA  ("Bateria",  "alumínio",    12.0,        12.0),
    /**
     * Pandeiro, feito de couro sintético.
     */
    PANDEIRO ("Pandeiro", "couro",        1.0,         1.0),
    /**
     * Flauta, feita de plástico ABS.
     */
    FLAUTA   ("Flauta",   "plástico",     0.3,         0.3);

    private final String nome;
    private final String materiaPrima; // trecho do nome da matéria-prima usada
    private final double materiaPrimaPorUnidade;
    private final double tempoProducao;

    Instrumento(String nome, String materiaPrima, double materiaPrimaPorUnidade, double tempoProducao) {
        this.nome = nome;
        this.materiaPrima = materiaPrima;
        this.materiaPrimaPorUnidade = materiaPrimaPorUnidade;
        this.tempoProducao = tempoProducao;
    }

    /**
     * Nome do instrumento mostrado nos menus.
     *
     * @return nome do instrumento
     */
    public String getNome() {
        return nome;
    }
    /**
     * Trecho do nome da matéria-prima usada. O gerenciador procura no estoque a matéria-prima
     * cujo nome contém esse trecho.
     *
     * @return trecho do nome da matéria-prima (por exemplo, {@code "mogno"})
     */
    public String getMateriaPrima() {
        return materiaPrima;
    }
    /**
     * Matéria-prima gasta por instrumento, antes de aplicar o fator da linha.
     *
     * @return quantidade de matéria-prima por unidade
     */
    public double getMateriaPrimaPorUnidade() {
        return materiaPrimaPorUnidade;
    }
    /**
     * Tempo-base de produção do instrumento.
     *
     * @return tempo-base de produção
     */
    public double getTempoProducao() {
        return tempoProducao;
    }
}
