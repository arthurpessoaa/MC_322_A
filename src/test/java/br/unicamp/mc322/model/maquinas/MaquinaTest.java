package br.unicamp.mc322.model.maquinas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.MaquinaIndisponivelException;
import br.unicamp.mc322.model.produtos.LinhaClassica;
import br.unicamp.mc322.model.produtos.Produto;
import br.unicamp.mc322.model.produtos.StatusProduto;
import br.unicamp.mc322.support.Fixtures;

/** Criação, estado, composição (motor, sensor, ferramentas) e processamento das máquinas. */
class MaquinaTest {

    private static final double DELTA = 0.0001;

    private MaquinaMontagem novaMontagem() {
        return new MaquinaMontagem("Montagem", true, 30.0, 0.0, 25.0);
    }

    private Produto novoProduto() {
        return new LinhaClassica("P001", "Violão", StatusProduto.AGUARDANDO, 4.0, 3.0);
    }

    // criação e validação

    @Test
    void maquinaNovaComecaComSaudeTotalESemFalhas() {
        MaquinaMontagem maquina = novaMontagem();

        assertEquals("Montagem", maquina.getNome());
        assertTrue(maquina.estaLigada());
        assertTrue(maquina.estaOperacional());
        assertEquals(100.0, maquina.getSaude(), DELTA);
        assertEquals(0, maquina.getHistoricoFalhas());
        assertEquals(30.0, maquina.getCapacidadeMaxima(), DELTA);
        assertEquals(25.0, maquina.getCustoOperacao(), DELTA);
    }

    @Test
    void cadaTipoDeMaquinaCriaSuasFerramentas() {
        assertEquals(3, novaMontagem().getQuantidadeFerramentas());
        assertEquals(2, new MaquinaEmbalagem("E", true, 30.0, 0.0, 10.0).getQuantidadeFerramentas());
        assertEquals(3, new MaquinaInspecaoSonora("I", true, 30.0, 0.0, 15.0).getQuantidadeFerramentas());
    }

    @Test
    void ferramentasSaoNumeradasESemDesgaste() {
        List<Maquina.EstadoFerramenta> ferramentas = novaMontagem().consultarFerramentas();

        assertEquals(3, ferramentas.size());
        assertEquals("Garra 1", ferramentas.get(0).nome());
        assertEquals("Garra 3", ferramentas.get(2).nome());
        assertEquals(0.0, ferramentas.get(0).desgaste(), DELTA);
    }

