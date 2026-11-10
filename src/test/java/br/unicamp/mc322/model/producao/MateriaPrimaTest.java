package br.unicamp.mc322.model.producao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.RecursoInsuficienteException;

/** Regras de estoque da {@link MateriaPrima}: consumo, reposição e validações. */
class MateriaPrimaTest {

    private static final double DELTA = 0.0001;

    private MateriaPrima aco;

    @BeforeEach
    void criarEstoque() {
        aco = new MateriaPrima("MP-01", "Aço", 10.0, "kg", 5.0);
    }

    @Test
    void novaMateriaPrimaGuardaOsDadosInformados() {
        assertEquals("MP-01", aco.getId());
        assertEquals("Aço", aco.getNome());
        assertEquals(10.0, aco.getQuantidade(), DELTA);
        assertEquals("kg", aco.getUnidade());
        assertEquals(5.0, aco.getCustoPorUnidade(), DELTA);
    }

    @Test
    void consumirMenosQueOEstoqueDescontaOSaldo() {
        aco.consumir(3.0);

        assertEquals(7.0, aco.getQuantidade(), DELTA);
    }

    @Test
    void consumirExatamenteOEstoqueZeraOSaldo() {
        aco.consumir(10.0);

        assertEquals(0.0, aco.getQuantidade(), DELTA);
    }

    @Test
    void consumirMaisQueOEstoqueLancaExcecaoEMantemOSaldo() {
        assertThrows(RecursoInsuficienteException.class, () -> aco.consumir(10.5));

        assertEquals(10.0, aco.getQuantidade(), DELTA);
    }

    @Test
    void consumirQuantidadeNaoPositivaLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> aco.consumir(0.0));
        assertThrows(EntradaInvalidaException.class, () -> aco.consumir(-2.0));
        assertThrows(EntradaInvalidaException.class, () -> aco.consumir(Double.NaN));
        assertEquals(10.0, aco.getQuantidade(), DELTA);
    }

    @Test
    void verificarDisponibilidadeNaoAlteraOEstoque() {
        assertTrue(aco.verificarDisponibilidade(10.0));
        assertFalse(aco.verificarDisponibilidade(10.1));
        assertEquals(10.0, aco.getQuantidade(), DELTA);
    }

    @Test
    void adicionarEstoqueSomaAoSaldo() {
        aco.adicionarEstoque(2.5);

        assertEquals(12.5, aco.getQuantidade(), DELTA);
    }

    @Test
    void adicionarQuantidadeInvalidaLancaExcecaoEMantemOSaldo() {
        assertThrows(EntradaInvalidaException.class, () -> aco.adicionarEstoque(0.0));
        assertThrows(EntradaInvalidaException.class, () -> aco.adicionarEstoque(-1.0));
        assertThrows(EntradaInvalidaException.class, () -> aco.adicionarEstoque(Double.POSITIVE_INFINITY));
        assertEquals(10.0, aco.getQuantidade(), DELTA);
    }
}
