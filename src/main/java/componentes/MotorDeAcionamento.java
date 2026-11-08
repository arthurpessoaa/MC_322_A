package componentes;

import auditoria.Auditavel;
import excecoes.EntradaInvalidaException;

/**
 * Motor que aciona a máquina (parte 1:1 da composição).
 * <p>
 * Cada ciclo de operação aumenta a temperatura e o desgaste do motor. Acima do limite de
 * temperatura o motor fica superaquecido e a máquina falha mais e se desgasta mais rápido.
 * Com desgaste máximo o motor quebra e a máquina para.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class MotorDeAcionamento implements Auditavel {

    private static final double TEMPERATURA_AMBIENTE = 25.0;
    private static final double LIMITE_SUPERAQUECIMENTO = 80.0;
    private static final double DESGASTE_MAXIMO = 100.0;
    private static final double LIMITE_MANUTENCAO = 70.0;
    private static final double TEMPERATURA_MAXIMA = 120.0;
    private static final double AQUECIMENTO_POR_CICLO = 4.0;
    private static final double TAXA_RESFRIAMENTO = 0.08;
    private static final double DESGASTE_POR_CICLO = 0.8;

    private double temperatura = TEMPERATURA_AMBIENTE;
    private double desgaste = 0.0;

    /**
     * Cria um motor novo, à temperatura ambiente e sem desgaste. Só a {@link maquinas.Maquina}
     * dona do motor o instancia.
     */
    public MotorDeAcionamento() {
    }

    /**
     * Executa um ciclo de operação: esquenta o motor (mais ainda se já estiver superaquecido)
     * e o desgasta. A cada ciclo o motor dissipa 8% do calor acima da temperatura ambiente; em
     * condições normais a temperatura se estabiliza abaixo do limite, mas em cenários de
     * desgaste alto o motor acaba superaquecendo.
     *
     * @param multiplicadorDesgaste fator de desgaste do cenário de operação (não negativo)
     * @throws EntradaInvalidaException se o multiplicador for negativo, infinito ou não numérico
     */
    public void operar(double multiplicadorDesgaste) {
        if (!(multiplicadorDesgaste >= 0) || Double.isInfinite(multiplicadorDesgaste)) {
            throw new EntradaInvalidaException("O multiplicador de desgaste do motor deve ser um número não negativo.");
        }

        double aquecimento = AQUECIMENTO_POR_CICLO * multiplicadorDesgaste * (estaSuperaquecido() ? 1.25 : 1.0);
        double resfriamento = TAXA_RESFRIAMENTO * (temperatura - TEMPERATURA_AMBIENTE);
        temperatura = Math.min(TEMPERATURA_MAXIMA, temperatura + aquecimento - resfriamento);
        desgaste = Math.min(DESGASTE_MAXIMO, desgaste + DESGASTE_POR_CICLO * multiplicadorDesgaste);
    }

    /**
     * Deixa o motor esfriar até a temperatura ambiente, sem alterar o desgaste.
     */
    public void resfriar() {
        temperatura = TEMPERATURA_AMBIENTE;
    }

    /**
     * Repara o motor: zera o desgaste e volta à temperatura ambiente.
     */
    public void reparar() {
        desgaste = 0.0;
        temperatura = TEMPERATURA_AMBIENTE;
    }

    /**
     * Indica se a temperatura passou do limite de segurança (80 °C).
     *
     * @return {@code true} se o motor está superaquecido
     */
    public boolean estaSuperaquecido() {
        return temperatura >= LIMITE_SUPERAQUECIMENTO;
    }

    /**
     * Indica se o motor quebrou (desgaste máximo).
     *
     * @return {@code true} se o motor está quebrado
     */
    public boolean estaQuebrado() {
        return desgaste >= DESGASTE_MAXIMO;
    }

    /**
     * Fator aplicado à probabilidade de falha da máquina: 1,0 normal, 1,5 superaquecido.
     *
     * @return multiplicador de falha causado pelo estado do motor
     */
    public double getFatorFalha() {
        return estaSuperaquecido() ? 1.5 : 1.0;
    }

    /**
     * Fator aplicado ao desgaste da máquina: 1,0 normal, 1,5 superaquecido.
     *
     * @return multiplicador de desgaste causado pelo estado do motor
     */
    public double getFatorDesgaste() {
        return estaSuperaquecido() ? 1.5 : 1.0;
    }

    /**
     * Temperatura atual do motor.
     *
     * @return temperatura em graus Celsius
     */
    public double getTemperatura() {
        return temperatura;
    }

    /**
     * Gera o diagnóstico do motor em uma linha.
     *
     * @return diagnóstico do motor
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        String estado;

        if (estaQuebrado()) {
            estado = "Quebrado";
        } else if (estaSuperaquecido()) {
            estado = "Superaquecido";
        } else if (precisaManutencao()) {
            estado = "Desgastado";
        } else {
            estado = "OK";
        }

        return String.format("Motor: %s (%.0f°C, desgaste %.0f%%)", estado, temperatura, desgaste);
    }

    /**
     * O motor precisa de manutenção se estiver quebrado, superaquecido ou muito desgastado.
     *
     * @return {@code true} se exige intervenção
     */
    @Override
    public boolean precisaManutencao() {
        return estaQuebrado() || estaSuperaquecido() || desgaste >= LIMITE_MANUTENCAO;
    }

    /**
    * Retorna o desgaste atual do motor.
    * Permite consultar esse valor sem expor o objeto internamente.
    *
    * @return desgaste atual do motor
    */
    public double getDesgaste() {
        return desgaste;
    }
}
