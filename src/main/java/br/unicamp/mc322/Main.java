package br.unicamp.mc322;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.IntConsumer;

import br.unicamp.mc322.exception.DemandaInvalidaException;
import br.unicamp.mc322.exception.EntradaInvalidaException;
import br.unicamp.mc322.exception.FabricaException;
import br.unicamp.mc322.exception.MaquinaIndisponivelException;
import br.unicamp.mc322.exception.PersistenciaException;
import br.unicamp.mc322.exception.RecursoInsuficienteException;
import br.unicamp.mc322.model.maquinas.LinhaDeProducao;
import br.unicamp.mc322.model.maquinas.MaquinaEmbalagem;
import br.unicamp.mc322.model.maquinas.MaquinaInspecaoSonora;
import br.unicamp.mc322.model.maquinas.MaquinaMontagem;
import br.unicamp.mc322.persistence.PersistenciaPlanta;
import br.unicamp.mc322.model.producao.Cenario;
import br.unicamp.mc322.service.GerenciadorProducao;
import br.unicamp.mc322.model.producao.MateriaPrima;
import br.unicamp.mc322.model.produtos.Instrumento;
import br.unicamp.mc322.model.produtos.LinhaProduto;

import br.unicamp.mc322.strategy.EstrategiaMaiorEncomenda;
import br.unicamp.mc322.strategy.EstrategiaOrcamento;
import br.unicamp.mc322.strategy.EstrategiaOrdemDePedido;
import br.unicamp.mc322.strategy.EstrategiaProducao;