    @Test
    void parametrosInvalidosNaCriacaoLancamExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> new MaquinaMontagem(" ", true, 30.0, 0.1, 25.0));
        assertThrows(EntradaInvalidaException.class, () -> new MaquinaMontagem(null, true, 30.0, 0.1, 25.0));
        assertThrows(EntradaInvalidaException.class, () -> new MaquinaMontagem("M", true, 0.0, 0.1, 25.0));
        assertThrows(EntradaInvalidaException.class, () -> new MaquinaMontagem("M", true, 30.0, 1.5, 25.0));
        assertThrows(EntradaInvalidaException.class, () -> new MaquinaMontagem("M", true, 30.0, -0.1, 25.0));
        assertThrows(EntradaInvalidaException.class, () -> new MaquinaMontagem("M", true, 30.0, 0.1, -1.0));
    }

    // ligar, desligar e processar

    @Test
    void ligarEDesligarAlteramOEstado() {
        MaquinaMontagem maquina = novaMontagem();

        maquina.desligar();
        assertFalse(maquina.estaLigada());
        assertFalse(maquina.estaOperacional());

        maquina.ligar();
        assertTrue(maquina.estaOperacional());
    }

    @Test
    void processarComMaquinaDesligadaLancaExcecao() {
        MaquinaMontagem maquina = novaMontagem();
        maquina.desligar();

        assertThrows(MaquinaIndisponivelException.class, () -> maquina.processar(novoProduto()));
    }

    @Test
    void processarComMaquinaQuebradaLancaExcecao() {
        MaquinaMontagem maquina = novaMontagem();
        maquina.restaurarEstado(50.0, 0, 25.0, 100.0, 100.0, Fixtures.ferramentasComDesgaste(maquina, 0, 0, 0));
        assertTrue(maquina.estaQuebrada());

        assertThrows(MaquinaIndisponivelException.class, () -> maquina.processar(novoProduto()));
    }

    @Test
    void montagemSemFalhaAprovaOProduto() {
        assertTrue(novaMontagem().processar(novoProduto()));
    }

    @Test
    void embalagemMarcaOProdutoComoEmbalado() {
        MaquinaEmbalagem embalagem = new MaquinaEmbalagem("Embalagem", true, 30.0, 0.0, 10.0);
        Produto produto = novoProduto();

        embalagem.processar(produto);

        assertEquals(StatusProduto.EMBALADO, produto.getStatus());
    }

    @Test
    void processarDesgastaAsPartesDaMaquina() {
        MaquinaMontagem maquina = novaMontagem();
        maquina.configurarCenario(1.0, 1.0);

        maquina.processar(novoProduto());

        assertTrue(maquina.lerDesgasteMotor() > 0.0);
        assertTrue(maquina.lerCalibracaoSensor() < 100.0);
        assertTrue(maquina.consultarFerramentas().get(0).desgaste() > 0.0);
        assertTrue(maquina.getSaude() < 100.0);
    }

    // manutenção

    @Test
    void repararRestauraSaudeEPartes() {
        MaquinaMontagem maquina = novaMontagem();
        maquina.restaurarEstado(20.0, 3, 80.0, 100.0, 10.0, Fixtures.ferramentasComDesgaste(maquina, 70, 80, 90));

        maquina.reparar();

        assertEquals(100.0, maquina.getSaude(), DELTA);
        assertEquals(0.0, maquina.lerDesgasteMotor(), DELTA);
        assertEquals(100.0, maquina.lerCalibracaoSensor(), DELTA);
        assertTrue(maquina.estaOperacional());
    }

    // restaurarEstado (usado pela persistência)

    @Test
    void restaurarEstadoAplicaTodosOsValores() {
        MaquinaMontagem maquina = novaMontagem();

        maquina.restaurarEstado(80.0, 2, 60.0, 10.0, 70.0, Fixtures.ferramentasComDesgaste(maquina, 5, 10, 15));

        assertEquals(80.0, maquina.getSaude(), DELTA);
        assertEquals(2, maquina.getHistoricoFalhas());
        assertEquals(60.0, maquina.lerTemperaturaMotor(), DELTA);
        assertEquals(10.0, maquina.lerDesgasteMotor(), DELTA);
        assertEquals(70.0, maquina.lerCalibracaoSensor(), DELTA);
        assertEquals(15.0, maquina.consultarFerramentas().get(2).desgaste(), DELTA);
    }

    @Test
    void restaurarEstadoInvalidoLancaExcecaoESemAlterarNada() {
        MaquinaMontagem maquina = novaMontagem();
        List<Maquina.EstadoFerramenta> validas = Fixtures.ferramentasComDesgaste(maquina, 0, 0, 0);

        assertThrows(EntradaInvalidaException.class,
                () -> maquina.restaurarEstado(101.0, 0, 25.0, 0.0, 100.0, validas));
        assertThrows(EntradaInvalidaException.class,
                () -> maquina.restaurarEstado(50.0, -1, 25.0, 0.0, 100.0, validas));
        assertThrows(EntradaInvalidaException.class,
                () -> maquina.restaurarEstado(50.0, 0, 10.0, 0.0, 100.0, validas));
        assertThrows(EntradaInvalidaException.class,
                () -> maquina.restaurarEstado(50.0, 0, 25.0, 0.0, 100.0, validas.subList(0, 2)));
        assertThrows(EntradaInvalidaException.class,
                () -> maquina.restaurarEstado(50.0, 0, 25.0, 0.0, 100.0,
                        List.of(new Maquina.EstadoFerramenta("Outra", 0), validas.get(1), validas.get(2))));

        assertEquals(100.0, maquina.getSaude(), DELTA);
        assertEquals(0, maquina.getHistoricoFalhas());
    }
}
