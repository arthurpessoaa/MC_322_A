package br.unicamp.mc322.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.unicamp.mc322.exception.DemandaInvalidaException;
import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.MaquinaIndisponivelException;
import br.unicamp.mc322.exception.RecursoInsuficienteException;
import br.unicamp.mc322.model.maquinas.LinhaDeProducao;
import br.unicamp.mc322.model.producao.Cenario;
import br.unicamp.mc322.model.producao.Demanda;
import br.unicamp.mc322.model.producao.MateriaPrima;
import br.unicamp.mc322.model.producao.StatusDemanda;
import br.unicamp.mc322.model.produtos.Instrumento;
import br.unicamp.mc322.model.produtos.LinhaProduto;
import br.unicamp.mc322.strategy.EstrategiaMaiorEncomenda;
import br.unicamp.mc322.support.Fixtures;

/**
 * Regras de negócio da produção. A linha usada tem máquinas sem falha aleatória (custo total de
 * R$ 35,00 por instrumento) e o budget inicial é o do cenário ideal (R$ 5000,00).
 */
class GerenciadorProducaoTest {

    private static final double DELTA = 0.0001;

    private GerenciadorProducao gerenciador;

    @BeforeEach
    void criarGerenciador() {
        gerenciador = Fixtures.gerenciador();
    }

    private Demanda registrar(LinhaProduto linha, int quantidade, Instrumento instrumento) {
        Demanda demanda = new Demanda(linha, quantidade, instrumento);
        gerenciador.registrarDemanda(demanda);
        return demanda;
    }

    private MateriaPrima mogno() {
        return gerenciador.getMateriasPrimas().get(0);
    }

    // criação

    @Test
    void gerenciadorNovoUsaOBudgetDoCenario() {
        assertEquals(Cenario.IDEAL.getBudgetInicial(), gerenciador.getBudget(), DELTA);
        assertEquals(Cenario.IDEAL, gerenciador.getCenario());
    }

    @Test
    void gerenciadorExigeLinhaComPeloMenosUmaMaquina() {
        assertThrows(EntradaInvalidaException.class,
                () -> new GerenciadorProducao(null, Fixtures.materiasPrimas(), Cenario.IDEAL));
        assertThrows(EntradaInvalidaException.class,
                () -> new GerenciadorProducao(new LinhaDeProducao("Vazia"), Fixtures.materiasPrimas(), Cenario.IDEAL));
    }

    // demandas

    @Test
    void registrarDemandaNovaAdicionaNaLista() {
        registrar(LinhaProduto.CLASSICA, 2, Instrumento.VIOLAO);

        assertEquals(1, gerenciador.getDemandas().size());
    }

    @Test
    void registrarDemandaDoMesmoInstrumentoELinhaAtualizaAExistente() {
        registrar(LinhaProduto.CLASSICA, 2, Instrumento.VIOLAO);

        registrar(LinhaProduto.CLASSICA, 5, Instrumento.VIOLAO);

        assertEquals(1, gerenciador.getDemandas().size());
        assertEquals(5, gerenciador.getDemandas().get(0).getQuantidadeProdutos());
    }

    @Test
    void linhasDiferentesDoMesmoInstrumentoSaoDemandasDistintas() {
        registrar(LinhaProduto.CLASSICA, 2, Instrumento.VIOLAO);
        registrar(LinhaProduto.STUDIO, 2, Instrumento.VIOLAO);

        assertEquals(2, gerenciador.getDemandas().size());
    }

    @Test
    void atualizarDemandaInexistenteCriaUmaNova() {
        gerenciador.atualizarDemanda(LinhaProduto.PALCO, 4, Instrumento.GUITARRA);

        assertEquals(1, gerenciador.getDemandas().size());
        assertEquals(4, gerenciador.getDemandas().get(0).getQuantidadeProdutos());
    }

    // compra de matéria-prima

    @Test
    void comprarMateriaPrimaAumentaOEstoqueEDescontaDoBudget() {
        gerenciador.comprarMateriaPrima(1, 10.0);

        assertEquals(110.0, mogno().getQuantidade(), DELTA);
        assertEquals(4990.0, gerenciador.getBudget(), DELTA);
    }