/**
 * Onde o sistema é iniciado e onde tem a interação com o usuário da Fábrica Harmonia.
 * <p>
 * Apresenta os menus em modo texto, lê e valida o que o usuário digita no terminal e
 * delega as operações de negócio ao {@link GerenciadorProducao}. Esta é a camada
 * responsável por capturar as exceções de domínio ({@link FabricaException} e suas
 * subclasses): a mensagem é exibida ao usuário e o programa volta ao menu, sem ser encerrado.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class Main {

    /** Arquivo XML onde o estado da planta é salvo e de onde é carregado. */
    private static final Path ARQUIVO_PERSISTENCIA = Path.of("dados", "planta.xml");

    /**
     * Construtor padrão da classe principal.
     */
    public Main() {
    }

    /**
     * Exibe o menu de demandas e o repete até o usuário escolher {@code 0}.
     * <p>
     * Há uma opção de atualização para cada {@link LinhaProduto} (geradas a partir de
     * {@code LinhaProduto.values()}, então uma nova linha aparece sozinha no menu) e uma
     * opção para listar as demandas registradas. Opções inexistentes exibem um aviso e o
     * menu é reapresentado.
     * </p>
     *
     * @param entrada {@link Scanner} usado para ler as escolhas do usuário
     * @param gerenciadorProducao gerenciador que mantém as demandas
     */
    public static void menuDemandas(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        LinhaProduto[] linhas = LinhaProduto.values();
        int opcaoVerDemandas = linhas.length + 1;
        int opcao;
        do {
            System.out.println();
            System.out.println("---------------- DEMANDAS ----------------");
            for (int i = 0; i < linhas.length; i++) {
                System.out.println((i + 1) + " - Atualizar demanda da " + linhas[i].getNome());
            }
            System.out.println(opcaoVerDemandas + " - Ver demandas");
            System.out.println("0 - Voltar");

            opcao = lerInteiro(entrada, "Escolha uma opção: ");

            if (opcao >= 1 && opcao <= linhas.length) {
                atualizarDemanda(entrada, gerenciadorProducao, linhas[opcao - 1]);
            } else if (opcao == opcaoVerDemandas) {
                gerenciadorProducao.exibirDemandas();
            } else if (opcao != 0) {
                System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    /**
     * Exibe o menu de fabricação: processar a próxima demanda (escolhida pela estratégia ativa)
     * ou fabricar uma demanda específica. Repete até o usuário escolher {@code 0}.
     * <p>
     * Recusas de produção (falta de matéria-prima, orçamento insuficiente, máquina parada,
     * demanda inexistente ou incompatível) são tratadas por
     * {@link #executarComTratamento(Scanner, String, Runnable)}.
     * </p>
     *
     * @param entrada {@link Scanner} usado para ler as escolhas do usuário
     * @param gerenciadorProducao gerenciador que executa a produção
     */
    public static void menuFabricacao(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        int opcao;
        do {
            System.out.println();
            System.out.println("--------------- FABRICAÇÃO ----------------");
            System.out.println("1 - Processar próxima demanda (usa estratégia ativa)");
            System.out.println("2 - Fabricar demanda específica");
            System.out.println("0 - Voltar");

            opcao = lerInteiro(entrada, "Escolha uma opção: ");

            if (opcao == 1) {
                executarComTratamento(entrada, "iniciar a produção", gerenciadorProducao::executarProximaProducao);
            } else if (opcao == 2) {
                fabricarDemanda(entrada, gerenciadorProducao);
            } else if (opcao != 0) {
                System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    /**
     * Exibe o menu de consultas (armazém de produtos acabados e estoque de matéria-prima).
     * Repete até o usuário escolher {@code 0}. Não altera o estado da fábrica.
     *
     * @param entrada {@link Scanner} usado para ler as escolhas do usuário
     * @param gerenciadorProducao gerenciador que fornece os dados exibidos
     */
    public static void menuConsultar(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        int opcao;
        do {
            System.out.println();
            System.out.println("---------------- CONSULTAR ----------------");
            System.out.println("1 - Ver armazém (produtos acabados)");
            System.out.println("2 - Ver estoque de matéria-prima");
            System.out.println("0 - Voltar");

            opcao = lerInteiro(entrada, "Escolha uma opção: ");

            if (opcao == 1) {
                gerenciadorProducao.exibirArmazem();
            } else if (opcao == 2) {
                gerenciadorProducao.exibirMateriasPrimas();
            } else if (opcao != 0) {
                System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    /**
     * Exibe a estratégia de produção atual e permite trocá-la em tempo de execução.
     * Diferente dos demais menus, apresenta as opções uma única vez e volta ao menu principal.
     *
     * @param entrada {@link Scanner} usado para ler a escolha do usuário
     * @param gerenciadorProducao gerenciador que recebe a nova estratégia
     * @param ordemDePedido estratégia que atende primeiro quem pediu primeiro
     * @param maiorEncomenda estratégia que prioriza a demanda com mais instrumentos
     * @param orcamento estratégia que prioriza a demanda que rende mais instrumentos com o orçamento atual
     */
    public static void menuEstrategia(
        Scanner entrada,
        GerenciadorProducao gerenciadorProducao,
        EstrategiaProducao ordemDePedido,
        EstrategiaProducao maiorEncomenda,
        EstrategiaProducao orcamento
    ) {
        System.out.println();
        System.out.println("------------- GERENCIAR ESTRATÉGIA -------------");
        System.out.println("Estratégia atual: " + gerenciadorProducao.getNomeEstrategiaAtual());
        System.out.println("1 - Ordem de Pedido");
        System.out.println("2 - Maior Encomenda");
        System.out.println("3 - Estratégia do Orçamento");
        System.out.println("0 - Voltar");

        int opcao = lerInteiro(entrada, "Escolha uma opção: ");

        if (opcao == 1) {
            gerenciadorProducao.setEstrategia(ordemDePedido);
        } else if (opcao == 2) {
            gerenciadorProducao.setEstrategia(maiorEncomenda);
        } else if (opcao == 3) {
            gerenciadorProducao.setEstrategia(orcamento);
        } else if (opcao != 0) {
            System.out.println("Opção inválida.");
        }
    }

    /**
     * Exibe o menu de auditoria: relatório geral da fábrica ou reparo de uma máquina.
     * Repete até o usuário escolher {@code 0}. Números de máquina inexistentes são recusados
     * com uma mensagem de erro, sem encerrar o programa.
     *
     * @param entrada {@link Scanner} usado para ler as escolhas do usuário
     * @param gerenciadorProducao gerenciador que gera o relatório e executa o reparo
     */
    public static void menuAuditoria(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        int opcao;
        do {
            System.out.println();
            System.out.println("---------------- AUDITORIA ----------------");
            System.out.println("1 - Relatório geral");
            System.out.println("2 - Reparar máquina");
            System.out.println("3 - Auditoria estrutural detalhada (linha > máquina > partes)");
            System.out.println("0 - Voltar");

            opcao = lerInteiro(entrada, "Escolha uma opção: ");

            if (opcao == 1) {
                gerenciadorProducao.gerarAuditoriaGeral();
            } else if (opcao == 3) {
                gerenciadorProducao.gerarAuditoriaEstrutural();
            } else if (opcao == 2) {
                gerenciadorProducao.exibirMaquinas();
                System.out.println("0 - Voltar");
                int escolha = lerInteiro(entrada, "Escolha a máquina para reparar: ");
                if (escolha != 0) {
                    executarComTratamento(entrada, "reparar a máquina", () -> gerenciadorProducao.repararMaquina(escolha));
                }
            } else if (opcao != 0) {
                System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    /**
     * Exibe o menu da linha de produção (o agrupador das máquinas) e o repete até o usuário
     * escolher {@code 0}. Permite ver a linha, a auditoria estrutural e fazer a manutenção de
     * uma máquina inteira ou de uma de suas partes. O usuário só escolhe a máquina: as partes
     * nunca são acessadas diretamente, a máquina as repara por delegação.
     *
     * @param entrada {@link Scanner} usado para ler as escolhas do usuário
     * @param gerenciadorProducao gerenciador que mantém a linha
     */
    public static void menuLinhaProducao(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        int opcao;
        do {
            System.out.println();
            System.out.println("------------ LINHA DE PRODUÇÃO ------------");
            System.out.println("1 - Ver linha e máquinas");
            System.out.println("2 - Auditoria estrutural detalhada");
            System.out.println("3 - Reparar máquina completa (motor, sensor e ferramentas)");
            System.out.println("4 - Resfriar motor de uma máquina");
            System.out.println("5 - Recalibrar sensor de uma máquina");
            System.out.println("6 - Trocar ferramentas de uma máquina");
            System.out.println("0 - Voltar");

            opcao = lerInteiro(entrada, "Escolha uma opção: ");

            if (opcao == 1) {
                gerenciadorProducao.exibirLinha();
            } else if (opcao == 2) {
                gerenciadorProducao.gerarAuditoriaEstrutural();
            } else if (opcao == 3) {
                escolherMaquinaEExecutar(entrada, gerenciadorProducao, "reparar a máquina", gerenciadorProducao::repararMaquina);
            } else if (opcao == 4) {
                escolherMaquinaEExecutar(entrada, gerenciadorProducao, "resfriar o motor", gerenciadorProducao::resfriarMotorDaMaquina);
            } else if (opcao == 5) {
                escolherMaquinaEExecutar(entrada, gerenciadorProducao, "recalibrar o sensor", gerenciadorProducao::calibrarSensorDaMaquina);
            } else if (opcao == 6) {
                escolherMaquinaEExecutar(entrada, gerenciadorProducao, "trocar as ferramentas", gerenciadorProducao::trocarFerramentasDaMaquina);
            } else if (opcao != 0) {
                System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    /**
     * Mostra a linha, pede o número de uma máquina e executa a ação escolhida sobre ela, tratando
     * as {@link FabricaException} (por exemplo, número de máquina inexistente).
     *
     * @param entrada {@link Scanner} usado para ler a escolha do usuário
     * @param gerenciadorProducao gerenciador que exibe a linha
     * @param operacao descreve a ação, usada no pedido e na mensagem de erro
     * @param acao ação que recebe o número da máquina escolhida
     */
    public static void escolherMaquinaEExecutar(
            Scanner entrada,
            GerenciadorProducao gerenciadorProducao,
            String operacao,
            IntConsumer acao
    ) {
        gerenciadorProducao.exibirLinha();
        System.out.println("0 - Voltar");

        int escolha = lerInteiro(entrada, "Escolha a máquina (" + operacao + "): ");

        if (escolha != 0) {
            executarComTratamento(entrada, operacao, () -> acao.accept(escolha));
        }
    }

    /**
     * Executa uma operação do domínio e trata as {@link FabricaException} lançadas por ela.
     * <p>
     * Se a operação recusar o pedido, o erro é exibido por
     * {@link #exibirErro(Scanner, String, String, String)} e o usuário volta ao menu. O estado
     * da fábrica continua consistente, pois as operações validam tudo antes de alterá-lo.
     * Exceções que não são de domínio (falhas inesperadas) não são capturadas aqui de propósito,
     * para não esconder bugs.
     * </p>
     *
     * @param entrada {@link Scanner} usado para esperar o usuário ler a mensagem
     * @param operacao descreve a ação, usado na frase "Não foi possível ..."
     * @param acao operação do domínio a ser executada
     */
    public static void executarComTratamento(Scanner entrada, String operacao, Runnable acao) {
        try {
            acao.run();
        } catch (RecursoInsuficienteException e) {
            exibirErro(entrada, "Recurso insuficiente", operacao, e.getMessage());
        } catch (DemandaInvalidaException e) {
            exibirErro(entrada, "Demanda inválida", operacao, e.getMessage());
        } catch (MaquinaIndisponivelException e) {
            exibirErro(entrada, "Máquina indisponível", operacao, e.getMessage());
        } catch (EntradaInvalidaException e) {
            exibirErro(entrada, "Entrada inválida", operacao, e.getMessage());
        } catch (PersistenciaException e) {
            exibirErro(entrada, "Persistência", operacao, e.getMessage());
        } catch (FabricaException e) {
            exibirErro(entrada, "Erro", operacao, e.getMessage());
        }
    }

    /**
     * Mostra uma mensagem de erro em uma caixa e espera o usuário pressionar ENTER
     * antes de voltar ao menu.
     *
     * @param entrada {@link Scanner} usado para esperar o ENTER
     * @param categoria tipo do erro, exibido no título (por exemplo, "Recurso insuficiente")
     * @param operacao ação que não pôde ser realizada
     * @param mensagem explicação detalhada do motivo, vinda da exceção
     */
    public static void exibirErro(Scanner entrada, String categoria, String operacao, String mensagem) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("ERRO - " + categoria);
        System.out.println("========================================");
        System.out.println("Não foi possível " + operacao + ":");
        System.out.println(mensagem);
        System.out.println();
        aguardarEnter(entrada);
    }

    /**
     * Pausa a execução até o usuário pressionar ENTER.
     * <p>
     * O primeiro {@code nextLine} descarta o resto da linha em que o usuário digitou o número
     * da opção; o segundo espera o ENTER.
     * </p>
     *
     * @param entrada {@link Scanner} do qual as linhas são consumidas
     */
    public static void aguardarEnter(Scanner entrada) {
        System.out.print("Pressione ENTER para continuar...");

        if (entrada.hasNextLine()) {
            entrada.nextLine();
        }
        if (entrada.hasNextLine()) {
            entrada.nextLine();
        }
    }

    /**
     * Lê um número inteiro do terminal, repetindo o pedido enquanto a entrada for inválida.
     * Entradas que não são inteiros são descartadas com um aviso.
     *
     * @param entrada {@link Scanner} do qual o valor é lido
     * @param mensagem texto exibido como pedido de entrada
     * @return o inteiro digitado pelo usuário
     * @throws NoSuchElementException se a entrada for encerrada antes de um valor válido
     *         (tratada em {@link #main(String[])})
     */
    public static int lerInteiro(Scanner entrada, String mensagem) {
        while (true) {
            System.out.print(mensagem);

            if (entrada.hasNextInt()) {
                return entrada.nextInt();
            }

            System.out.println("Entrada inválida. Digite um número inteiro.");
            entrada.next();
        }
    }

    /**
     * Lê um número decimal do terminal, repetindo o pedido enquanto a entrada for inválida.
     *
     * @param entrada {@link Scanner} do qual o valor é lido
     * @param mensagem texto exibido como pedido de entrada
     * @return o número digitado pelo usuário
     * @throws NoSuchElementException se a entrada for encerrada antes de um valor válido
     */
    public static double lerDouble(Scanner entrada, String mensagem) {
        while (true) {
            System.out.print(mensagem);

            if (entrada.hasNextDouble()) {
                return entrada.nextDouble();
            }

            System.out.println("Entrada inválida. Digite um valor numérico.");
            entrada.next();
        }
    }

    /**
     * Exibe a apresentação da fábrica: nome, instrumentos produzidos e linhas de produção.
     */
    public static void exibirIntroducao() {
        System.out.println("========================================");
        System.out.println("           FÁBRICA HARMONIA");
        System.out.println("   \"Transformando materiais em harmonia\"");
        System.out.println("========================================");
        System.out.println("Violões, guitarras, ukuleles, baterias, pandeiros e flautas");
        System.out.println("fabricados em três linhas:");
        System.out.println();
        System.out.println("  Linha Studio   - acabamento de estúdio, inspeção rigorosa");
        System.out.println("  Linha Palco    - feita para aguentar shows e ensaios");
        System.out.println("  Linha Clássica - simples e resistente, para começar");
        System.out.println();
        System.out.println("Desenvolvido por: Arthur Pessoa");
        System.out.println("========================================");
    }

    /**
     * Pede ao usuário que escolha o cenário de operação, repetindo até receber uma opção válida.
     *
     * @param entrada {@link Scanner} usado para ler a escolha
     * @return {@link Cenario#IDEAL} ou {@link Cenario#APOCALIPTICO}, conforme a escolha
     */
    public static Cenario escolherCenario(Scanner entrada) {
        while (true) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("           ESCOLHA O CENÁRIO");
            System.out.println("========================================");
            System.out.println("1 - Ideal (orçamento farto, poucas falhas)");
            System.out.println("2 - Apocalíptico (orçamento curto, máquinas instáveis)");

            int escolha = lerInteiro(entrada, "Cenário: ");

            if (escolha == 1) {
                return Cenario.IDEAL;
            } else if (escolha == 2) {
                return Cenario.APOCALIPTICO;
            }

            System.out.println("Opção inválida.");
        }
    }

    /**
     * Lista os instrumentos a partir do enum {@link Instrumento} (um instrumento novo aparece
     * aqui sozinho) e lê a escolha do usuário.
     *
     * @param entrada {@link Scanner} usado para ler a escolha
     * @return o instrumento escolhido, ou {@code null} se o usuário escolheu voltar
     *         ou digitou uma opção inexistente
     */
    public static Instrumento escolherInstrumento(Scanner entrada) {
        Instrumento[] instrumentos = Instrumento.values();

        System.out.println();
        for (int i = 0; i < instrumentos.length; i++) {
            System.out.println((i + 1) + " - " + instrumentos[i].getNome());
        }
        System.out.println("0 - Voltar");

        int escolha = lerInteiro(entrada, "Escolha o instrumento: ");

        if (escolha >= 1 && escolha <= instrumentos.length) {
            return instrumentos[escolha - 1];
        }

        if (escolha != 0) {
            System.out.println("Opção inválida.");
        }
        return null;
    }

    /**
     * Pede o instrumento e a nova quantidade e atualiza a demanda da linha de produto indicada.
     * Quantidades negativas são recusadas pelo domínio e exibidas como erro.
     *
     * @param entrada {@link Scanner} usado para ler os dados
     * @param gerenciadorProducao gerenciador que atualiza a demanda
     * @param linha linha de produto cuja demanda será atualizada
     */
    public static void atualizarDemanda(Scanner entrada, GerenciadorProducao gerenciadorProducao, LinhaProduto linha) {
        Instrumento instrumento = escolherInstrumento(entrada);

        if (instrumento == null) {
            return;
        }

        int quantidade = lerInteiro(entrada, "Informe a quantidade da demanda: ");
        executarComTratamento(entrada, "atualizar a demanda", () -> gerenciadorProducao.atualizarDemanda(linha, quantidade, instrumento));
    }

    /**
     * Lista as demandas, pede o número da demanda desejada e tenta fabricá-la.
     * Escolher {@code 0} volta ao menu sem fazer nada.
     *
     * @param entrada {@link Scanner} usado para ler a escolha
     * @param gerenciadorProducao gerenciador que executa a produção
     */
    public static void fabricarDemanda(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        gerenciadorProducao.exibirDemandas();
        System.out.println("0 - Voltar");

        int escolha = lerInteiro(entrada, "Escolha a demanda que deseja fabricar: ");

        if (escolha == 0) {
            return;
        }

        executarComTratamento(entrada, "iniciar a produção", () -> gerenciadorProducao.fabricarDemanda(escolha));
    }

    /**
     * Lista as matérias-primas, pede qual comprar e a quantidade, e realiza a compra.
     * Número inexistente, quantidade não positiva e orçamento insuficiente são exibidos como erro,
     * sem alterar estoque nem orçamento.
     *
     * @param entrada {@link Scanner} usado para ler os dados
     * @param gerenciadorProducao gerenciador que realiza a compra
     */
    public static void comprarMateriaPrima(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        gerenciadorProducao.exibirMateriasPrimas();
        System.out.println("0 - Voltar");

        int escolha = lerInteiro(entrada, "Escolha a matéria-prima: ");

        if (escolha == 0) {
            return;
        }

        double quantidade = lerDouble(entrada, "Informe a quantidade que deseja comprar: ");
        executarComTratamento(entrada, "comprar a matéria-prima", () -> gerenciadorProducao.comprarMateriaPrima(escolha, quantidade));
    }

    /**
     * Inicia a Fábrica Harmonia.
     * <p>
     * Se a entrada padrão for encerrada, o programa termina de forma
     * limpa, com uma mensagem.
     * </p>
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);

        try {
            executarFabrica(entrada);
        } catch (NoSuchElementException e) {
            // Entrada encerrada: termina de forma limpa
            System.out.println();
            System.out.println("Entrada encerrada. Encerrando a Fábrica Harmonia...");
        } finally {
            entrada.close();
        }
    }

    /**
     * Cria a planta padrão do projeto: seis matérias-primas, uma linha de produção com as três
     * máquinas (montagem, embalagem e inspeção sonora) e o {@link GerenciadorProducao} configurado
     * com o cenário informado.
     *
     * @param cenario cenário de operação da fábrica
     * @return gerenciador com a planta padrão
     */
    public static GerenciadorProducao criarPlantaPadrao(Cenario cenario) {
        ArrayList<MateriaPrima> materiasPrimas = new ArrayList<>();

        materiasPrimas.add(new MateriaPrima("MP01", "Madeira de mogno", 100.0, "kg", 1.0));
        materiasPrimas.add(new MateriaPrima("MP02", "Madeira de cedro", 100.0, "kg", 1.0));
        materiasPrimas.add(new MateriaPrima("MP03", "Cordas de nylon", 200.0, "unidades", 1.0));
        materiasPrimas.add(new MateriaPrima("MP04", "Alumínio", 50.0, "kg", 0.1));
        materiasPrimas.add(new MateriaPrima("MP05", "Couro sintético", 30.0, "m²", 0.1));
        materiasPrimas.add(new MateriaPrima("MP06", "Plástico ABS", 50.0, "kg", 0.1));

        LinhaDeProducao linhaPrincipal = new LinhaDeProducao("Principal");

        linhaPrincipal.adicionarMaquina(new MaquinaMontagem("Máquina de montagem", true, 30.0, 0.10, 25.0));
        linhaPrincipal.adicionarMaquina(new MaquinaEmbalagem("Máquina de embalagem", true, 30.0, 0.20, 10.0));
        linhaPrincipal.adicionarMaquina(new MaquinaInspecaoSonora("Máquina de inspeção sonora", true, 30.0, 0.10, 15.0));

        return new GerenciadorProducao(linhaPrincipal, materiasPrimas, cenario);
    }

    /**
     * Tenta recuperar o estado salvo na execução anterior. A ausência do arquivo (primeira
     * execução) é uma situação normal e não gera mensagem; se o arquivo existir mas for inválido,
     * o motivo é exibido e a fábrica começa do zero, sem que o arquivo seja alterado.
     *
     * @return planta recuperada, ou {@code null} se não houver estado salvo utilizável
     */
    public static GerenciadorProducao carregarEstadoInicial() {
        if (!Files.exists(ARQUIVO_PERSISTENCIA)) {
            return null;
        }

        try {
            GerenciadorProducao carregado = new PersistenciaPlanta().carregar(ARQUIVO_PERSISTENCIA);
            System.out.println();
            System.out.println("Estado anterior da planta carregado de " + ARQUIVO_PERSISTENCIA + ".");
            return carregado;
        } catch (PersistenciaException e) {
            System.out.println();
            System.out.println("Não foi possível carregar o estado salvo: " + e.getMessage());
            System.out.println("Iniciando com uma planta nova.");
            return null;
        }
    }

    /**
     * Salva o estado atual da planta no arquivo de persistência. Se a gravação falhar, o erro é
     * exibido e o arquivo anterior é preservado.
     *
     * @param entrada {@link Scanner} usado para esperar o usuário ler uma eventual mensagem de erro
     * @param gerenciadorProducao planta a ser salva
     */
    public static void salvarEstado(Scanner entrada, GerenciadorProducao gerenciadorProducao) {
        executarComTratamento(entrada, "salvar o estado da planta", () -> {
            new PersistenciaPlanta().salvar(gerenciadorProducao, ARQUIVO_PERSISTENCIA);
            System.out.println("Estado da planta salvo em " + ARQUIVO_PERSISTENCIA + ".");
        });
    }

    /**
     * Carrega o estado salvo. A planta atual só deve ser substituída se o carregamento tiver
     * sucesso: em caso de falha, o erro é exibido e nada é alterado.
     *
     * @param entrada {@link Scanner} usado para esperar o usuário ler uma eventual mensagem de erro
     * @return planta recuperada, ou {@code null} se o carregamento falhou
     */
    public static GerenciadorProducao carregarEstado(Scanner entrada) {
        try {
            GerenciadorProducao carregado = new PersistenciaPlanta().carregar(ARQUIVO_PERSISTENCIA);
            System.out.println("Estado da planta carregado de " + ARQUIVO_PERSISTENCIA + ".");
            return carregado;
        } catch (PersistenciaException e) {
            exibirErro(entrada, "Persistência", "carregar o estado da planta", e.getMessage());
            return null;
        }
    }

    /**
     * Inicia a fábrica e executa o menu principal até o usuário escolher sair.
     * <p>
     * Se existir um estado salvo válido, ele é carregado; caso contrário, o usuário escolhe o
     * cenário e a planta padrão é criada. As três estratégias de produção são criadas, sendo a
     * Ordem de Pedido a ativa no início (a estratégia ativa não é persistida).
     * </p>
     *
     * @param entrada {@link Scanner} usado para ler as escolhas do usuário
     * @throws NoSuchElementException se a entrada for encerrada durante a execução
     */
    public static void executarFabrica(Scanner entrada) {
        exibirIntroducao();

        GerenciadorProducao gerenciadorProducao = carregarEstadoInicial();
        if (gerenciadorProducao == null) {
            gerenciadorProducao = criarPlantaPadrao(escolherCenario(entrada));
        }

        EstrategiaProducao ordemDePedido = new EstrategiaOrdemDePedido();
        EstrategiaProducao maiorEncomenda = new EstrategiaMaiorEncomenda();
        EstrategiaProducao orcamento = new EstrategiaOrcamento(gerenciadorProducao.calcularCustoProducao());

        gerenciadorProducao.setEstrategia(ordemDePedido);

        int opcao;

        do {
            System.out.println();
            System.out.println("========================================");
            System.out.println("            MENU PRINCIPAL");
            System.out.println("========================================");
            System.out.println("Cenário ativo: " + gerenciadorProducao.getCenario().getNome());
            System.out.println("Estratégia atual: " + gerenciadorProducao.getNomeEstrategiaAtual());
            gerenciadorProducao.exibirBudget();
            System.out.println("========================================");
            System.out.println("1 - Demandas");
            System.out.println("2 - Fabricação");
            System.out.println("3 - Consultar");
            System.out.println("4 - Comprar matéria-prima");
            System.out.println("5 - Gerenciar estratégia");
            System.out.println("6 - Auditoria");
            System.out.println("7 - Linha de produção");
            System.out.println("8 - Salvar estado da planta");
            System.out.println("9 - Carregar estado da planta");
            System.out.println("0 - Sair");
            System.out.println("========================================");

            opcao = lerInteiro(entrada, "Escolha uma opção: ");

            if (opcao == 1) {
                menuDemandas(entrada, gerenciadorProducao);
            } else if (opcao == 2) {
                menuFabricacao(entrada, gerenciadorProducao);
            } else if (opcao == 3) {
                menuConsultar(entrada, gerenciadorProducao);
            } else if (opcao == 4) {
                comprarMateriaPrima(entrada, gerenciadorProducao);
            } else if (opcao == 5) {
                menuEstrategia(entrada, gerenciadorProducao, ordemDePedido, maiorEncomenda, orcamento);
            } else if (opcao == 6) {
                menuAuditoria(entrada, gerenciadorProducao);
            } else if (opcao == 7) {
                menuLinhaProducao(entrada, gerenciadorProducao);
            } else if (opcao == 8) {
                salvarEstado(entrada, gerenciadorProducao);
            } else if (opcao == 9) {
                GerenciadorProducao carregado = carregarEstado(entrada);

                if (carregado != null) {
                    gerenciadorProducao = carregado;
                    orcamento = new EstrategiaOrcamento(gerenciadorProducao.calcularCustoProducao());
                    gerenciadorProducao.setEstrategia(ordemDePedido);
                }
            } else if (opcao == 0) {
                System.out.println("Encerrando a Fábrica Harmonia...");
            } else {
                System.out.println("Opção inválida.");
            }

        } while (opcao != 0);
    }
}