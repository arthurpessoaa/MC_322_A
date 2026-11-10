package br.unicamp.mc322.exception;

/**
 * Indica uma falha ao salvar ou carregar o estado da planta. arquivo inexistente, vazio,
 * corrompido, com dados incompatíveis com as regras do domínio ou com erro de leitura/escrita.
 *
 * @author Arthur Lopes Pessoa
 */
public class PersistenciaException extends FabricaException {

    /**
     * Cria a exceção com uma mensagem compreensível para o usuário.
     *
     * @param mensagem descrição do problema
     */
    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    /**
     * Cria a exceção preservando a causa original.
     *
     * @param mensagem descrição do problema
     * @param causa exceção que originou a falha
     */
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem);
        initCause(causa);
    }
}