package br.unicamp.mc322.strategy;

import java.util.List;
import br.unicamp.mc322.model.producao.Demanda;

/**
 * Estratégia do Orçamento: escolhe a demanda que rende mais instrumentos com o orçamento
 * que sobrou em caixa. Demandas em que nem um instrumento cabe no orçamento são ignoradas.
 *
 * @author Arthur Lopes Pessoa
 */
public class EstrategiaOrcamento implements EstrategiaProducao {

    private double custoPorProduto;

    /**
     * Cria a estratégia com o custo de operação de um instrumento.
     *
     * @param custoPorProduto custo, em reais, para fabricar um instrumento
     *         (soma do custo de operação das máquinas da linha)
     */
    public EstrategiaOrcamento(double custoPorProduto) {
        this.custoPorProduto = custoPorProduto;
    }

    /**
     * Retorna a demanda elegível cujo pedido o orçamento consegue cobrir em maior quantidade
     * (ver {@link Demanda#calcularProdutosViaveis(double, double)}).
     *
     * @param demandas demandas registradas, na ordem de criação
     * @param orcamentoDisponivel orçamento atual da fábrica
     * @return a melhor demanda para o orçamento, ou {@code null} se nenhuma for elegível
     *         ou se o orçamento não cobrir nem um instrumento
     */
    @Override
    public Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel) {
        Demanda escolhida = null;
        int maiorProducao = 0;

        for (Demanda demanda : demandas) {
            // Pula demandas não pendentes ou em que nem um instrumento cabe no budget
            if (!estaElegivel(demanda) || !demanda.temOrcamento(orcamentoDisponivel, custoPorProduto)) {
                continue;
            }

            // A própria demanda sabe calcular quanto do pedido o orçamento cobre
            int viaveis = demanda.calcularProdutosViaveis(orcamentoDisponivel, custoPorProduto);

            if (viaveis > maiorProducao) {
                maiorProducao = viaveis;
                escolhida = demanda;
            }
        }

        return escolhida;
    }

    /**
     * Nome mostrado nos menus.
     *
     * @return {@code "Estratégia do Orçamento"}
     */
    @Override
    public String getNomeEstrategia() {
        return "Estratégia do Orçamento";
    }
}