    @Test
    void comprarMateriaPrimaInexistenteLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.comprarMateriaPrima(0, 10.0));
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.comprarMateriaPrima(99, 10.0));
    }

    @Test
    void comprarQuantidadeInvalidaLancaExcecaoSemAlterarNada() {
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.comprarMateriaPrima(1, 0.0));
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.comprarMateriaPrima(1, -5.0));

        assertEquals(100.0, mogno().getQuantidade(), DELTA);
        assertEquals(5000.0, gerenciador.getBudget(), DELTA);
    }

    @Test
    void comprarSemBudgetSuficienteLancaExcecaoSemAlterarNada() {
        assertThrows(RecursoInsuficienteException.class, () -> gerenciador.comprarMateriaPrima(1, 6000.0));

        assertEquals(100.0, mogno().getQuantidade(), DELTA);
        assertEquals(5000.0, gerenciador.getBudget(), DELTA);
    }

    // produção

    @Test
    void fabricarDemandaCompletaConsomeMateriaPrimaEBudgetEConcluiADemanda() {
        Demanda demanda = registrar(LinhaProduto.CLASSICA, 3, Instrumento.VIOLAO);

        gerenciador.fabricarDemanda(1);

        // violão = 4,0 kg x linha clássica (fator 1,0) x 3 unidades = 12 kg
        assertEquals(88.0, mogno().getQuantidade(), DELTA);
        // 3 instrumentos x (25 + 10) de operação das máquinas
        assertEquals(5000.0 - 105.0, gerenciador.getBudget(), DELTA);
        assertEquals(StatusDemanda.CONCLUIDA, demanda.getStatus());
        assertEquals(0, demanda.getQuantidadeProdutos());
    }

    @Test
    void fabricarComEstoqueInsuficienteLancaExcecaoESemAlterarNada() {
        ArrayList<MateriaPrima> pouco = Fixtures.materiasPrimas();
        pouco.set(0, new MateriaPrima("MP01", "Madeira de mogno", 2.0, "kg", 1.0));
        gerenciador = Fixtures.gerenciador(pouco, Fixtures.linhaDeterministica());
        Demanda demanda = registrar(LinhaProduto.CLASSICA, 3, Instrumento.VIOLAO);

        assertThrows(RecursoInsuficienteException.class, () -> gerenciador.fabricarDemanda(1));

        assertEquals(2.0, mogno().getQuantidade(), DELTA);
        assertEquals(5000.0, gerenciador.getBudget(), DELTA);
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
        assertEquals(3, demanda.getQuantidadeProdutos());
    }

    @Test
    void estoqueQueAcabaNoMeioInterrompeOLoteEMantemODemaisNaDemanda() {
        ArrayList<MateriaPrima> pouco = Fixtures.materiasPrimas();
        pouco.set(0, new MateriaPrima("MP01", "Madeira de mogno", 10.0, "kg", 1.0));
        gerenciador = Fixtures.gerenciador(pouco, Fixtures.linhaDeterministica());
        Demanda demanda = registrar(LinhaProduto.CLASSICA, 5, Instrumento.VIOLAO);

        gerenciador.fabricarDemanda(1);

        // 10 kg / 4 kg por violão = 2 instrumentos fabricados; sobram 2 kg
        assertEquals(2.0, mogno().getQuantidade(), DELTA);
        assertEquals(3, demanda.getQuantidadeProdutos());
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
        assertEquals(5000.0 - 70.0, gerenciador.getBudget(), DELTA);
    }

    @Test
    void fabricarSemBudgetParaAOperacaoLancaExcecaoSemAlterarNada() {
        Demanda demanda = registrar(LinhaProduto.CLASSICA, 3, Instrumento.VIOLAO);
        gerenciador.restaurarEstado(10.0, new ArrayList<>(gerenciador.getDemandas()));

        assertThrows(RecursoInsuficienteException.class, () -> gerenciador.fabricarDemanda(1));

        assertEquals(100.0, mogno().getQuantidade(), DELTA);
        assertEquals(10.0, gerenciador.getBudget(), DELTA);
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
    }

    @Test
    void fabricarInstrumentoSemMateriaPrimaCadastradaLancaExcecao() {
        ArrayList<MateriaPrima> semMogno = Fixtures.materiasPrimas();
        semMogno.remove(0);
        gerenciador = Fixtures.gerenciador(semMogno, Fixtures.linhaDeterministica());
        registrar(LinhaProduto.CLASSICA, 1, Instrumento.VIOLAO);

        assertThrows(RecursoInsuficienteException.class, () -> gerenciador.fabricarDemanda(1));
    }

    @Test
    void fabricarComMaquinaDesligadaLancaExcecaoSemAlterarNada() {
        gerenciador.getLinha().getMaquina(1).desligar();
        Demanda demanda = registrar(LinhaProduto.CLASSICA, 1, Instrumento.VIOLAO);

        assertThrows(MaquinaIndisponivelException.class, () -> gerenciador.fabricarDemanda(1));

        assertEquals(100.0, mogno().getQuantidade(), DELTA);
        assertEquals(StatusDemanda.PENDENTE, demanda.getStatus());
    }

    @Test
    void fabricarDemandaInexistenteLancaExcecao() {
        registrar(LinhaProduto.CLASSICA, 1, Instrumento.VIOLAO);

        assertThrows(DemandaInvalidaException.class, () -> gerenciador.fabricarDemanda(0));
        assertThrows(DemandaInvalidaException.class, () -> gerenciador.fabricarDemanda(2));
    }

    @Test
    void fabricarDemandaQueNaoEstaPendenteLancaExcecao() {
        Demanda demanda = registrar(LinhaProduto.CLASSICA, 1, Instrumento.VIOLAO);
        demanda.cancelar();

        assertThrows(DemandaInvalidaException.class, () -> gerenciador.fabricarDemanda(1));
    }

    @Test
    void fabricarDemandaSemQuantidadeLancaExcecao() {
        registrar(LinhaProduto.CLASSICA, 0, Instrumento.VIOLAO);

        assertThrows(DemandaInvalidaException.class, () -> gerenciador.fabricarDemanda(1));
    }

    // estratégia

    @Test
    void executarProximaProducaoSegueAEstrategiaAtual() {
        Demanda violao = registrar(LinhaProduto.CLASSICA, 1, Instrumento.VIOLAO);
        Demanda ukulele = registrar(LinhaProduto.CLASSICA, 2, Instrumento.UKULELE);
        gerenciador.setEstrategia(new EstrategiaMaiorEncomenda());

        gerenciador.executarProximaProducao();

        assertEquals("Maior Encomenda", gerenciador.getNomeEstrategiaAtual());
        assertEquals(StatusDemanda.CONCLUIDA, ukulele.getStatus());
        assertEquals(StatusDemanda.PENDENTE, violao.getStatus());
    }

    @Test
    void executarProximaProducaoSemDemandaElegivelLancaExcecao() {
        assertThrows(DemandaInvalidaException.class, () -> gerenciador.executarProximaProducao());
    }

    // restaurarEstado (usado pela persistência)

    @Test
    void restaurarEstadoSubstituiBudgetEDemandas() {
        registrar(LinhaProduto.CLASSICA, 1, Instrumento.VIOLAO);
        ArrayList<Demanda> novas = new ArrayList<>();
        novas.add(new Demanda(LinhaProduto.PALCO, 7, Instrumento.GUITARRA));

        gerenciador.restaurarEstado(123.45, novas);

        assertEquals(123.45, gerenciador.getBudget(), DELTA);
        assertEquals(1, gerenciador.getDemandas().size());
        assertEquals(Instrumento.GUITARRA, gerenciador.getDemandas().get(0).getInstrumento());
    }

    @Test
    void restaurarEstadoInvalidoLancaExcecaoEMantemOEstado() {
        registrar(LinhaProduto.CLASSICA, 1, Instrumento.VIOLAO);

        assertThrows(EntradaInvalidaException.class, () -> gerenciador.restaurarEstado(-1.0, new ArrayList<>()));
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.restaurarEstado(Double.NaN, new ArrayList<>()));
        assertThrows(EntradaInvalidaException.class, () -> gerenciador.restaurarEstado(10.0, null));

        assertEquals(5000.0, gerenciador.getBudget(), DELTA);
        assertEquals(1, gerenciador.getDemandas().size());
    }
}
