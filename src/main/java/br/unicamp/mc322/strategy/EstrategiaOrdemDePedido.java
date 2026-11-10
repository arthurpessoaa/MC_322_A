package br.unicamp.mc322.strategy;

import java.util.List;
import br.unicamp.mc322.model.producao.Demanda;

/**
 * Estratégia "primeiro a chegar, primeiro a ser atendido": escolhe a primeira demanda elegível
 * da lista, na ordem de registro.
 *
 * @author Arthur Lopes Pessoa
 */
public class EstrategiaOrdemDePedido implements EstrategiaProducao {

    /**
     * Construtor padrão da estratégia por ordem de pedido.
     */
    public EstrategiaOrdemDePedido() {
    }

    /**
     * Retorna a primeira demanda elegível da lista.
     *
     * @param demandas demandas registradas, na ordem de criação
     * @param orcamentoDisponivel orçamento atual (não influencia esta estratégia)
     * @return a primeira demanda elegível, ou {@code null} se não houver nenhuma
     */
    @Override
    public Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel) {
        for (Demanda demanda : demandas) {
            if (estaElegivel(demanda)) {
                return demanda;
            }
        }
        return null;
    }

    /**
     * Nome mostrado nos menus.
     *
     * @return {@code "Ordem de Pedido"}
     */
    @Override
    public String getNomeEstrategia() {
        return "Ordem de Pedido";
    }
}
