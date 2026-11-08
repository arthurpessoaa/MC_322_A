package componentes;

import auditoria.Auditavel;
import excecoes.EntradaInvalidaException;

/**
 * Ferramenta que atua diretamente sobre o instrumento (parte 1:N da composição: cada máquina
 * possui várias).
 * <p>
 * Desgasta a cada uso. Uma ferramenta gasta deixa o instrumento com defeitos, aumentando a
 * probabilidade de falha acumulada do produto; uma ferramenta quebrada piora ainda mais.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class FerramentaDeTrabalho implements Auditavel {

    private static final double DESGASTE_MAXIMO = 100.0;
    private static final double LIMITE_GASTA = 60.0;
    private static final double DESGASTE_POR_USO = 1.5;
    private static final double PENALIDADE_GASTA = 0.02;
    private static final double PENALIDADE_QUEBRADA = 0.05;

    private final String nome;
    private double desgaste = 0.0;

    /**
     * Cria uma ferramenta nova.
     *
     * @param nome nome da ferramenta, usado nos relatórios (ex.: "Garra 1")
     * @throws EntradaInvalidaException se o nome for nulo ou vazio
     */
    public FerramentaDeTrabalho(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new EntradaInvalidaException("Uma ferramenta precisa de um nome.");
        }

        this.nome = nome;
    }

    /**
     * Desgasta a ferramenta por um uso.
     *
     * @param multiplicadorDesgaste fator de desgaste do cenário de operação (não negativo)
     * @throws EntradaInvalidaException se o multiplicador for negativo, infinito ou não numérico
     */
    public void desgastar(double multiplicadorDesgaste) {
        if (!(multiplicadorDesgaste >= 0) || Double.isInfinite(multiplicadorDesgaste)) {
            throw new EntradaInvalidaException("O multiplicador de desgaste da ferramenta deve ser um número não negativo.");
        }

        desgaste = Math.min(DESGASTE_MAXIMO, desgaste + DESGASTE_POR_USO * multiplicadorDesgaste);
    }

    /**
     * Troca/afia a ferramenta, zerando o desgaste.
     */
    public void reparar() {
        desgaste = 0.0;
    }

    /**
     * Indica se a ferramenta está gasta (desgaste a partir de 60%).
     *
     * @return {@code true} se está gasta ou quebrada
     */
    public boolean estaGasta() {
        return desgaste >= LIMITE_GASTA;
    }

    /**
     * Indica se a ferramenta quebrou (desgaste máximo).
     *
     * @return {@code true} se está quebrada
     */
    public boolean estaQuebrada() {
        return desgaste >= DESGASTE_MAXIMO;
    }

    /**
     * Quanto esta ferramenta aumenta a probabilidade de falha do produto a cada uso.
     *
     * @return penalidade: 0 se está boa, 0,02 se gasta, 0,05 se quebrada
     */
    public double getPenalidadeQualidade() {
        if (estaQuebrada()) {
            return PENALIDADE_QUEBRADA;
        }
        return estaGasta() ? PENALIDADE_GASTA : 0.0;
    }

    /**
     * Gera o diagnóstico atual do componente. Componentes simples devolvem uma única linha;
     * componentes compostos devolvem um texto hierárquico e indentado, com uma linha para o
     * próprio componente seguida do diagnóstico de cada uma de suas partes.
     *
     * @return relatório de diagnóstico legível para o usuário
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        String estado = estaQuebrada() ? "Quebrada" : (estaGasta() ? "Desgastada" : "OK");
        return String.format("%s: %s (desgaste %.0f%%)", nome, estado, desgaste);
    }

    /**
     * A ferramenta precisa de manutenção quando está gasta ou quebrada.
     *
     * @return {@code true} se exige intervenção
     */
    @Override
    public boolean precisaManutencao() {
        return estaGasta();
    }
    /**
    * Retorna o nome da ferramenta.
    *
    * @return nome da ferramenta
    */
    public String getNome() {
        return nome;
    }       

    /**
    * Retorna o desgaste atual da ferramenta.
    *
     * @return desgaste atual da ferramenta
     */
    public double getDesgaste() {
        return desgaste;
    }
}
