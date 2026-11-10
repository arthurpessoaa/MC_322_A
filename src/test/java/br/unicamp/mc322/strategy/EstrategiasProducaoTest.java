package br.unicamp.mc322.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.unicamp.mc322.model.producao.Demanda;
import br.unicamp.mc322.model.produtos.Instrumento;
import br.unicamp.mc322.model.produtos.LinhaProduto;

/** As três implementações do padrão Strategy escolhem a próxima demanda de formas diferentes. */
class EstrategiasProducaoTest {

    private Demanda pequena;
    private Demanda grande;
    private Demanda media;
    private List<Demanda> demandas;

    @BeforeEach
    void criarDemandas() {
        pequena = new Demanda(LinhaProduto.CLASSICA, 2, Instrumento.VIOLAO);
        grande = new Demanda(LinhaProduto.PALCO, 10, Instrumento.GUITARRA);
        media = new Demanda(LinhaProduto.STUDIO, 5, Instrumento.UKULELE);
        demandas = new ArrayList<>(List.of(pequena, grande, media));
    }

    // Ordem de Pedido

    @Test
    void ordemDePedidoEscolheAPrimeiraDemandaPendente() {
        assertSame(pequena, new EstrategiaOrdemDePedido().selecionarDemanda(demandas, 5000.0));
    }

    @Test
    void ordemDePedidoPulaDemandasNaoElegiveis() {
        pequena.cancelar();

        assertSame(grande, new EstrategiaOrdemDePedido().selecionarDemanda(demandas, 5000.0));
    }

    // Maior Encomenda

    @Test
    void maiorEncomendaEscolheAMaiorQuantidade() {
        assertSame(grande, new EstrategiaMaiorEncomenda().selecionarDemanda(demandas, 5000.0));
    }

    @Test
    void maiorEncomendaIgnoraDemandaCanceladaOuZerada() {
        grande.cancelar();
        pequena.atualizarQuantidade(0);

        assertSame(media, new EstrategiaMaiorEncomenda().selecionarDemanda(demandas, 5000.0));
    }

    // Orçamento

    @Test
    void orcamentoEscolheADemandaQueMaisProduzComOBudget() {
        // custo 100 por produto, budget 1000: viáveis = 2, 10 e 5
        assertSame(grande, new EstrategiaOrcamento(100.0).selecionarDemanda(demandas, 1000.0));
    }

    @Test
    void orcamentoLimitaAProducaoViavelPeloBudget() {
        // budget 250: viáveis = 2, 2 e 2 -> empate, vale a primeira
        assertSame(pequena, new EstrategiaOrcamento(100.0).selecionarDemanda(demandas, 250.0));
    }

    @Test
    void orcamentoDevolveNuloQuandoNemUmProdutoCabeNoBudget() {
        assertNull(new EstrategiaOrcamento(100.0).selecionarDemanda(demandas, 50.0));
    }

    // comportamento comum

    @Test
    void semDemandasElegiveisTodasDevolvemNulo() {
        List<Demanda> vazia = new ArrayList<>();
        pequena.cancelar();
        grande.cancelar();
        media.cancelar();

        for (EstrategiaProducao estrategia : List.of(
                new EstrategiaOrdemDePedido(), new EstrategiaMaiorEncomenda(), new EstrategiaOrcamento(10.0))) {
            assertNull(estrategia.selecionarDemanda(vazia, 5000.0));
            assertNull(estrategia.selecionarDemanda(demandas, 5000.0));
        }
    }

    @Test
    void cadaEstrategiaTemSeuNome() {
        assertEquals("Ordem de Pedido", new EstrategiaOrdemDePedido().getNomeEstrategia());
        assertEquals("Maior Encomenda", new EstrategiaMaiorEncomenda().getNomeEstrategia());
        assertEquals("Estratégia do Orçamento", new EstrategiaOrcamento(10.0).getNomeEstrategia());
    }
}
