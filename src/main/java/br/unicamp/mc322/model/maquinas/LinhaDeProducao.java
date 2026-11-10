package br.unicamp.mc322.model.maquinas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.unicamp.mc322.model.auditoria.Auditavel;
import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.MaquinaIndisponivelException;

/**
 * Agrupador das máquinas da fábrica: uma linha de produção com as máquinas na ordem em que
 * processam o produto.
 * <p>
 * A relação entre a linha e as {@link Maquina}s é uma <b>agregação</b> (1..*), e não uma
 * composição: as máquinas são criadas fora da linha e uma máquina pode ser retirada de uma
 * linha e colocada em outra, pois seu ciclo de vida não depende da linha. Já as partes internas
 * de cada máquina (motor, sensor, ferramentas) são composição e nunca aparecem aqui.
 * </p>
 * <p>
 * A coleção interna é protegida: {@link #getMaquinas()} devolve uma visão imutável, e qualquer
 * alteração da estrutura passa por {@link #adicionarMaquina(Maquina)} e
 * {@link #removerMaquina(Maquina)}. A linha também é {@link Auditavel} e propaga a auditoria em
 * cascata (linha, máquinas e partes).
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class LinhaDeProducao implements Auditavel {

    private final String nome;
    private final List<Maquina> maquinas;

    /**
     * Cria uma linha de produção vazia.
     *
     * @param nome nome da linha, usado nos relatórios
     * @throws EntradaInvalidaException se o nome for nulo ou vazio
     */
    public LinhaDeProducao(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new EntradaInvalidaException("Uma linha de produção precisa de um nome.");
        }

        this.nome = nome;
        this.maquinas = new ArrayList<>();
    }

    /**
     * Nome da linha.
     *
     * @return nome da linha
     */
    public String getNome() {
        return nome;
    }

    /**
     * Adiciona uma máquina ao final da linha (última etapa).
     *
     * @param maquina máquina a adicionar
     * @throws EntradaInvalidaException se a máquina for nula ou já estiver nesta linha
     */
    public void adicionarMaquina(Maquina maquina) {
        if (maquina == null) {
            throw new EntradaInvalidaException("Não é possível adicionar uma máquina nula à linha " + nome + ".");
        }

        if (maquinas.contains(maquina)) {
            throw new EntradaInvalidaException(maquina.getNome() + " já faz parte da linha " + nome + ".");
        }

        maquinas.add(maquina);
    }

    /**
     * Retira uma máquina da linha. A máquina continua existindo e pode ser adicionada a outra
     * linha (agregação).
     *
     * @param maquina máquina a retirar
     * @throws EntradaInvalidaException se a máquina não estiver nesta linha
     */
    public void removerMaquina(Maquina maquina) {
        if (!maquinas.remove(maquina)) {
            throw new EntradaInvalidaException("A máquina informada não faz parte da linha " + nome + ".");
        }
    }

    /**
     * Máquinas da linha, na ordem de processamento. A lista devolvida é imutável: tentar
     * modificá-la lança {@link UnsupportedOperationException}.
     *
     * @return visão imutável das máquinas
     */
    public List<Maquina> getMaquinas() {
        return Collections.unmodifiableList(maquinas);
    }

    /**
     * Quantidade de máquinas da linha.
     *
     * @return número de máquinas
     */
    public int getQuantidadeMaquinas() {
        return maquinas.size();
    }

    /**
     * Máquina de posição informada.
     *
     * @param numero número da máquina, começando em 1
     * @return a máquina escolhida
     * @throws EntradaInvalidaException se o número não existir
     */
    public Maquina getMaquina(int numero) {
        int indice = numero - 1;

        if (indice < 0 || indice >= maquinas.size()) {
            throw new EntradaInvalidaException(
                    "Máquina inválida: o número " + numero
                            + " não existe (escolha de 1 a " + maquinas.size() + ")."
            );
        }

        return maquinas.get(indice);
    }

    /**
     * Indica se a máquina é a última etapa da linha.
     *
     * @param maquina máquina a testar
     * @return {@code true} se for a última da linha
     */
    public boolean ehUltimaEtapa(Maquina maquina) {
        return !maquinas.isEmpty() && maquinas.get(maquinas.size() - 1) == maquina;
    }

    /**
     * Aplica os multiplicadores do cenário a todas as máquinas da linha.
     *
     * @param multiplicadorFalha fator aplicado à probabilidade de falha
     * @param multiplicadorDesgaste fator aplicado ao desgaste de cada uso
     * @throws EntradaInvalidaException se algum multiplicador for inválido
     */
    public void configurarCenario(double multiplicadorFalha, double multiplicadorDesgaste) {
        for (Maquina maquina : maquinas) {
            maquina.configurarCenario(multiplicadorFalha, multiplicadorDesgaste);
        }
    }

    /**
     * Soma do custo de operação de todas as máquinas, ou seja, quanto custa passar um
     * instrumento por toda a linha.
     *
     * @return custo de produção de um instrumento em reais
     */
    public double calcularCustoProducao() {
        double custoTotal = 0;

        for (Maquina maquina : maquinas) {
            custoTotal += maquina.getCustoOperacao();
        }

        return custoTotal;
    }

    /**
     * Capacidade da linha: a menor capacidade entre as máquinas.
     *
     * @return quantidade máxima de instrumentos por lote
     */
    public int calcularCapacidade() {
        int capacidade = Integer.MAX_VALUE;

        for (Maquina maquina : maquinas) {
            capacidade = Math.min(capacidade, (int) maquina.getCapacidadeMaxima());
        }

        return capacidade;
    }

    /**
     * Confere se todas as máquinas estão operacionais (ligadas e não quebradas).
     *
     * @throws MaquinaIndisponivelException se alguma máquina estiver desligada ou quebrada
     */
    public void verificarOperacional() {
        for (Maquina maquina : maquinas) {
            if (!maquina.estaOperacional()) {
                throw new MaquinaIndisponivelException(
                        maquina.getNome() + " está desligada ou quebrada. Repare-a no menu Auditoria antes de produzir."
                );
            }
        }
    }

    /**
     * Repara a máquina de número informado, incluindo todas as suas partes.
     *
     * @param numero número da máquina, começando em 1
     * @return a máquina reparada
     * @throws EntradaInvalidaException se o número não existir
     */
    public Maquina repararMaquina(int numero) {
        Maquina maquina = getMaquina(numero);
        maquina.reparar();
        return maquina;
    }

    /**
     * Quantas máquinas da linha precisam de manutenção (por saúde ou por alguma parte).
     *
     * @return número de máquinas que precisam de manutenção
     */
    public int contarMaquinasQuePrecisamManutencao() {
        int total = 0;

        for (Maquina maquina : maquinas) {
            if (maquina.precisaManutencao()) {
                total++;
            }
        }

        return total;
    }

    /**
     * Gera o relatório hierárquico e indentado da linha: o cabeçalho da linha, cada máquina e,
     * dentro de cada máquina, cada uma de suas partes.
     *
     * @return relatório em várias linhas
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        StringBuilder relatorio = new StringBuilder();
        relatorio.append(String.format("Linha %s (%d máquinas, %d precisam de manutenção)",
                nome, maquinas.size(), contarMaquinasQuePrecisamManutencao()));

        // Cascata: a linha pede o diagnóstico a cada máquina, que pede às suas partes
        for (Maquina maquina : maquinas) {
            relatorio.append("\n").append(maquina.gerarRelatorioDiagnostico("  "));
        }

        return relatorio.toString();
    }

    /**
     * A linha precisa de intervenção se qualquer uma de suas máquinas precisar.
     *
     * @return {@code true} se alguma máquina precisa de manutenção
     */
    @Override
    public boolean precisaManutencao() {
        return contarMaquinasQuePrecisamManutencao() > 0;
    }
}
