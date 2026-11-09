package br.unicamp.mc322.model.producao;

import br.unicamp.mc322.exception.DemandaInvalidaException;
import br.unicamp.mc322.model.produtos.Instrumento;
import br.unicamp.mc322.model.produtos.LinhaProduto;

/**
 * Pedido de uma quantidade de um instrumento em uma linha de produto.
 * <p>
 * A demanda controla seu próprio ciclo de vida: {@code PENDENTE} para {@code EM_PRODUCAO}
 * e depois {@code CONCLUIDA} (ou de volta a {@code PENDENTE} se a produção foi parcial);
 * pode ser {@code CANCELADA} enquanto não estiver concluída. Transições incompatíveis com o
 * estado atual são recusadas com {@link DemandaInvalidaException}.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class Demanda {

    private LinhaProduto linha;
    private int quantidadeProdutos;
    private Instrumento instrumento;
    private StatusDemanda status;

    /**
     * Cria uma demanda pendente.
     *
     * @param linha linha de produto pedida
     * @param quantidadeProdutos quantidade de instrumentos pedida
     * @param instrumento instrumento pedido
     * @throws DemandaInvalidaException se a quantidade for negativa
     */
    public Demanda(LinhaProduto linha, int quantidadeProdutos, Instrumento instrumento) {
        this.linha = linha;
        this.instrumento = instrumento;
        this.status = StatusDemanda.PENDENTE;
        atualizarQuantidade(quantidadeProdutos);
    }

    /**
     * Atualiza a quantidade pedida. Uma demanda concluída ou cancelada volta a ficar pendente
     * se receber uma quantidade maior que zero.
     *
     * @param novaQuantidade nova quantidade de instrumentos
     * @throws DemandaInvalidaException se a quantidade for negativa
     */
    public void atualizarQuantidade(int novaQuantidade) {
        if (novaQuantidade < 0) {
            throw new DemandaInvalidaException("A quantidade da demanda não pode ser negativa.");
        }

        quantidadeProdutos = novaQuantidade;

        // Uma demanda concluída ou cancelada volta para a fila se receber nova quantidade
        if (novaQuantidade > 0 && (status == StatusDemanda.CONCLUIDA || status == StatusDemanda.CANCELADA)) {
            status = StatusDemanda.PENDENTE;
        }
    }

    /**
     * Calcula a matéria-prima estimada por instrumento: consumo-base do instrumento multiplicado
     * pelo fator da linha (linhas mais sofisticadas gastam mais).
     *
     * @return matéria-prima necessária por instrumento
     */
    public double calcularMateriaPrimaPorUnidade() {
        return instrumento.getMateriaPrimaPorUnidade() * linha.getFatorMateriaPrima();
    }

    /**
     * Calcula a matéria-prima necessária para a demanda inteira.
     *
     * @return matéria-prima total necessária
     */
    public double calcularMateriaPrimaNecessaria() {
        return quantidadeProdutos * calcularMateriaPrimaPorUnidade();
    }

    /**
     * Calcula o custo estimado da demanda inteira.
     *
     * @param custoPorProduto custo de operação para fabricar um instrumento
     * @return custo total estimado em reais
     */
    public double calcularCustoEstimado(double custoPorProduto) {
        return quantidadeProdutos * custoPorProduto;
    }

    /**
     * Verifica a viabilidade financeira: o orçamento cobre pelo menos um instrumento?
     *
     * @param orcamento orçamento disponível
     * @param custoPorProduto custo de operação para fabricar um instrumento
     * @return {@code true} se o orçamento cobre ao menos um instrumento
     */
    public boolean temOrcamento(double orcamento, double custoPorProduto) {
        return orcamento >= custoPorProduto;
    }

    /**
     * Calcula quantos instrumentos da demanda o orçamento consegue pagar.
     *
     * @param orcamento orçamento disponível
     * @param custoPorProduto custo de operação para fabricar um instrumento
     * @return a quantidade da demanda, se o orçamento cobre tudo; senão, quantos instrumentos cabem
     *         no orçamento (zero se nem um cabe)
     */
    public int calcularProdutosViaveis(double orcamento, double custoPorProduto) {
        if (!temOrcamento(orcamento, custoPorProduto)) {
            return 0;
        }

        if (orcamento >= calcularCustoEstimado(custoPorProduto)) {
            return quantidadeProdutos;
        }

        return (int) (orcamento / custoPorProduto);
    }

    /**
     * Marca a demanda como em produção.
     *
     * @throws DemandaInvalidaException se a demanda não estiver pendente
     */
    public void iniciarProducao() {
        if (status != StatusDemanda.PENDENTE) {
            throw new DemandaInvalidaException(
                    "Não é possível iniciar a produção: a demanda está " + status.getDescricao().toLowerCase() + "."
            );
        }
        status = StatusDemanda.EM_PRODUCAO;
    }

    /**
     * Marca a demanda como concluída.
     *
     * @throws DemandaInvalidaException se a demanda não estiver em produção
     */
    public void concluir() {
        if (status != StatusDemanda.EM_PRODUCAO) {
            throw new DemandaInvalidaException(
                    "Não é possível concluir: a demanda está " + status.getDescricao().toLowerCase() + "."
            );
        }
        status = StatusDemanda.CONCLUIDA;
    }

    /**
     * Devolve à fila uma demanda em produção que foi atendida só em parte.
     *
     * @throws DemandaInvalidaException se a demanda não estiver em produção
     */
    public void voltarParaPendente() {
        if (status != StatusDemanda.EM_PRODUCAO) {
            throw new DemandaInvalidaException(
                    "Só uma demanda em produção pode voltar para a fila; esta está "
                            + status.getDescricao().toLowerCase() + "."
            );
        }
        status = StatusDemanda.PENDENTE;
    }

    /**
     * Cancela a demanda.
     *
     * @throws DemandaInvalidaException se a demanda já estiver concluída
     */
    public void cancelar() {
        if (status == StatusDemanda.CONCLUIDA) {
            throw new DemandaInvalidaException("Uma demanda concluída não pode ser cancelada.");
        }
        status = StatusDemanda.CANCELADA;
    }

    /**
     * Restaura o status da demanda a partir de um valor recuperado do XML.
     *
     * @param statusRecuperado status salvo
     * @throws DemandaInvalidaException se o status for nulo
     */
    public void restaurarStatus(StatusDemanda statusRecuperado) {
        if (statusRecuperado == null) {
            throw new DemandaInvalidaException("O status da demanda não pode ser nulo.");
        }
        this.status = statusRecuperado;
    }

    /**
     * Linha de produto pedida.
     *
     * @return linha de produto
     */
    public LinhaProduto getLinha() {
        return linha;
    }
    /**
     * Quantos instrumentos ainda faltam ser fabricados. O valor diminui conforme os instrumentos
     * são aprovados.
     *
     * @return quantidade de instrumentos pendentes
     */
    public int getQuantidadeProdutos() {
        return quantidadeProdutos;
    }
    /**
     * Instrumento pedido.
     *
     * @return instrumento
     */
    public Instrumento getInstrumento() {
        return instrumento;
    }
    /**
     * Situação atual da demanda.
     *
     * @return status da demanda
     */
    public StatusDemanda getStatus() {
        return status;
    }
}
