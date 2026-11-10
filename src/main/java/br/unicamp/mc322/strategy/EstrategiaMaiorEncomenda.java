package br.unicamp.mc322.strategy;

import java.util.List;
import br.unicamp.mc322.model.producao.Demanda;

/**
 * Estratégia que prioriza o pedido com mais instrumentos (útil para encomendas grandes,
 * como as de escolas de música). Em caso de empate, vence a demanda registrada primeiro.
 *
 * @author Arthur Lopes Pessoa
 */
public class EstrategiaMaiorEncomenda implements EstrategiaProducao {

    /**
     * Construtor padrão da estratégia de maior encomenda.
     */
    public EstrategiaMaiorEncomenda() {
    }

    /**
     * Retorna a demanda elegível com a maior quantidade de instrumentos.
     *
     * @param demandas demandas registradas, na ordem de criação
     * @param orcamentoDisponivel orçamento atual (não influencia esta estratégia)
     * @return a demanda elegível de maior quantidade, ou {@code null} se não houver nenhuma
     */
    @Override
    public Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel) {
        Demanda escolhida = null;

        for (Demanda demanda : demandas) {
            if (!estaElegivel(demanda)) {
                continue;
            }

            if (escolhida == null || demanda.getQuantidadeProdutos() > escolhida.getQuantidadeProdutos()) {
                escolhida = demanda;
            }
        }

        return escolhida;
    }
    
    /**
     * Nome mostrado nos menus.
     *
     * @return {@code "Maior Encomenda"}
     */
    @Override
    public String getNomeEstrategia() {
        return "Maior Encomenda";
    }
}