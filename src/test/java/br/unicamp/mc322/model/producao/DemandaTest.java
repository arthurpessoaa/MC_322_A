package br.unicamp.mc322.model.producao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.unicamp.mc322.exception.DemandaInvalidaException;
import br.unicamp.mc322.model.produtos.Instrumento;
import br.unicamp.mc322.model.produtos.LinhaProduto;

/** Cálculos e máquina de estados ({@link StatusDemanda}) da {@link Demanda}. */
class DemandaTest {

    private static final double DELTA = 0.0001;

    private Demanda novaDemanda(int quantidade) {
        return new Demanda(LinhaProduto.STUDIO, quantidade, Instrumento.VIOLAO);
    }

    // criação

    @Test
    void demandaNovaComecaPendente() {
        Demanda demanda = novaDemanda(3);

        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
        assertEquals(3, demanda.getQuantidadeProdutos());
        assertEquals(LinhaProduto.STUDIO, demanda.getLinha());
        assertEquals(Instrumento.VIOLAO, demanda.getInstrumento());
    }

    @Test
    void quantidadeNegativaNaCriacaoLancaExcecao() {
        assertThrows(DemandaInvalidaException.class, () -> novaDemanda(-1));
    }

    @Test
    void quantidadeZeroEPermitida() {
        assertEquals(0, novaDemanda(0).getQuantidadeProdutos());
    }

    // cálculos

    @Test
    void materiaPrimaPorUnidadeMultiplicaInstrumentoPeloFatorDaLinha() {
        // violão = 4,0 kg; linha Studio = fator 2,0
        assertEquals(8.0, novaDemanda(3).calcularMateriaPrimaPorUnidade(), DELTA);
    }

    @Test
    void materiaPrimaNecessariaMultiplicaPelaQuantidade() {
        assertEquals(24.0, novaDemanda(3).calcularMateriaPrimaNecessaria(), DELTA);
    }

    @Test
    void custoEstimadoMultiplicaPelaQuantidade() {
        assertEquals(300.0, novaDemanda(3).calcularCustoEstimado(100.0), DELTA);
    }

    @Test
    void produtosViaveisLimitadosPeloOrcamento() {
        Demanda demanda = novaDemanda(10);

        assertEquals(3, demanda.calcularProdutosViaveis(350.0, 100.0));
    }

    @Test
    void produtosViaveisCobremADemandaQuandoOOrcamentoSobra() {
        assertEquals(10, novaDemanda(10).calcularProdutosViaveis(5000.0, 100.0));
    }

    @Test
    void produtosViaveisZeroQuandoNemUmProdutoCabeNoOrcamento() {
        Demanda demanda = novaDemanda(10);

        assertFalse(demanda.temOrcamento(50.0, 100.0));
        assertEquals(0, demanda.calcularProdutosViaveis(50.0, 100.0));
    }

    // atualização de quantidade

    @Test
    void atualizarQuantidadeAlteraOValor() {
        Demanda demanda = novaDemanda(3);

        demanda.atualizarQuantidade(7);

        assertEquals(7, demanda.getQuantidadeProdutos());
    }

    @Test
    void atualizarQuantidadeNegativaLancaExcecaoEMantemOValor() {
        Demanda demanda = novaDemanda(3);

        assertThrows(DemandaInvalidaException.class, () -> demanda.atualizarQuantidade(-5));

        assertEquals(3, demanda.getQuantidadeProdutos());
    }

    @Test
    void novaQuantidadeReativaDemandaCancelada() {
        Demanda demanda = novaDemanda(3);
        demanda.cancelar();

        demanda.atualizarQuantidade(5);

        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
    }

    @Test
    void quantidadeZeroNaoReativaDemandaCancelada() {
        Demanda demanda = novaDemanda(3);
        demanda.cancelar();

        demanda.atualizarQuantidade(0);

        assertEquals(StatusDemanda.CANCELADA, demanda.getStatus());
    }

    // transições de status

    @Test
    void cicloNormalPendenteEmProducaoConcluida() {
        Demanda demanda = novaDemanda(3);

        demanda.iniciarProducao();
        assertEquals(StatusDemanda.EM_PRODUCAO, demanda.getStatus());

        demanda.concluir();
        assertEquals(StatusDemanda.CONCLUIDA, demanda.getStatus());
    }

    @Test
    void producaoParcialVoltaParaPendente() {
        Demanda demanda = novaDemanda(3);
        demanda.iniciarProducao();

        demanda.voltarParaPendente();

        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
    }

    @Test
    void iniciarProducaoDeDemandaQueNaoEstaPendenteLancaExcecao() {
        Demanda demanda = novaDemanda(3);
        demanda.iniciarProducao();

        assertThrows(DemandaInvalidaException.class, demanda::iniciarProducao);
    }

    @Test
    void concluirDemandaPendenteLancaExcecao() {
        assertThrows(DemandaInvalidaException.class, () -> novaDemanda(3).concluir());
    }

    @Test
    void voltarParaPendenteSoValeParaDemandaEmProducao() {
        assertThrows(DemandaInvalidaException.class, () -> novaDemanda(3).voltarParaPendente());
    }

    @Test
    void cancelarDemandaPendenteFunciona() {
        Demanda demanda = novaDemanda(3);

        demanda.cancelar();

        assertEquals(StatusDemanda.CANCELADA, demanda.getStatus());
    }

    @Test
    void cancelarDemandaConcluidaLancaExcecao() {
        Demanda demanda = novaDemanda(3);
        demanda.iniciarProducao();
        demanda.concluir();

        assertThrows(DemandaInvalidaException.class, demanda::cancelar);
        assertEquals(StatusDemanda.CONCLUIDA, demanda.getStatus());
    }

    @Test
    void restaurarStatusAplicaOStatusRecuperado() {
        Demanda demanda = novaDemanda(3);

        demanda.restaurarStatus(StatusDemanda.EM_PRODUCAO);

        assertEquals(StatusDemanda.EM_PRODUCAO, demanda.getStatus());
    }

    @Test
    void restaurarStatusNuloLancaExcecao() {
        Demanda demanda = novaDemanda(3);

        assertThrows(DemandaInvalidaException.class, () -> demanda.restaurarStatus(null));
        assertTrue(demanda.getStatus() == StatusDemanda.PENDENTE);
    }
}
