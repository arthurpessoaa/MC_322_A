package br.unicamp.mc322.model.maquinas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import br.unicamp.mc322.model.auditoria.Auditavel;
import br.unicamp.mc322.model.componentes.FerramentaDeTrabalho;
import br.unicamp.mc322.model.componentes.MotorDeAcionamento;
import br.unicamp.mc322.model.componentes.SensorDeVibracao;
import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.MaquinaIndisponivelException;
import br.unicamp.mc322.model.produtos.Produto;

/**
 * Máquina genérica da linha de produção.
 * <p>
 * Cada uso desgasta a máquina (reduz sua saúde) e a probabilidade de falha cresce conforme
 * a saúde diminui. Uma máquina com saúde zero está quebrada e precisa ser reparada. O método
 * {@link #processar(Produto)} define o fluxo comum (validação e desgaste) e delega a etapa
 * específica às subclasses por meio de {@link #executarProcessamento(Produto)}.
 * </p>
 * <p>
 * Máquinas são {@link Auditavel}: precisam de manutenção quando a saúde fica abaixo de 30 ou
 * quando alguma de suas partes precisa de manutenção. Os multiplicadores de falha e desgaste
 * são definidos pelo cenário de operação.
 * </p>
 * <p>
 * A máquina é o "Todo" de uma <b>composição estrita</b>: ela cria, no próprio construtor, seu
 * {@link MotorDeAcionamento} (1:1), seu {@link SensorDeVibracao} (1:1) e uma lista de
 * {@link FerramentaDeTrabalho} (1:N). As partes nunca saem da máquina: não existe nenhum getter
 * que as exponha, e quem precisa de algo delas usa os métodos repassadores da máquina
 * (Lei de Deméter). O estado das partes influencia a máquina: motor superaquecido ou sensor
 * descalibrado aumentam a chance de falha, e ferramentas gastas pioram o produto processado.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public abstract class Maquina implements Auditavel {

    private static final double SAUDE_MAXIMA = 100.0;
    private static final double LIMITE_MANUTENCAO = 30.0;
    private static final double DESGASTE_MAXIMO_BASE = 3.0;

    private static final Random random = new Random();

    private String nome;
    private boolean ligada;
    private double capacidadeMaxima;
    private double probabilidadeFalha;
    private double custoOperacao;

    private double saude = SAUDE_MAXIMA;
    private int historicoFalhas = 0;
    private double multiplicadorFalha = 1.0;
    private double multiplicadorDesgaste = 1.0;

    // Composição: as partes nascem e morrem com a máquina
    private final MotorDeAcionamento motor;
    private final SensorDeVibracao sensor;
    private final List<FerramentaDeTrabalho> ferramentas;

    /**
     * Cria uma máquina com saúde máxima (100) e sem histórico de falhas. As partes internas
     * (motor, sensor e ferramentas) são criadas aqui, e só aqui.
     *
     * @param nome nome da máquina, usado em mensagens
     * @param ligada {@code true} se a máquina começa ligada
     * @param capacidadeMaxima quantidade máxima de instrumentos por lote
     * @param probabilidadeFalha probabilidade-base de falha por processamento, entre 0 e 1
     * @param custoOperacao custo, em reais, de operar a máquina em um instrumento
     * @param nomeFerramenta nome base das ferramentas da máquina (ex.: "Garra" gera "Garra 1", ...)
     * @param quantidadeFerramentas quantas ferramentas a máquina possui (pelo menos 1)
     * @throws EntradaInvalidaException se o nome ou o nome da ferramenta forem vazios, a capacidade
     *         não for positiva, a probabilidade estiver fora de 0 a 1, o custo for negativo ou a
     *         quantidade de ferramentas for menor que 1
     */
    public Maquina(
            String nome,
            boolean ligada,
            double capacidadeMaxima,
            double probabilidadeFalha,
            double custoOperacao,
            String nomeFerramenta,
            int quantidadeFerramentas
    ) {
        if (nome == null || nome.isBlank()) {
            throw new EntradaInvalidaException("Uma máquina precisa de um nome.");
        }

        if (!(capacidadeMaxima > 0)) {
            throw new EntradaInvalidaException("A capacidade máxima da máquina " + nome + " deve ser positiva.");
        }

        if (!(probabilidadeFalha >= 0 && probabilidadeFalha <= 1)) {
            throw new EntradaInvalidaException("A probabilidade de falha da máquina " + nome + " deve estar entre 0 e 1.");
        }

        if (!(custoOperacao >= 0)) {
            throw new EntradaInvalidaException("O custo de operação da máquina " + nome + " não pode ser negativo.");
        }

        if (nomeFerramenta == null || nomeFerramenta.isBlank()) {
            throw new EntradaInvalidaException("A máquina " + nome + " precisa de um nome base para suas ferramentas.");
        }

        if (quantidadeFerramentas < 1) {
            throw new EntradaInvalidaException("A máquina " + nome + " precisa de pelo menos 1 ferramenta.");
        }

        this.nome = nome;
        this.ligada = ligada;
        this.capacidadeMaxima = capacidadeMaxima;
        this.probabilidadeFalha = probabilidadeFalha;
        this.custoOperacao = custoOperacao;

        // Fase de construção: o Todo instancia suas Partes
        this.motor = new MotorDeAcionamento();
        this.sensor = new SensorDeVibracao();
        this.ferramentas = new ArrayList<>();

        for (int i = 1; i <= quantidadeFerramentas; i++) {
            this.ferramentas.add(new FerramentaDeTrabalho(nomeFerramenta + " " + i));
        }
    }

    /**
     * Liga a máquina.
     */
    public void ligar() { ligada = true; }
    /**
     * Desliga a máquina. Uma máquina desligada não processa produtos.
     */
    public void desligar() { ligada = false; }

    /**
     * Processa um produto nesta máquina, desgastando-a.
     * <p>
     * Antes de qualquer alteração, verifica se a máquina pode operar; se não puder, lança
     * a exceção sem desgastar a máquina nem modificar o produto.
     * </p>
     *
     * @param produto produto a ser processado
     * @return {@code true} se o produto segue na linha (aprovado nesta etapa); {@code false} se foi
     *         reprovado. Somente a inspeção sonora pode reprovar.
     * @throws MaquinaIndisponivelException se a máquina estiver desligada ou quebrada
     */
    public final boolean processar(Produto produto) {
        if (!estaLigada()) {
            throw new MaquinaIndisponivelException(nome + " está desligada.");
        }

        if (estaQuebrada()) {
            throw new MaquinaIndisponivelException(nome + " está quebrada e precisa de reparo.");
        }

        desgastar();
        aplicarEfeitoDasFerramentas(produto);
        return executarProcessamento(produto);
    }

    /**
     * Executa a etapa específica de cada tipo de máquina.
     *
     * @param produto produto a ser processado
     * @return {@code true} se o produto foi aprovado nesta etapa
     */
    protected abstract boolean executarProcessamento(Produto produto);
    /**
     * Tipo da máquina, usado nos relatórios de auditoria.
     *
     * @return nome do tipo da máquina
     */
    public abstract String getTipo();

    // desgaste e falhas
    private void desgastar() {
        boolean motorJaQuebrado = motor.estaQuebrado();

        // O motor superaquecido acelera o desgaste da máquina inteira
        double desgaste = random.nextDouble() * DESGASTE_MAXIMO_BASE * multiplicadorDesgaste * motor.getFatorDesgaste();
        saude = Math.max(0.0, saude - desgaste);

        // As partes também envelhecem a cada uso
        motor.operar(multiplicadorDesgaste);
        sensor.registrarUso(multiplicadorDesgaste);
        for (FerramentaDeTrabalho ferramenta : ferramentas) {
            ferramenta.desgastar(multiplicadorDesgaste);
        }

        if (!motorJaQuebrado && motor.estaQuebrado()) {
            System.out.println("O motor de " + nome + " quebrou.");
        }

        if (estaQuebrada()) {
            System.out.println(nome + " quebrou de tanto uso.");
        }
    }

    // Ferramentas gastas deixam defeitos no produto (uma Parte quebrada afeta o resultado do Todo)
    private void aplicarEfeitoDasFerramentas(Produto produto) {
        double penalidade = 0.0;

        for (FerramentaDeTrabalho ferramenta : ferramentas) {
            penalidade += ferramenta.getPenalidadeQualidade();
        }

        if (penalidade > 0.0) {
            produto.aumentarProbabilidadeFalha(penalidade);
            System.out.printf("Ferramentas gastas em %s deixaram defeitos em %s (+%.2f na probabilidade de falha).%n",
                    nome, produto.getNome(), penalidade);
        }
    }

    /**
     * Calcula a probabilidade de falha atual. Ela aumenta conforme a saúde cai:
     * {@code base * multiplicadorDoCenario * (1 + (100 - saude) / 50) * fatorMotor * fatorSensor},
     * limitada a 1. Motor superaquecido e sensor descalibrado multiplicam a chance por 1,5 cada.
     *
     * @return probabilidade de falha entre 0 e 1
     */
    public double getProbabilidadeFalhaAtual() {
        double fatorDesgaste = 1.0 + (SAUDE_MAXIMA - saude) / 50.0;
        double fatorPartes = motor.getFatorFalha() * sensor.getFatorFalha();
        return Math.min(1.0, probabilidadeFalha * multiplicadorFalha * fatorDesgaste * fatorPartes);
    }

    /**
     * Sorteia se a máquina falha neste processamento. Se falhar, registra a falha no histórico.
     *
     * @return {@code true} se ocorreu uma falha
     */
    protected boolean verificarFalha() {
        boolean falhou = random.nextDouble() < getProbabilidadeFalhaAtual();

        if (falhou) {
            historicoFalhas++;
        }

        return falhou;
    }

    /**
     * Sorteia um evento aleatório com a probabilidade informada.
     *
     * @param probabilidade chance do evento ocorrer, entre 0 e 1
     * @return {@code true} se o evento ocorreu
     */
    protected boolean sortearEvento(double probabilidade) {
        return random.nextDouble() < probabilidade;
    }

    /**
     * Repara a máquina, restaurando a saúde máxima (100) e reparando todas as suas partes
     * (motor, sensor e ferramentas). O histórico de falhas é mantido.
     */
    public void reparar() {
        saude = SAUDE_MAXIMA;
        motor.reparar();
        sensor.calibrar();
        trocarFerramentas();
    }

    // ---- Métodos repassadores (Lei de Deméter): o cliente só conhece a máquina ----

    /**
     * Temperatura atual do motor interno da máquina.
     *
     * @return temperatura em graus Celsius
     */
    public double lerTemperaturaMotor() {
        return motor.getTemperatura();
    }

    /**
     * Calibração atual do sensor interno da máquina.
     *
     * @return calibração entre 0 e 100
     */
    public double lerCalibracaoSensor() {
        return sensor.getCalibracao();
    }

    /**
     * Quantas ferramentas a máquina possui.
     *
     * @return número de ferramentas
     */
    public int getQuantidadeFerramentas() {
        return ferramentas.size();
    }

    /**
     * Quantas ferramentas estão gastas ou quebradas.
     *
     * @return número de ferramentas gastas
     */
    public int contarFerramentasGastas() {
        int total = 0;

        for (FerramentaDeTrabalho ferramenta : ferramentas) {
            if (ferramenta.estaGasta()) {
                total++;
            }
        }

        return total;
    }

    /**
     * Deixa o motor esfriar até a temperatura ambiente.
     */
    public void resfriarMotor() {
        motor.resfriar();
    }

    /**
     * Recalibra o sensor.
     */
    public void calibrarSensor() {
        sensor.calibrar();
    }

    /**
     * Troca todas as ferramentas por novas (zera o desgaste de cada uma).
     */
    public void trocarFerramentas() {
        for (FerramentaDeTrabalho ferramenta : ferramentas) {
            ferramenta.reparar();
        }
    }

    /**
     * Aplica os multiplicadores do cenário de operação.
     *
     * @param multiplicadorFalha fator aplicado à probabilidade de falha
     * @param multiplicadorDesgaste fator aplicado ao desgaste de cada uso
     * @throws EntradaInvalidaException se algum multiplicador for negativo, infinito ou não numérico
     */
    public void configurarCenario(double multiplicadorFalha, double multiplicadorDesgaste) {
        if (!(multiplicadorFalha >= 0) || !(multiplicadorDesgaste >= 0)
                || Double.isInfinite(multiplicadorFalha) || Double.isInfinite(multiplicadorDesgaste)) {
            throw new EntradaInvalidaException("Os multiplicadores do cenário devem ser números não negativos.");
        }

        this.multiplicadorFalha = multiplicadorFalha;
        this.multiplicadorDesgaste = multiplicadorDesgaste;
    }

    /**
     * Gera o diagnóstico da máquina e de todas as suas partes (auditoria em cascata), em várias
     * linhas: a primeira traz tipo, saúde, chance de falha, falhas acumuladas e estado (OK,
     * precisa de manutenção ou quebrada); as seguintes, indentadas, trazem o diagnóstico de
     * cada parte.
     *
     * @return diagnóstico hierárquico da máquina
     */
    @Override
    public String gerarRelatorioDiagnostico() {
        return montarRelatorio("", "    ");
    }

    /**
     * Mesmo diagnóstico de {@link #gerarRelatorioDiagnostico()}, mas indentado para aparecer
     * dentro do relatório de um agrupador (por exemplo, a linha de produção).
     *
     * @param recuo espaços de indentação do nível da máquina
     * @return diagnóstico hierárquico da máquina, indentado
     */
    public String gerarRelatorioDiagnostico(String recuo) {
        return montarRelatorio(recuo + "> ", recuo + "    ");
    }

    private String montarRelatorio(String prefixoMaquina, String recuoPartes) {
        String estado;

        if (estaQuebrada()) {
            estado = "QUEBRADA";
        } else if (precisaManutencao()) {
            estado = "PRECISA DE MANUTENÇÃO";
        } else {
            estado = "OK";
        }

        StringBuilder relatorio = new StringBuilder();
        relatorio.append(String.format("%s%s [%s] | Saúde: %.0f/100 | Chance de falha: %.0f%% | Falhas: %d | %s",
                prefixoMaquina, nome, getTipo(), saude, getProbabilidadeFalhaAtual() * 100, historicoFalhas, estado));

        // Cascata: a máquina pede o diagnóstico a cada uma de suas partes
        relatorio.append("\n").append(recuoPartes).append("> ").append(motor.gerarRelatorioDiagnostico());
        relatorio.append("\n").append(recuoPartes).append("> ").append(sensor.gerarRelatorioDiagnostico());

        for (FerramentaDeTrabalho ferramenta : ferramentas) {
            relatorio.append("\n").append(recuoPartes).append("> ").append(ferramenta.gerarRelatorioDiagnostico());
        }

        return relatorio.toString();
    }

    /**
     * Indica se a máquina precisa de manutenção: saúde abaixo do limite (30) ou alguma parte
     * (motor, sensor ou ferramenta) precisando de manutenção.
     *
     * @return {@code true} se a máquina precisa de manutenção
     */
    @Override
    public boolean precisaManutencao() {
        if (saude < LIMITE_MANUTENCAO || motor.precisaManutencao() || sensor.precisaManutencao()) {
            return true;
        }

        for (FerramentaDeTrabalho ferramenta : ferramentas) {
            if (ferramenta.precisaManutencao()) {
                return true;
            }
        }

        return false;
    }


    /**
     * Nome da máquina, usado nas mensagens ao usuário.
     *
     * @return nome da máquina
     */
    public String getNome() { 
        return nome; 
    }
    /**
     * Indica se a máquina está ligada.
     *
     * @return {@code true} se estiver ligada
     */
    public boolean estaLigada() { 
        return ligada; 
    }
    /**
     * Indica se a máquina quebrou: a saúde chegou a zero ou o motor quebrou.
     *
     * @return {@code true} se estiver quebrada
     */
    public boolean estaQuebrada() { 
        return saude <= 0.0 || motor.estaQuebrado(); 
    }
    /**
     * Indica se a máquina pode operar: ligada e não quebrada.
     *
     * @return {@code true} se estiver operacional
     */
    public boolean estaOperacional() { 
        return ligada && !estaQuebrada(); 
    }
    /**
     * Saúde atual da máquina: 100 é a máquina nova e 0 é a máquina quebrada.
     *
     * @return saúde entre 0 e 100
     */
    public double getSaude() { 
        return saude; 
    }
    /**
     * Quantas falhas a máquina já teve desde que foi criada. O reparo não zera esse número.
     *
     * @return número de falhas registradas
     */
    public int getHistoricoFalhas() { 
        return historicoFalhas; 
    }
    /**
     * Quanto custa passar um instrumento por esta máquina. O valor sai do orçamento a cada
     * instrumento processado.
     *
     * @return custo de operação em reais
     */
    public double getCustoOperacao() { 
        return custoOperacao; 
    }
    /**
     * Quantos instrumentos a máquina aguenta por lote. A menor capacidade entre as máquinas da
     * linha limita o tamanho do lote.
     *
     * @return quantidade máxima de instrumentos por lote
     */
    public double getCapacidadeMaxima() { 
        return capacidadeMaxima; 
    }

    /**
    * Consulta o desgaste do motor interno da máquina.
    * Preserva o encapsulamento, pois não retorna o objeto motor.
    *
     * @return desgaste atual do motor
    */
    public double lerDesgasteMotor() {
        return motor.getDesgaste();
    }

    /**
    * Representa os dados de uma ferramenta utilizados na persistência.
    * Não expõe o objeto FerramentaDeTrabalho original.
    *
    * @param nome nome da ferramenta
    * @param desgaste desgaste atual da ferramenta
    */
    public record EstadoFerramenta(String nome, double desgaste) {}

    /**
    * Retorna os estados das ferramentas da máquina.
   * Cria uma representação independente para cada ferramenta,
     * sem expor diretamente a lista ou os objetos internos.
     *
     * @return lista imutável com os estados das ferramentas
     */
    public List<EstadoFerramenta> consultarFerramentas() {

        List<EstadoFerramenta> estados = new ArrayList<>();

        // Percorre as ferramentas que pertencem à máquina.
        for (FerramentaDeTrabalho ferramenta : ferramentas) {

            // Cria uma representação dos dados da ferramenta.
            estados.add(new EstadoFerramenta(ferramenta.getNome(),ferramenta.getDesgaste()));
            
    }

    // Retorna uma lista que não pode ser modificada externamente.
    return List.copyOf(estados);
    }

    /**
    * Retorna a probabilidade-base de falha da máquina.
    * Esse valor será utilizado para reconstruir a configuração
    * original da máquina durante o carregamento do XML.
    *
    * @return probabilidade-base de falha
    */
    public double getProbabilidadeFalhaBase() {
        return probabilidadeFalha;
    }

    /**
 * Restaura o estado da máquina e de seus componentes internos.
 *
 * @param saude saúde recuperada do XML
 * @param historicoFalhas quantidade de falhas registradas
 * @param temperaturaMotor temperatura do motor
 * @param desgasteMotor desgaste do motor
 * @param calibracaoSensor calibração do sensor
 * @param estadosFerramentas estados das ferramentas
 * @throws EntradaInvalidaException se algum dado for inválido
 */
    public void restaurarEstado(
        double saude,
        int historicoFalhas,
        double temperaturaMotor,
        double desgasteMotor,
        double calibracaoSensor,
        List<EstadoFerramenta> estadosFerramentas) {

        if (!Double.isFinite(saude) || saude < 0.0 || saude > 100.0 || historicoFalhas < 0) {

        throw new EntradaInvalidaException( "Estado inválido da máquina.");
        }

        if (estadosFerramentas == null || estadosFerramentas.size() != ferramentas.size()) {

        throw new EntradaInvalidaException("Quantidade de ferramentas incompatível.");
        }

        if (!Double.isFinite(temperaturaMotor) ||temperaturaMotor < 25.0 || temperaturaMotor > 120.0 || !Double.isFinite(desgasteMotor) || desgasteMotor < 0.0 || desgasteMotor > 100.0 ||!Double.isFinite(calibracaoSensor) || calibracaoSensor < 0.0 || calibracaoSensor > 100.0) {

        throw new EntradaInvalidaException("Estado inválido dos componentes.");
        }

        for (int i = 0; i < ferramentas.size(); i++) {
            EstadoFerramenta estado = estadosFerramentas.get(i);

            if (estado == null || !ferramentas.get(i).getNome().equals(estado.nome()) ||!Double.isFinite(estado.desgaste()) || estado.desgaste() < 0.0 || estado.desgaste() > 100.0) {
                throw new EntradaInvalidaException("Estado inválido das ferramentas.");
        }
        }

        motor.restaurarEstado(temperaturaMotor, desgasteMotor);
        sensor.restaurarEstado(calibracaoSensor);

        for (int i = 0; i < ferramentas.size(); i++) {
            ferramentas.get(i).restaurarEstado(estadosFerramentas.get(i).desgaste());
        }

        this.saude = saude;
        this.historicoFalhas = historicoFalhas;
    }
}
