package br.unicamp.mc322.strategy;

import java.util.List;

import br.unicamp.mc322.model.producao.Demanda;
import br.unicamp.mc322.model.producao.StatusDemanda;

/**
 * Contrato para os algoritmos que escolhem qual demanda fabricar a seguir (padrão Strategy).
 * <p>
 * O {@link br.unicamp.mc322.service.GerenciadorProducao} guarda uma estratégia e pode trocá-la em tempo de
 * execução; cada implementação define o critério de prioridade.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public interface EstrategiaProducao {

    /**
     * Escolhe a próxima demanda a ser fabricada.
     *
     * @param demandas demandas registradas, na ordem em que foram criadas
     * @param orcamentoDisponivel orçamento (budget) atual da fábrica
     * @return a demanda escolhida, ou {@code null} se nenhuma for elegível
     */
    Demanda selecionarDemanda(List<Demanda> demandas, double orcamentoDisponivel);

    /**
     * Nome da estratégia, mostrado nos menus.
     *
     * @return nome da estratégia
     */
    String getNomeEstrategia();

    /**
     * Filtro comum a todas as estratégias: só são elegíveis demandas pendentes
     * e com pelo menos um instrumento.
     *
     * @param demanda demanda a ser avaliada
     * @return {@code true} se a demanda está pendente e tem quantidade maior que zero
     */
    default boolean estaElegivel(Demanda demanda) {
        return demanda.getStatus() == StatusDemanda.PENDENTE && demanda.getQuantidadeProdutos() > 0;
    }
}
