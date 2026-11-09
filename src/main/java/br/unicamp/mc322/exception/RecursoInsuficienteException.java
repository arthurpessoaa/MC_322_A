package br.unicamp.mc322.exception;

/**
 * Lançada quando falta algum recurso para concluir uma operação: matéria-prima
 * em estoque ou orçamento (budget) disponível.
 * <p>
 * Quem lança esta exceção garante que nada foi consumido ou debitado antes do erro.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class RecursoInsuficienteException extends FabricaException {

    /**
     * Cria a exceção com a descrição do recurso que faltou.
     *
     * @param mensagem qual recurso faltou e em que quantidade
     */
    public RecursoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
