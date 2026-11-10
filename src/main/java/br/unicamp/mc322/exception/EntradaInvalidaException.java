package br.unicamp.mc322.exception;

/**
 * Lançada quando um valor informado pelo usuário é inválido para o domínio:
 * quantidade não positiva, número que não corresponde a nenhuma matéria-prima
 * ou máquina existente, etc.
 *
 * @author Arthur Lopes Pessoa
 */
public class EntradaInvalidaException extends FabricaException {

    /**
     * Cria a exceção com a explicação do valor inválido.
     *
     * @param mensagem o que foi informado de errado e o que é esperado
     */
    public EntradaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
