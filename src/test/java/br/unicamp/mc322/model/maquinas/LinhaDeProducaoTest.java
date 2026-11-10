package br.unicamp.mc322.model.maquinas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.MaquinaIndisponivelException;

/** Composição da linha: ordem das etapas, capacidade, custo e disponibilidade. */
class LinhaDeProducaoTest {

    private static final double DELTA = 0.0001;

    private LinhaDeProducao linha;
    private MaquinaMontagem montagem;
    private MaquinaEmbalagem embalagem;

    @BeforeEach
    void montarLinha() {
        linha = new LinhaDeProducao("Linha de teste");
        montagem = new MaquinaMontagem("Montagem", true, 20.0, 0.0, 25.0);
        embalagem = new MaquinaEmbalagem("Embalagem", true, 30.0, 0.0, 10.0);
        linha.adicionarMaquina(montagem);
        linha.adicionarMaquina(embalagem);
    }

    @Test
    void linhaSemNomeLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> new LinhaDeProducao(" "));
        assertThrows(EntradaInvalidaException.class, () -> new LinhaDeProducao(null));
    }

    @Test
    void adicionarMaquinasMantemAOrdemDasEtapas() {
        assertEquals(2, linha.getQuantidadeMaquinas());
        assertSame(montagem, linha.getMaquinas().get(0));
        assertSame(embalagem, linha.getMaquinas().get(1));
    }

    @Test
    void adicionarMaquinaNulaOuRepetidaLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> linha.adicionarMaquina(null));
        assertThrows(EntradaInvalidaException.class, () -> linha.adicionarMaquina(montagem));
        assertEquals(2, linha.getQuantidadeMaquinas());
    }

    @Test
    void getMaquinaUsaNumeracaoAPartirDeUm() {
        assertSame(montagem, linha.getMaquina(1));
        assertSame(embalagem, linha.getMaquina(2));
    }

    @Test
    void getMaquinaForaDaFaixaLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> linha.getMaquina(0));
        assertThrows(EntradaInvalidaException.class, () -> linha.getMaquina(3));
    }

    @Test
    void apenasAUltimaMaquinaEAUltimaEtapa() {
        assertFalse(linha.ehUltimaEtapa(montagem));
        assertTrue(linha.ehUltimaEtapa(embalagem));
    }

    @Test
    void capacidadeDaLinhaEADaMaquinaMaisLenta() {
        assertEquals(20, linha.calcularCapacidade());
    }

    @Test
    void custoDeProducaoSomaOCustoDeTodasAsMaquinas() {
        assertEquals(35.0, linha.calcularCustoProducao(), DELTA);
    }

    @Test
    void verificarOperacionalPassaQuandoTodasEstaoLigadas() {
        linha.verificarOperacional();
    }

    @Test
    void verificarOperacionalFalhaComMaquinaDesligada() {
        embalagem.desligar();

        assertThrows(MaquinaIndisponivelException.class, () -> linha.verificarOperacional());
    }

    @Test
    void repararMaquinaDevolveAMaquinaComSaudeTotal() {
        montagem.restaurarEstado(10.0, 1, 25.0, 0.0, 100.0,
                br.unicamp.mc322.support.Fixtures.ferramentasComDesgaste(montagem, 0, 0, 0));

        Maquina reparada = linha.repararMaquina(1);

        assertSame(montagem, reparada);
        assertEquals(100.0, montagem.getSaude(), DELTA);
    }
}
