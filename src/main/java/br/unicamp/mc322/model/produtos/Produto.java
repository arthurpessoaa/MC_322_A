package br.unicamp.mc322.model.produtos;
import br.unicamp.mc322.model.auditoria.Auditavel;

/**
 * Instrumento fabricado pela Harmonia.
 * <p>
 * Cada produto tem um identificador, um nome, um lote, um {@link StatusProduto}, uma qualidade
 * fixa definida pela linha e uma probabilidade de falha acumulada, que cresce quando as máquinas
 * falham durante o processamento. As subclasses representam as linhas de produção e definem
 * como o produto é processado e quanto tempo leva para ser fabricado.
 * </p>
 * <p>
 * Produtos são {@link Auditavel}: ficam em risco quando a probabilidade de falha acumulada
 * chega a 30%.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public abstract class Produto implements Auditavel {
    private String id;
    private String nome;
    private String lote;
    private StatusProduto status;
    private double quantidadeMateriaPrimaPorUnidade;
    private double qualidade;
    private double probabilidadeFalhaAcumulada;
    private double tempoProducao;

    private static final double LIMITE_RISCO = 0.3;

    private static int totalProdutosFabricados = 0;

    /**
     * Cria um produto sem falhas acumuladas e incrementa o contador de produtos fabricados.
     *
     * @param id identificador do produto
     * @param nome nome do instrumento
     * @param status status inicial
     * @param quantidadeMateriaPrimaPorUnidade matéria-prima gasta neste produto
     * @param qualidade qualidade da linha, entre 0 e 1
     * @param tempoProducao tempo-base de produção
     */
    public Produto(
            String id,
            String nome,
            StatusProduto status,
            double quantidadeMateriaPrimaPorUnidade,
            double qualidade,
            double tempoProducao
    ) {
        this.id = id;
        this.nome = nome;
        this.status = status;
        this.quantidadeMateriaPrimaPorUnidade = quantidadeMateriaPrimaPorUnidade;
        this.qualidade = qualidade;
        this.tempoProducao = tempoProducao;
        this.probabilidadeFalhaAcumulada = 0.0;

        totalProdutosFabricados++;
    }

    /**
     * Executa a montagem e o acabamento específicos da linha e atualiza o status do produto.
     */
    public abstract void processar();

    /**
     * Calcula o tempo de produção, que depende da linha (linhas mais sofisticadas levam mais tempo).
     *
     * @return tempo de produção
     */
    public abstract double calcularTempoProducao();

    /**
     * Nome da linha a que o produto pertence (Studio, Palco ou Clássica).
     *
     * @return nome da linha
     */
    public abstract String getTipo();

    /**
     * Código do produto, no formato {@code P001}.
     *
     * @return código do produto
     */
    public String getId() {
        return id;
    }
    /**
     * Nome do instrumento (Violão, Guitarra etc.).
     *
     * @return nome do instrumento
     */
    public String getNome() {
        return nome;
    }
    /**
     * Muda a etapa em que o produto está na linha de produção.
     *
     * @param status nova etapa do produto
     */
    public void setStatus(StatusProduto status) {
        this.status = status;
    }
    /**
     * Etapa em que o produto está na linha de produção.
     *
     * @return status atual
     */
    public StatusProduto getStatus() {
        return status;
    }
    /**
     * Qualidade do produto, que depende só da linha: 0,5 na Clássica, 0,7 no Palco e 0,9 no Studio.
     *
     * @return qualidade entre 0 e 1
     */
    public double getQualidade() {
        return qualidade;
    }
    /**
     * Quanto de matéria-prima foi gasto para fazer este produto.
     *
     * @return quantidade de matéria-prima
     */
    public double getQuantidadeMateriaPrimaPorUnidade() {
        return quantidadeMateriaPrimaPorUnidade;
    }
    /**
     * Tempo-base do instrumento, que cada linha multiplica em {@link #calcularTempoProducao()}.
     *
     * @return tempo-base de produção
     */
    protected double getTempoProducao() {
        return tempoProducao;
    }
    /**
     * Soma ao risco acumulado do produto. É chamado quando uma máquina falha durante o processamento.
     *
     * @param aumento valor a somar (por exemplo, 0,3 numa falha da montagem)
     */
    public void aumentarProbabilidadeFalha(double aumento) {
        probabilidadeFalhaAcumulada += aumento;
    }
    /**
     * Risco acumulado de falha do produto. Pesa na chance de rejeição na inspeção sonora e, a partir
     * de 30%, marca o produto como em risco na auditoria.
     *
     * @return risco acumulado
     */
    public double getProbabilidadeFalhaAcumulada() {
        return probabilidadeFalhaAcumulada;
    }
    /**
     * Total de produtos já criados (os rejeitados também contam). Serve para gerar os códigos
     * {@code P001}, {@code P002} e assim por diante.
     *
     * @return total de produtos criados
     */
    public static int getTotalProdutosFabricados() {
        return totalProdutosFabricados;
    }
    /**
     * Registra o lote em que o produto foi fabricado.
     *
     * @param lote identificador do lote (por exemplo, {@code L001-VIOLAO})
     */
    public void setLote(String lote) {
        this.lote = lote;
    }
    /**
     * Lote em que o produto foi fabricado.
     *
     * @return identificador do lote, ou {@code null} se o produto ainda não foi aprovado
     */
    public String getLote() {
        return lote;
    }

    /**
     * Gera o diagnóstico do produto: tipo, nome, qualidade, risco acumulado e situação.
     *
     * @return linha de diagnóstico
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        return String.format("%s %s | Qualidade: %.0f%% | Risco acumulado: %.0f%% | %s",
                getTipo(), nome, qualidade * 100, probabilidadeFalhaAcumulada * 100,
                precisaManutencao() ? "EM RISCO" : "OK");
    }

    /**
     * Indica se o produto está em risco, isto é, se a probabilidade de falha acumulada é de pelo menos 30%.
     *
     * @return {@code true} se o produto está em risco
     */
    @Override
    public boolean precisaManutencao() {
        return probabilidadeFalhaAcumulada >= LIMITE_RISCO;
    }
}
