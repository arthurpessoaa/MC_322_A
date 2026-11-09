package br.unicamp.mc322.exception;

/**
 * Exceção base de todos os erros de domínio da Fábrica Harmonia.
 * <p>
 * É uma exceção não verificada ({@link RuntimeException}): quem representa a
 * interação com o usuário (o menu em {@code Main}) captura as subclasses,
 * mostra a mensagem e devolve o usuário ao menu, sem encerrar o programa.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class FabricaException extends RuntimeException {

    /**
     * Cria uma exceção de domínio com uma mensagem legível para o usuário.
     *
     * @param mensagem explicação do que aconteceu e, quando possível, como corrigir
     */
    public FabricaException(String mensagem) {
        super(mensagem);
    }
}
