package br.unicamp.mc322.exception;

/**
 * Lançada quando uma máquina da linha de produção não pode operar
 * porque está desligada ou quebrada e precisa de reparo.
 *
 * @author Arthur Lopes Pessoa
 */
public class MaquinaIndisponivelException extends FabricaException {

    /**
     * Cria a exceção indicando qual máquina está indisponível e por quê.
     *
     * @param mensagem nome da máquina e motivo da indisponibilidade
     */
    public MaquinaIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
