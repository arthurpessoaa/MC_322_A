package br.unicamp.mc322.model.componentes;

import br.unicamp.mc322.model.auditoria.Auditavel;
import br.unicamp.mc322.exception.EntradaInvalidaException;

/**
 * Sensor de vibração que monitora a máquina (parte 1:1 da composição).
 * <p>
 * Perde calibração a cada uso. Descalibrado, o sensor deixa a máquina operar "às cegas" e
 * aumenta a chance de falha.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class SensorDeVibracao implements Auditavel {

    private static final double CALIBRACAO_MAXIMA = 100.0;
    private static final double LIMITE_DESCALIBRADO = 40.0;
    private static final double PERDA_POR_USO = 0.6;

    private double calibracao = CALIBRACAO_MAXIMA;

    /**
     * Cria um sensor novo, com calibração máxima. Só a {@link br.unicamp.mc322.model.maquinas.Maquina} dona do sensor
     * o instancia.
     */
    public SensorDeVibracao() {
    }

    /**
     * Registra um uso do sensor, que perde um pouco de calibração.
     *
     * @param multiplicadorDesgaste fator de desgaste do cenário de operação (não negativo)
     * @throws EntradaInvalidaException se o multiplicador for negativo, infinito ou não numérico
     */
    public void registrarUso(double multiplicadorDesgaste) {
        if (!(multiplicadorDesgaste >= 0) || Double.isInfinite(multiplicadorDesgaste)) {
            throw new EntradaInvalidaException("O multiplicador de desgaste do sensor deve ser um número não negativo.");
        }

        calibracao = Math.max(0.0, calibracao - PERDA_POR_USO * multiplicadorDesgaste);
    }

    /**
     * Recalibra o sensor, restaurando a calibração máxima.
     */
    public void calibrar() {
        calibracao = CALIBRACAO_MAXIMA;
    }

    /**
     * Indica se a calibração caiu abaixo do limite aceitável (40%).
     *
     * @return {@code true} se o sensor está descalibrado
     */
    public boolean estaDescalibrado() {
        return calibracao < LIMITE_DESCALIBRADO;
    }

    /**
     * Fator aplicado à probabilidade de falha da máquina: 1,0 calibrado, 1,5 descalibrado.
     *
     * @return multiplicador de falha causado pelo sensor
     */
    public double getFatorFalha() {
        return estaDescalibrado() ? 1.5 : 1.0;
    }

    /**
     * Calibração atual do sensor.
     *
     * @return calibração entre 0 e 100
     */
    public double getCalibracao() {
        return calibracao;
    }

    /**
     * Gera o diagnóstico do sensor em uma linha.
     *
     * @return diagnóstico do sensor
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("Sensor: %s (calibração %.0f%%)",
                estaDescalibrado() ? "Descalibrado" : "OK", calibracao);
    }

    /**
     * O sensor precisa de manutenção quando está descalibrado.
     *
     * @return {@code true} se exige intervenção
     */
    @Override
    public boolean precisaManutencao() {
        return estaDescalibrado();
    }
    /**
    * Restaura a calibração do sensor.
    *
    * @param calibracao calibração recuperada do XML
     * @throws EntradaInvalidaException se o valor for inválido
     */
    public void restaurarEstado(double calibracao) {
        if (!Double.isFinite(calibracao) || calibracao < 0.0 || calibracao > 100.0) {

            throw new EntradaInvalidaException( "Calibração inválida do sensor.");
    }

    this.calibracao = calibracao;
    }
}
