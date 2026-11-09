package br.unicamp.mc322.exception;

/**
 * Lançada quando uma operação sobre demandas não pode ser feita: a demanda
 * escolhida não existe, está em um estado incompatível com a operação
 * (por exemplo, fabricar uma demanda que não está pendente) ou tem
 * uma quantidade inválida.
 *
 * @author Arthur Lopes Pessoa
 */
public class DemandaInvalidaException extends FabricaException {

    /**
     * Cria a exceção com o motivo pelo qual a demanda não pôde ser usada.
     *
     * @param mensagem motivo da recusa
     */
    public DemandaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
