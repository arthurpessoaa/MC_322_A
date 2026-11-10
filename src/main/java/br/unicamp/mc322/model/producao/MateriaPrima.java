package br.unicamp.mc322.model.producao;

import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.RecursoInsuficienteException;

/**
 * Matéria-prima mantida em estoque (madeira, cordas, alumínio etc.), com quantidade,
 * unidade de medida e custo por unidade. O estoque só é alterado depois que todas as
 * validações passam.
 *
 * @author Arthur Lopes Pessoa
 */
public class MateriaPrima {

    private String id;
    private String nome;
    private double quantidade;
    private String unidade;
    private double custoPorUnidade;

    /**
     * Cria uma matéria-prima.
     *
     * @param id identificador da matéria-prima
     * @param nome nome da matéria-prima
     * @param quantidade quantidade inicial em estoque
     * @param unidade unidade de medida (por exemplo, "kg")
     * @param custoPorUnidade custo, em reais, de cada unidade comprada
     */
    public MateriaPrima(
            String id,
            String nome,
            double quantidade,
            String unidade,
            double custoPorUnidade
    ) {
        this.id = id;
        this.nome = nome;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.custoPorUnidade = custoPorUnidade;
    }

    /**
     * Verifica se o estoque cobre a quantidade pedida, sem alterá-lo.
     *
     * @param quantidadeNecessaria quantidade desejada
     * @return {@code true} se o estoque é suficiente
     */
    public boolean verificarDisponibilidade(
            double quantidadeNecessaria
    ) {
        return quantidade >= quantidadeNecessaria;
    }

    /**
     * Retira uma quantidade do estoque. O estoque só é alterado depois de todas as validações.
     *
     * @param quantidadeConsumida quantidade a retirar, maior que zero
     * @throws EntradaInvalidaException se a quantidade não for positiva
     * @throws RecursoInsuficienteException se o estoque não cobrir a quantidade
     */
    public void consumir(double quantidadeConsumida) {
        if (!(quantidadeConsumida > 0.0)) {
            throw new EntradaInvalidaException("A quantidade consumida deve ser positiva.");
        }

        if (!verificarDisponibilidade(quantidadeConsumida)) {
            throw new RecursoInsuficienteException(
                    "Estoque insuficiente de " + nome + ": necessário " + quantidadeConsumida
                            + " " + unidade + ", disponível " + quantidade + " " + unidade + "."
            );
        }

        quantidade -= quantidadeConsumida;

        System.out.printf(
                "%.2f %s de %s foram consumidos.%n",
                quantidadeConsumida,
                unidade,
                nome
        );
    }

    /**
     * Adiciona uma quantidade ao estoque.
     *
     * @param quantidadeAdicionada quantidade a adicionar, positiva e finita
     * @throws EntradaInvalidaException se a quantidade não for um número positivo
     */
    public void adicionarEstoque(double quantidadeAdicionada) {
        if (!(quantidadeAdicionada > 0.0) || Double.isInfinite(quantidadeAdicionada)) {
            throw new EntradaInvalidaException("A quantidade adicionada deve ser um número positivo.");
        }

        quantidade += quantidadeAdicionada;

        System.out.printf(
                "%.2f %s de %s foram adicionados ao estoque.%n",
                quantidadeAdicionada,
                unidade,
                nome
        );
    }

    /**
     * Código da matéria-prima (por exemplo, {@code MP01}).
     *
     * @return código da matéria-prima
     */
    public String getId() {
        return id;
    }

    /**
     * Nome da matéria-prima.
     *
     * @return nome da matéria-prima
     */
    public String getNome() {
        return nome;
    }

    /**
     * Quantidade disponível em estoque, na unidade de {@link #getUnidade()}.
     *
     * @return quantidade em estoque
     */
    public double getQuantidade() {
        return quantidade;
    }

    /**
     * Unidade de medida do estoque (kg, unidades, m²).
     *
     * @return unidade de medida
     */
    public String getUnidade() {
        return unidade;
    }

    /**
     * Preço de cada unidade, em reais, usado nas compras.
     *
     * @return custo por unidade
     */
    public double getCustoPorUnidade() {
        return custoPorUnidade;
    }
}
