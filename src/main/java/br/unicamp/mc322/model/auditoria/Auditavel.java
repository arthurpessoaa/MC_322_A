package br.unicamp.mc322.model.auditoria;

/**
 * Contrato para qualquer componente da fábrica que possa ser auditado.
 * <p>
 * Máquinas e instrumentos prontos implementam esta interface, de modo que a auditoria geral
 * gera o relatório da fábrica inteira sem precisar saber o tipo concreto de cada componente.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public interface Auditavel {

    /**
     * Gera uma linha de texto com o diagnóstico atual do componente.
     *
     * @return relatório de diagnóstico legível para o usuário
     */
    String gerarRelatorioDiagnostico();

    /**
     * Indica se o componente exige intervenção (reparo de máquina ou atenção a produto em risco).
     *
     * @return {@code true} se o componente precisa de manutenção
     */
    boolean precisaManutencao();
}
