package producao;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import auditoria.Auditavel;
import estrategias.EstrategiaProducao;
import excecoes.DemandaInvalidaException;
import excecoes.EntradaInvalidaException;
import excecoes.MaquinaIndisponivelException;
import excecoes.RecursoInsuficienteException;
import maquinas.LinhaDeProducao;
import maquinas.Maquina;
import produtos.Instrumento;
import produtos.LinhaClassica;
import produtos.LinhaPalco;
import produtos.LinhaProduto;
import produtos.LinhaStudio;
import produtos.Produto;
import produtos.StatusProduto;

/**
 * Núcleo da planta industrial: gerencia demandas, estoque de matéria-prima, a
 * {@link LinhaDeProducao} (agrupador das máquinas), orçamento (budget), produtos acabados e a
 * estratégia de produção ativa. O gerenciador só conhece a linha e as máquinas; as partes
 * internas das máquinas são acessadas apenas por delegação.
 * <p>
 * As operações que alteram o estado seguem a regra "validar antes de alterar": quando uma
 * operação falha com uma {@link excecoes.FabricaException}, nenhum estoque, orçamento ou demanda foi
 * modificado. Quem apresenta os erros ao usuário é a camada de interação ({@code Main}).
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class GerenciadorProducao {

    private ArrayList<Demanda> demandas;
    private ArrayList<Produto> produtosFabricados;
    private LinhaDeProducao linha;
    private ArrayList<MateriaPrima> materiasPrimas;

    private Cenario cenario;
    private double budget;
    private static int contadorLotes = 1;
    
    private EstrategiaProducao estrategiaAtual;

    /**
     * Cria o gerenciador, define o orçamento inicial do cenário e aplica a ele os multiplicadores
     * de falha e desgaste de cada máquina.
     *
     * @param linha linha de produção, com as máquinas na ordem em que processam o produto
     * @param materiasPrimas estoque de matérias-primas
     * @param cenario cenário de operação
     * @throws EntradaInvalidaException se a linha for nula ou não tiver nenhuma máquina
     */
    public GerenciadorProducao(LinhaDeProducao linha, ArrayList<MateriaPrima> materiasPrimas, Cenario cenario) {
        if (linha == null || linha.getQuantidadeMaquinas() == 0) {
            throw new EntradaInvalidaException("O gerenciador precisa de uma linha de produção com pelo menos uma máquina.");
        }

        this.demandas = new ArrayList<>();
        this.produtosFabricados = new ArrayList<>();
        this.linha = linha;
        this.materiasPrimas = materiasPrimas;
        this.cenario = cenario;
        this.budget = cenario.getBudgetInicial();

        linha.configurarCenario(cenario.getMultiplicadorFalha(), cenario.getMultiplicadorDesgaste());
    }

    /**
     * Registra uma demanda. Se já existe uma demanda da mesma linha e do mesmo instrumento,
     * apenas atualiza a quantidade dela.
     *
     * @param novaDemanda demanda a registrar
     */
    public void registrarDemanda(Demanda novaDemanda) {
        Demanda demandaExistente = buscarDemanda(novaDemanda.getLinha(), novaDemanda.getInstrumento());

        if (demandaExistente == null) {

            demandas.add(novaDemanda);
            System.out.println("Demanda registrada.");

        } else {

            demandaExistente.atualizarQuantidade(novaDemanda.getQuantidadeProdutos());
            System.out.println("A demanda existente foi atualizada.");

        }
    }

    /**
     * Cria ou atualiza a demanda de um instrumento em uma linha. A quantidade é validada
     * pela própria {@link Demanda}.
     *
     * @param linha linha de produto
     * @param novaQuantidade nova quantidade de instrumentos
     * @param instrumento instrumento da demanda
     * @throws DemandaInvalidaException se a quantidade for negativa
     */
    public void atualizarDemanda(LinhaProduto linha, int novaQuantidade, Instrumento instrumento) {
        Demanda demanda = buscarDemanda(linha, instrumento);

        if (demanda == null) {
            registrarDemanda(new Demanda(linha, novaQuantidade, instrumento));
        } else {
            demanda.atualizarQuantidade(novaQuantidade);
            System.out.println("Demanda atualizada.");
        }
    }

    /**
     * Exibe as demandas numeradas, com instrumento, linha, quantidade e status.
     */
    public void exibirDemandas() {

        System.out.println();
        System.out.println("========================================");
        System.out.println("                DEMANDAS");
        System.out.println("========================================");

        if (demandas.isEmpty()) {
            System.out.println("Nenhuma demanda registrada.");
            return;
        }

        for (int i = 0; i < demandas.size(); i++) {
            Demanda demanda = demandas.get(i);

            System.out.println((i + 1) + " - " + demanda.getInstrumento().getNome() + " " + demanda.getLinha().getNome() + " | Quantidade: " + demanda.getQuantidadeProdutos() + " | " + demanda.getStatus().getDescricao());
        }
    }

    /**
     * Fabrica a demanda de número informado, em lotes limitados pela capacidade da linha.
     * <p>
     * Todas as validações (demanda, máquinas, matéria-prima e orçamento) acontecem antes de qualquer
     * alteração de estado. Depois que a produção começa, a falta de recurso ou a quebra de uma máquina
     * no meio do lote interrompe o lote sem ser considerada erro: o que já foi aprovado vale, e a
     * demanda fica com o restante e volta à fila (ou é concluída, se nada restou). Se uma máquina
     * quebrar antes da última etapa, o produto em andamento é rejeitado, pois não passou por toda a
     * linha. Um bloco {@code finally} garante que a demanda nunca fique presa no estado "em produção".
     * </p>
     *
     * @param numeroDemanda número da demanda, como mostrado em {@link #exibirDemandas()} (começa em 1)
     * @throws DemandaInvalidaException se o número não existir, a demanda não estiver pendente
     *         ou não tiver instrumentos
     * @throws MaquinaIndisponivelException se alguma máquina estiver desligada ou quebrada
     * @throws RecursoInsuficienteException se não houver matéria-prima cadastrada, estoque
     *         ou orçamento para fabricar ao menos um instrumento
     */
    public void fabricarDemanda(int numeroDemanda) {
        Demanda demanda = buscarDemandaPorNumero(numeroDemanda);

        if (demanda.getStatus() != StatusDemanda.PENDENTE) {
            throw new DemandaInvalidaException("Essa demanda não está pendente: " + demanda.getStatus().getDescricao() + ".");
        }

        if (demanda.getQuantidadeProdutos() == 0) {
            throw new DemandaInvalidaException("Essa demanda não tem nenhum instrumento para fabricar.");
        }

        linha.verificarOperacional();

        MateriaPrima materiaPrima = buscarMateriaPrimaDoInstrumento(demanda.getInstrumento());
        double materiaPrimaPorUnidade = demanda.calcularMateriaPrimaPorUnidade();
        double custoProducao = calcularCustoProducao();

        try {
            verificarRecursos(materiaPrima, materiaPrimaPorUnidade, custoProducao);
        } catch (RecursoInsuficienteException e) {
            throw new RecursoInsuficienteException(e.getMessage() + " Nenhuma alteração foi realizada.");
        }

        double materiaPrimaNecessaria = demanda.calcularMateriaPrimaNecessaria();
        System.out.printf("Matéria-prima necessária para a demanda: %.2f %s de %s (estoque: %.2f).%n", materiaPrimaNecessaria, materiaPrima.getUnidade(), materiaPrima.getNome(), materiaPrima.getQuantidade());

        if (!materiaPrima.verificarDisponibilidade(materiaPrimaNecessaria)) {
            System.out.println("Atenção: o estoque não cobre a demanda inteira. A produção vai parar quando a matéria-prima acabar.");
        }

        int capacidadeLinha = linha.calcularCapacidade();
        int quantidadeLote = Math.min(demanda.getQuantidadeProdutos(), capacidadeLinha);

        if (quantidadeLote < demanda.getQuantidadeProdutos()) {
            System.out.println("A capacidade da linha é de " + capacidadeLinha + " unidades por vez. Serão fabricadas " + quantidadeLote + " agora.");
        }

        // A partir daqui a produção começa de fato: o lote só é numerado agora
        String loteAtual = String.format("L%03d-%s", contadorLotes++, demanda.getInstrumento().name());
        demanda.iniciarProducao();

        int quantidadeAprovada = 0;
        int quantidadeRestante;

        try {
            for (int i = 0; i < quantidadeLote; i++) {

                // Falta de recurso no meio do lote não é um erro: o lote é interrompido e o que já foi feito vale
                try {
                    verificarRecursos(materiaPrima, materiaPrimaPorUnidade, custoProducao);
                } catch (RecursoInsuficienteException e) {
                    System.out.println("Produção interrompida: " + e.getMessage());
                    break;
                }

                Produto produto = criarProduto(demanda);
                materiaPrima.consumir(materiaPrimaPorUnidade);

                boolean aprovado = true;
                boolean maquinaQuebrouAgora = false;

                for (Maquina maquina : linha.getMaquinas()) {

                    budget -= maquina.getCustoOperacao();

                    if (!maquina.processar(produto)) {
                        aprovado = false;
                    }

                    if (!maquina.estaOperacional()) {
                        System.out.println("A máquina " + maquina.getNome() + " quebrou durante o processamento.");
                        maquinaQuebrouAgora = true;

                        // Se a quebra foi antes da última etapa, o produto não passou por toda a linha
                        if (!linha.ehUltimaEtapa(maquina)) {
                            aprovado = false;
                            System.out.println(produto.getNome() + " não passou por todas as etapas da linha.");
                        }
                        break;
                    }
                }

                if (aprovado) {
                    produto.setStatus(StatusProduto.APROVADO);
                    produto.setLote(loteAtual);
                    produtosFabricados.add(produto);
                    quantidadeAprovada++;
                    System.out.println(produto.getNome() + " foi aprovado e adicionado ao armazém.");
                } else {
                    produto.setStatus(StatusProduto.REJEITADO);
                    System.out.println(produto.getNome() + " foi rejeitado.");
                }

                if (maquinaQuebrouAgora) {
                    break;
                }
            }
        } finally {
            // Mesmo que algo inesperado interrompa o lote, a demanda reflete apenas o que foi aprovado
            // e nunca fica presa no estado "em produção"
            quantidadeRestante = demanda.getQuantidadeProdutos() - quantidadeAprovada;
            demanda.atualizarQuantidade(quantidadeRestante);

            if (quantidadeRestante == 0) {
                demanda.concluir();
            } else {
                demanda.voltarParaPendente();
            }
        }

        if (quantidadeRestante == 0) {
            System.out.println("Demanda completamente atendida.");
            return;
        }

        // Produção parcial: a demanda volta para a fila
        System.out.println("Restaram " + quantidadeRestante + " produtos na demanda.");
    }

    /**
     * Troca a estratégia de produção em tempo de execução.
     *
     * @param novaEstrategia estratégia que passa a escolher as demandas
     */
    public void setEstrategia(EstrategiaProducao novaEstrategia) {
        this.estrategiaAtual = novaEstrategia;
        System.out.println("Estratégia alterada para: " + novaEstrategia.getNomeEstrategia());
    }

    /**
     * Nome da estratégia de produção ativa, mostrado no menu principal.
     *
     * @return nome da estratégia atual
     */
    public String getNomeEstrategiaAtual() {
        return estrategiaAtual.getNomeEstrategia();
    }

    /**
     * Pede à estratégia ativa a próxima demanda e a fabrica com {@link #fabricarDemanda(int)}.
     *
     * @throws DemandaInvalidaException se nenhuma demanda for elegível ou se a demanda escolhida for recusada
     * @throws MaquinaIndisponivelException se alguma máquina estiver desligada ou quebrada
     * @throws RecursoInsuficienteException se faltar matéria-prima ou orçamento
     */
    public void executarProximaProducao() {
        Demanda escolhida = estrategiaAtual.selecionarDemanda(demandas, budget);

        if (escolhida == null) {
            throw new DemandaInvalidaException(
                    "Nenhuma demanda elegível para a estratégia " + estrategiaAtual.getNomeEstrategia()
                            + ". Registre uma demanda pendente com quantidade maior que zero."
            );
        }

        System.out.println(estrategiaAtual.getNomeEstrategia() + " escolheu: " + escolhida.getInstrumento().getNome() + " " + escolhida.getLinha().getNome());
        fabricarDemanda(demandas.indexOf(escolhida) + 1);
    }

    /**
     * Exibe as matérias-primas numeradas, com estoque e custo por unidade.
     */
    public void exibirMateriasPrimas() {

        System.out.println();
        System.out.println("========================================");
        System.out.println("            MATÉRIAS-PRIMAS");
        System.out.println("========================================");

        if (materiasPrimas.isEmpty()) {

            System.out.println("Nenhuma matéria-prima cadastrada.");
            return;

        }

        for (int i = 0; i < materiasPrimas.size(); i++) {

            MateriaPrima materiaPrima = materiasPrimas.get(i);
            System.out.println((i + 1) + " - " + materiaPrima.getNome() + " | Estoque: " + materiaPrima.getQuantidade() + " " + materiaPrima.getUnidade() + " | Custo: R$ " + materiaPrima.getCustoPorUnidade());

        }
    }

    /**
     * Compra matéria-prima, aumentando o estoque e debitando o orçamento. Tudo é validado
     * antes de alterar estoque ou orçamento.
     *
     * @param numeroMateriaPrima número da matéria-prima, como mostrado em {@link #exibirMateriasPrimas()}
     * @param quantidadeComprada quantidade a comprar, positiva e finita
     * @throws EntradaInvalidaException se o número não existir ou a quantidade não for um número positivo
     * @throws RecursoInsuficienteException se o orçamento não cobrir o custo da compra
     */
    public void comprarMateriaPrima(int numeroMateriaPrima, double quantidadeComprada) {
        int indice = numeroMateriaPrima - 1;

        if (indice < 0 || indice >= materiasPrimas.size()) {
            throw new EntradaInvalidaException(
                    "Matéria-prima inválida: o número " + numeroMateriaPrima
                            + " não existe (escolha de 1 a " + materiasPrimas.size() + ")."
            );
        }

        if (!(quantidadeComprada > 0) || Double.isInfinite(quantidadeComprada)) {
            throw new EntradaInvalidaException("A quantidade comprada deve ser um número positivo.");
        }

        MateriaPrima materiaPrima = materiasPrimas.get(indice);
        double custoCompra = quantidadeComprada * materiaPrima.getCustoPorUnidade();

        if (custoCompra > budget) {
            throw new RecursoInsuficienteException(
                    String.format("Budget insuficiente para comprar %.2f %s de %s: custo R$ %.2f, budget atual R$ %.2f. Nenhuma compra foi realizada.",
                            quantidadeComprada, materiaPrima.getUnidade(), materiaPrima.getNome(), custoCompra, budget)
            );
        }

        materiaPrima.adicionarEstoque(quantidadeComprada);
        budget -= custoCompra;

        System.out.printf("Compra de %s realizada por R$ %.2f.%n", materiaPrima.getNome(), custoCompra);
        System.out.printf("Budget restante: R$ %.2f.%n", budget);
    }

    /**
     * Exibe o orçamento atual.
     */
    public void exibirBudget() {
        System.out.printf("Budget atual: R$ %.2f%n", budget);
    }

    /**
     * Exibe o armazém de produtos acabados, agrupando instrumentos iguais (mesmo instrumento,
     * linha e lote), com quantidade, qualidade e número de itens em risco.
     */
    public void exibirArmazem() {
        String separador = "------------------------------------------------------------------------";

        System.out.println();
        System.out.println("========================================================================");
        System.out.println("                    ARMAZÉM - INSTRUMENTOS PRONTOS");
        System.out.println("========================================================================");

        if (produtosFabricados.isEmpty()) {
            System.out.println("O armazém está vazio.");
            return;
        }

        // Agrupa instrumentos iguais (mesmo instrumento, linha e lote) e conta o total de cada instrumento
        Map<String, List<Produto>> grupos = new LinkedHashMap<>();
        Map<String, Integer> totalPorInstrumento = new LinkedHashMap<>();

        for (Produto produto : produtosFabricados) {
            String chave = produto.getNome() + "|" + produto.getTipo() + "|" + produto.getLote();
            grupos.computeIfAbsent(chave, k -> new ArrayList<>()).add(produto);
            totalPorInstrumento.merge(produto.getNome(), 1, Integer::sum);
        }

        System.out.printf("%-12s %-16s %-15s %5s %10s %9s%n", "Instrumento", "Linha", "Lote", "Qtd", "Qualidade", "Em risco");
        System.out.println(separador);

        for (List<Produto> grupo : grupos.values()) {
            Produto exemplo = grupo.get(0);
            int emRisco = 0;

            for (Produto produto : grupo) {
                if (produto.precisaManutencao()) {
                    emRisco++;
                }
            }

            System.out.printf("%-12s %-16s %-15s %5d %9.0f%% %9d%n",
                exemplo.getNome(), exemplo.getTipo(), exemplo.getLote(),
                grupo.size(), exemplo.getQualidade() * 100, emRisco);
        }

        List<String> resumo = new ArrayList<>();
        for (Map.Entry<String, Integer> total : totalPorInstrumento.entrySet()) {
            resumo.add(total.getKey() + ": " + total.getValue());
        }

        System.out.println(separador);
        System.out.println("Total armazenado: " + produtosFabricados.size() + " instrumentos  |  " + String.join("  |  ", resumo));
    }

    /**
     * Imprime o relatório de auditoria em cascata: a linha de produção (que propaga o pedido
     * às máquinas e, destas, às suas partes) e os instrumentos prontos, todos tratados por
     * igual através de {@link Auditavel}, com o resumo de quantos precisam de intervenção.
     */
    public void gerarAuditoriaGeral() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("           RELATÓRIO DE AUDITORIA");
        System.out.println("========================================");

        // Linha (e, em cascata, máquinas e partes) e instrumentos entram na mesma lista
        List<Auditavel> componentes = new ArrayList<>();
        componentes.add(linha);
        componentes.addAll(produtosFabricados);

        int produtosEmRisco = 0;

        for (Auditavel componente : componentes) {
            if (componente instanceof Produto) {
                if (componente.precisaManutencao()) {
                    produtosEmRisco++;
                }
            }

            System.out.println(componente.gerarRelatorioDiagnostico());
        }

        System.out.println("----------------------------------------");
        System.out.println("Resumo: " + linha.contarMaquinasQuePrecisamManutencao() + " de "
                + linha.getQuantidadeMaquinas() + " máquinas e " + produtosEmRisco + " de "
                + produtosFabricados.size() + " instrumentos precisam de intervenção.");
        System.out.println("========================================");
    }

    /**
     * Exibe as máquinas numeradas com o respectivo diagnóstico (incluindo as partes de cada uma).
     */
    public void exibirMaquinas() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("               MÁQUINAS");
        System.out.println("========================================");

        List<Maquina> maquinas = linha.getMaquinas();

        for (int i = 0; i < maquinas.size(); i++) {
            System.out.println((i + 1) + " - " + maquinas.get(i).gerarRelatorioDiagnostico());
        }
    }

    /**
     * Repara a máquina de número informado, restaurando a saúde total e reparando suas partes.
     *
     * @param numeroMaquina número da máquina, como mostrado em {@link #exibirMaquinas()}
     * @throws EntradaInvalidaException se o número não existir
     */
    public void repararMaquina(int numeroMaquina) {
        Maquina maquina = linha.repararMaquina(numeroMaquina);
        System.out.println(maquina.getNome() + " foi reparada e está com saúde total (motor, sensor e ferramentas incluídos).");
    }

    /**
     * Exibe a linha de produção: o nome, e cada máquina numerada com seu estado e um resumo das
     * partes. Os dados das partes chegam pelos métodos repassadores da máquina, sem que o
     * gerenciador conheça o motor, o sensor ou as ferramentas.
     */
    public void exibirLinha() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("        LINHA DE PRODUÇÃO: " + linha.getNome().toUpperCase());
        System.out.println("========================================");

        List<Maquina> maquinas = linha.getMaquinas();

        for (int i = 0; i < maquinas.size(); i++) {
            Maquina maquina = maquinas.get(i);
            String estado;

            if (maquina.estaQuebrada()) {
                estado = "QUEBRADA";
            } else if (!maquina.estaLigada()) {
                estado = "DESLIGADA";
            } else {
                estado = "OPERACIONAL";
            }

            System.out.printf("%d - %s [%s] | %s | Motor: %.0f°C | Sensor: %.0f%% calibrado | Ferramentas gastas: %d/%d%n",
                    i + 1, maquina.getNome(), maquina.getTipo(), estado,
                    maquina.lerTemperaturaMotor(), maquina.lerCalibracaoSensor(),
                    maquina.contarFerramentasGastas(), maquina.getQuantidadeFerramentas());
        }
    }

    /**
     * Imprime a auditoria estrutural detalhada: a linha, cada máquina e cada parte, em formato
     * hierárquico e indentado (linha &gt; máquina &gt; partes).
     */
    public void gerarAuditoriaEstrutural() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("        AUDITORIA ESTRUTURAL");
        System.out.println("========================================");
        System.out.println(linha.gerarRelatorioDiagnostico());
        System.out.println("========================================");
    }

    /**
     * Deixa o motor da máquina de número informado esfriar.
     *
     * @param numeroMaquina número da máquina, como mostrado em {@link #exibirLinha()}
     * @throws EntradaInvalidaException se o número não existir
     */
    public void resfriarMotorDaMaquina(int numeroMaquina) {
        Maquina maquina = linha.getMaquina(numeroMaquina);
        maquina.resfriarMotor();
        System.out.println("O motor de " + maquina.getNome() + " esfriou até a temperatura ambiente.");
    }

    /**
     * Recalibra o sensor da máquina de número informado.
     *
     * @param numeroMaquina número da máquina, como mostrado em {@link #exibirLinha()}
     * @throws EntradaInvalidaException se o número não existir
     */
    public void calibrarSensorDaMaquina(int numeroMaquina) {
        Maquina maquina = linha.getMaquina(numeroMaquina);
        maquina.calibrarSensor();
        System.out.println("O sensor de " + maquina.getNome() + " foi recalibrado.");
    }

    /**
     * Troca as ferramentas da máquina de número informado.
     *
     * @param numeroMaquina número da máquina, como mostrado em {@link #exibirLinha()}
     * @throws EntradaInvalidaException se o número não existir
     */
    public void trocarFerramentasDaMaquina(int numeroMaquina) {
        Maquina maquina = linha.getMaquina(numeroMaquina);
        maquina.trocarFerramentas();
        System.out.println("As ferramentas de " + maquina.getNome() + " foram trocadas.");
    }

    /**
     * Calcula o custo de operar as máquinas para fabricar um instrumento, que é
     * a soma do custo de operação de todas as máquinas da linha.
     *
     * @return custo de produção de um instrumento em reais
     */
    public double calcularCustoProducao() {
        return linha.calcularCustoProducao();
    }

    private Demanda buscarDemanda(LinhaProduto linha, Instrumento instrumento) {

        for (Demanda demanda : demandas) {

            if (demanda.getLinha() == linha && demanda.getInstrumento() == instrumento) {
                return demanda;
            }
        }

        return null;
    }

    private MateriaPrima buscarMateriaPrimaDoInstrumento(Instrumento instrumento) {

        for (MateriaPrima materiaPrima : materiasPrimas) {

            if (materiaPrima.getNome().toLowerCase().contains(instrumento.getMateriaPrima())) {
                return materiaPrima;
            }
        }

        throw new RecursoInsuficienteException(
                "Não existe matéria-prima cadastrada para fabricar " + instrumento.getNome() + "."
        );
    }

    private Demanda buscarDemandaPorNumero(int numeroDemanda) {
        int indice = numeroDemanda - 1;

        if (indice < 0 || indice >= demandas.size()) {
            throw new DemandaInvalidaException(
                    "Demanda inválida: o número " + numeroDemanda
                            + " não corresponde a nenhuma demanda (existem " + demandas.size() + " registradas)."
            );
        }

        return demandas.get(indice);
    }

    // Confere se há matéria-prima e budget para fabricar mais um instrumento (não altera nada)
    private void verificarRecursos(MateriaPrima materiaPrima, double materiaPrimaPorUnidade, double custoProducao) {
        if (!materiaPrima.verificarDisponibilidade(materiaPrimaPorUnidade)) {
            throw new RecursoInsuficienteException(
                    String.format("Matéria-prima insuficiente: %s (necessário %.2f %s por instrumento, estoque %.2f). Compre mais no menu principal.",
                            materiaPrima.getNome(), materiaPrimaPorUnidade, materiaPrima.getUnidade(), materiaPrima.getQuantidade())
            );
        }

        if (budget < custoProducao) {
            throw new RecursoInsuficienteException(
                    String.format("Budget insuficiente: cada instrumento custa R$ %.2f de operação das máquinas e o budget atual é R$ %.2f.",
                            custoProducao, budget)
            );
        }
    }

    // Cria o instrumento na classe da linha pedida pela demanda
    private Produto criarProduto(Demanda demanda) {
        Instrumento instrumento = demanda.getInstrumento();
        double quantidadeMateriaPrima = demanda.calcularMateriaPrimaPorUnidade();
        String id = String.format("P%03d", Produto.getTotalProdutosFabricados() + 1);

        switch (demanda.getLinha()) {
            case STUDIO:
                return new LinhaStudio(id, instrumento.getNome(), StatusProduto.AGUARDANDO, quantidadeMateriaPrima, instrumento.getTempoProducao());
            case PALCO:
                return new LinhaPalco(id, instrumento.getNome(), StatusProduto.AGUARDANDO, quantidadeMateriaPrima, instrumento.getTempoProducao());
            case CLASSICA:
                return new LinhaClassica(id, instrumento.getNome(), StatusProduto.AGUARDANDO, quantidadeMateriaPrima, instrumento.getTempoProducao());
            default:
                throw new IllegalArgumentException("Linha de produção desconhecida: " + demanda.getLinha());
        }
    }

    /**
     * Restaura o orçamento e as demandas recuperados de um arquivo de persistência.
     * Substitui as demandas atuais, sem combiná-las com as recuperadas.
     *
     * @param budgetRecuperado orçamento salvo (finito e não negativo)
     * @param demandasRecuperadas demandas salvas
     * @throws EntradaInvalidaException se o orçamento ou a lista de demandas forem inválidos
     */
    public void restaurarEstado(double budgetRecuperado, List<Demanda> demandasRecuperadas) {
        if (!Double.isFinite(budgetRecuperado) || budgetRecuperado < 0 || demandasRecuperadas == null) {
            throw new EntradaInvalidaException("Estado inválido do gerenciador de produção.");
        }

        this.budget = budgetRecuperado;
        this.demandas = new ArrayList<>(demandasRecuperadas);
    }

    /**
     * Cenário de operação escolhido no início da execução.
     *
     * @return cenário atual
     */
    public Cenario getCenario(){
        return cenario;
    }

    /**
    * Retorna as matérias-primas cadastradas na fábrica.
    */
    public List<MateriaPrima> getMateriasPrimas() {
        return Collections.unmodifiableList(materiasPrimas);
    }

    /**
    * Retorna as demandas registradas.
    */
    public List<Demanda> getDemandas() {
        return Collections.unmodifiableList(demandas);
    }

    /**
     * Retorna a linha de produção da fábrica.
     */
    public LinhaDeProducao getLinha() {
        return linha;
    }

    /**
    * Retorna o orçamento atual da fábrica.
    */
    public double getBudget() {
        return budget;
    }
}
