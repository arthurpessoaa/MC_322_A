package br.unicamp.mc322.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.unicamp.mc322.exception.PersistenciaException;
import br.unicamp.mc322.model.maquinas.LinhaDeProducao;
import br.unicamp.mc322.model.maquinas.Maquina;
import br.unicamp.mc322.model.producao.Cenario;
import br.unicamp.mc322.model.producao.Demanda;
import br.unicamp.mc322.model.producao.MateriaPrima;
import br.unicamp.mc322.model.producao.StatusDemanda;
import br.unicamp.mc322.model.produtos.Instrumento;
import br.unicamp.mc322.model.produtos.LinhaProduto;
import br.unicamp.mc322.service.GerenciadorProducao;
import br.unicamp.mc322.strategy.EstrategiaOrdemDePedido;
import br.unicamp.mc322.support.Fixtures;

/**
 * Salvar e carregar a planta em XML. Todos os arquivos ficam em uma pasta temporária criada pelo
 * JUnit ({@link TempDir}); o arquivo real da aplicação nunca é tocado.
 */
class PersistenciaPlantaTest {

    private static final double DELTA = 1e-9;

    @TempDir
    Path pasta;

    private PersistenciaPlanta persistencia;
    private Path arquivo;
    private GerenciadorProducao original;

    @BeforeEach
    void prepararPlanta() {
        persistencia = new PersistenciaPlanta();
        arquivo = pasta.resolve("planta.xml");
        original = criarPlantaComEstadoVariado();
    }

    /** Planta com os 3 tipos de máquina, estados de desgaste, e demandas em 3 status diferentes. */
    private GerenciadorProducao criarPlantaComEstadoVariado() {
        LinhaDeProducao linha = Fixtures.linhaCompleta();
        Maquina montagem = linha.getMaquina(1);
        montagem.restaurarEstado(80.0, 2, 60.0, 10.0, 70.0, Fixtures.ferramentasComDesgaste(montagem, 5.0, 10.0, 15.0));
        Maquina inspecao = linha.getMaquina(3);
        inspecao.restaurarEstado(45.5, 7, 33.0, 22.0, 88.0, Fixtures.ferramentasComDesgaste(inspecao, 1.0, 2.0, 3.0));

        GerenciadorProducao gerenciador = new GerenciadorProducao(linha, Fixtures.materiasPrimas(), Cenario.APOCALIPTICO);
        gerenciador.setEstrategia(new EstrategiaOrdemDePedido());

        Demanda pendente = new Demanda(LinhaProduto.CLASSICA, 3, Instrumento.VIOLAO);
        Demanda concluida = new Demanda(LinhaProduto.PALCO, 0, Instrumento.GUITARRA);
        concluida.restaurarStatus(StatusDemanda.CONCLUIDA);
        Demanda cancelada = new Demanda(LinhaProduto.STUDIO, 8, Instrumento.BATERIA);
        cancelada.cancelar();
        gerenciador.restaurarEstado(1234.5, new ArrayList<>(List.of(pendente, concluida, cancelada)));
        gerenciador.getMateriasPrimas().get(1).consumir(25.5);
        return gerenciador;
    }

    private String lerArquivo() throws IOException {
        return Files.readString(arquivo, StandardCharsets.UTF_8);
    }

    private void escreverArquivo(String conteudo) throws IOException {
        Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
    }

    /** Salva a planta, troca o valor da primeira ocorrência da tag e devolve a exceção do carregamento. */
    private PersistenciaException carregarComTagAlterada(String tag, String novoValor) throws IOException {
        persistencia.salvar(original, arquivo);
        String alterado = lerArquivo().replaceFirst(
                "<" + tag + ">[^<]*</" + tag + ">", Matcher.quoteReplacement("<" + tag + ">" + novoValor + "</" + tag + ">"));
        escreverArquivo(alterado);
        return assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
    }

    // salvar

    @Test
    void salvarCriaUmArquivoXmlComOsDadosDaPlanta() throws IOException {
        persistencia.salvar(original, arquivo);

        assertTrue(Files.exists(arquivo));
        String xml = lerArquivo();
        assertTrue(xml.contains("<planta>"));
        assertTrue(xml.contains("<materiasPrimas>"));
        assertTrue(xml.contains("<maquinas>"));
        assertTrue(xml.contains("<demandas>"));
        assertTrue(xml.contains("Madeira de mogno"));
    }

    @Test
    void salvarCriaAsPastasQueFaltam() {
        Path aninhado = pasta.resolve("a").resolve("b").resolve("planta.xml");

        persistencia.salvar(original, aninhado);

        assertTrue(Files.exists(aninhado));
    }

    @Test
    void salvarNaoDeixaArquivoTemporarioParaTras() throws IOException {
        persistencia.salvar(original, arquivo);

        try (var arquivos = Files.list(pasta)) {
            assertEquals(1, arquivos.count());
        }
    }

    @Test
    void salvarDeNovoAtualizaOConteudoDoArquivo() {
        persistencia.salvar(original, arquivo);
        original.comprarMateriaPrima(1, 10.0);

        persistencia.salvar(original, arquivo);

        GerenciadorProducao carregado = persistencia.carregar(arquivo);
        assertEquals(110.0, carregado.getMateriasPrimas().get(0).getQuantidade(), DELTA);
        assertEquals(original.getBudget(), carregado.getBudget(), DELTA);
    }

    @Test
    void salvarSemPlantaOuSemArquivoLancaExcecao() {
        assertThrows(PersistenciaException.class, () -> persistencia.salvar(null, arquivo));
        assertThrows(PersistenciaException.class, () -> persistencia.salvar(original, null));
    }

    @Test
    void falhaDeEscritaLancaExcecaoDePersistencia() throws IOException {
        Path arquivoComum = pasta.resolve("arquivo.txt");
        Files.writeString(arquivoComum, "não sou uma pasta");

        // o "diretório pai" do destino é um arquivo comum: não dá para criar a pasta
        assertThrows(PersistenciaException.class,
                () -> persistencia.salvar(original, arquivoComum.resolve("planta.xml")));
    }

    // salvar + carregar: o estado volta igual

    @Test
    void carregarRecuperaConfiguracaoDaPlanta() {
        persistencia.salvar(original, arquivo);

        GerenciadorProducao carregado = persistencia.carregar(arquivo);

        assertEquals(Cenario.APOCALIPTICO, carregado.getCenario());
        assertEquals(1234.5, carregado.getBudget(), DELTA);
        assertEquals("Linha principal", carregado.getLinha().getNome());
    }

    @Test
    void carregarRecuperaOEstoqueDeMateriasPrimas() {
        persistencia.salvar(original, arquivo);

        List<MateriaPrima> carregadas = persistencia.carregar(arquivo).getMateriasPrimas();

        assertEquals(original.getMateriasPrimas().size(), carregadas.size());
        for (int i = 0; i < carregadas.size(); i++) {
            MateriaPrima esperada = original.getMateriasPrimas().get(i);
            MateriaPrima obtida = carregadas.get(i);
            assertEquals(esperada.getId(), obtida.getId());
            assertEquals(esperada.getNome(), obtida.getNome());
            assertEquals(esperada.getQuantidade(), obtida.getQuantidade(), DELTA);
            assertEquals(esperada.getUnidade(), obtida.getUnidade());
            assertEquals(esperada.getCustoPorUnidade(), obtida.getCustoPorUnidade(), DELTA);
        }
        assertEquals(74.5, carregadas.get(1).getQuantidade(), DELTA);
    }

    @Test
    void carregarRecuperaMaquinasNaMesmaOrdemEComOMesmoTipo() {
        persistencia.salvar(original, arquivo);

        List<Maquina> carregadas = persistencia.carregar(arquivo).getLinha().getMaquinas();

        assertEquals(3, carregadas.size());
        for (int i = 0; i < carregadas.size(); i++) {
            Maquina esperada = original.getLinha().getMaquinas().get(i);
            Maquina obtida = carregadas.get(i);
            assertEquals(esperada.getClass(), obtida.getClass());
            assertEquals(esperada.getTipo(), obtida.getTipo());
            assertEquals(esperada.getNome(), obtida.getNome());
            assertEquals(esperada.estaLigada(), obtida.estaLigada());
            assertEquals(esperada.getCapacidadeMaxima(), obtida.getCapacidadeMaxima(), DELTA);
            assertEquals(esperada.getCustoOperacao(), obtida.getCustoOperacao(), DELTA);
            assertEquals(esperada.getProbabilidadeFalhaBase(), obtida.getProbabilidadeFalhaBase(), DELTA);
        }
        assertFalse(carregadas.get(2).estaLigada());
    }

    @Test
    void carregarRecuperaEstadoETodosOsSubcomponentesDasMaquinas() {
        persistencia.salvar(original, arquivo);

        List<Maquina> carregadas = persistencia.carregar(arquivo).getLinha().getMaquinas();

        for (int i = 0; i < carregadas.size(); i++) {
            Maquina esperada = original.getLinha().getMaquinas().get(i);
            Maquina obtida = carregadas.get(i);
            assertEquals(esperada.getSaude(), obtida.getSaude(), DELTA);
            assertEquals(esperada.getHistoricoFalhas(), obtida.getHistoricoFalhas());
            assertEquals(esperada.lerTemperaturaMotor(), obtida.lerTemperaturaMotor(), DELTA);
            assertEquals(esperada.lerDesgasteMotor(), obtida.lerDesgasteMotor(), DELTA);
            assertEquals(esperada.lerCalibracaoSensor(), obtida.lerCalibracaoSensor(), DELTA);
            assertEquals(esperada.getQuantidadeFerramentas(), obtida.getQuantidadeFerramentas());
            for (int f = 0; f < esperada.getQuantidadeFerramentas(); f++) {
                assertEquals(esperada.consultarFerramentas().get(f).nome(), obtida.consultarFerramentas().get(f).nome());
                assertEquals(esperada.consultarFerramentas().get(f).desgaste(),
                        obtida.consultarFerramentas().get(f).desgaste(), DELTA);
            }
        }
        assertEquals(7, carregadas.get(2).getHistoricoFalhas());
        assertEquals(2.0, carregadas.get(2).consultarFerramentas().get(1).desgaste(), DELTA);
    }

    @Test
    void carregarRecuperaDemandasComQuantidadeEStatus() {
        persistencia.salvar(original, arquivo);

        List<Demanda> carregadas = persistencia.carregar(arquivo).getDemandas();

        assertEquals(3, carregadas.size());
        for (int i = 0; i < carregadas.size(); i++) {
            Demanda esperada = original.getDemandas().get(i);
            Demanda obtida = carregadas.get(i);
            assertEquals(esperada.getLinha(), obtida.getLinha());
            assertEquals(esperada.getInstrumento(), obtida.getInstrumento());
            assertEquals(esperada.getQuantidadeProdutos(), obtida.getQuantidadeProdutos());
            assertEquals(esperada.getStatus(), obtida.getStatus());
        }
        assertEquals(StatusDemanda.PENDENTE, carregadas.get(0).getStatus());
        assertEquals(StatusDemanda.CONCLUIDA, carregadas.get(1).getStatus());
        assertEquals(StatusDemanda.CANCELADA, carregadas.get(2).getStatus());
    }

    @Test
    void plantaSemDemandasNemEstoqueVoltaVazia() {
        GerenciadorProducao vazia = new GerenciadorProducao(
                Fixtures.linhaDeterministica(), new ArrayList<>(), Cenario.IDEAL);
        persistencia.salvar(vazia, arquivo);

        GerenciadorProducao carregado = persistencia.carregar(arquivo);

        assertTrue(carregado.getDemandas().isEmpty());
        assertTrue(carregado.getMateriasPrimas().isEmpty());
        assertEquals(5000.0, carregado.getBudget(), DELTA);
    }

    @Test
    void objetoCarregadoEIndependenteDoOriginal() {
        persistencia.salvar(original, arquivo);
        GerenciadorProducao carregado = persistencia.carregar(arquivo);

        carregado.getMateriasPrimas().get(0).consumir(10.0);
        carregado.getLinha().getMaquina(1).desligar();

        assertNotSame(original, carregado);
        assertEquals(100.0, original.getMateriasPrimas().get(0).getQuantidade(), DELTA);
        assertTrue(original.getLinha().getMaquina(1).estaLigada());
    }

    @Test
    void plantaCarregadaRespeitaAsRegrasDeNegocioEConsegueProduzir() {
        GerenciadorProducao deterministico = Fixtures.gerenciador();
        deterministico.registrarDemanda(new Demanda(LinhaProduto.CLASSICA, 2, Instrumento.VIOLAO));
        persistencia.salvar(deterministico, arquivo);

        GerenciadorProducao carregado = persistencia.carregar(arquivo);
        carregado.setEstrategia(new EstrategiaOrdemDePedido());
        carregado.fabricarDemanda(1);

        assertEquals(StatusDemanda.CONCLUIDA, carregado.getDemandas().get(0).getStatus());
        assertEquals(92.0, carregado.getMateriasPrimas().get(0).getQuantidade(), DELTA);
        assertEquals(5000.0 - 70.0, carregado.getBudget(), DELTA);
    }

    // carregar: situações de erro

    @Test
    void carregarArquivoInexistenteLancaExcecaoCompreensivel() {
        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> persistencia.carregar(pasta.resolve("nao-existe.xml")));

        assertTrue(erro.getMessage().contains("não encontrado"));
    }

    @Test
    void carregarArquivoNuloLancaExcecao() {
        assertThrows(PersistenciaException.class, () -> persistencia.carregar(null));
    }

    @Test
    void carregarArquivoVazioLancaExcecao() throws IOException {
        Files.createFile(arquivo);

        PersistenciaException erro = assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));

        assertTrue(erro.getMessage().contains("vazio"));
    }

    @Test
    void carregarConteudoQueNaoEXmlLancaExcecao() throws IOException {
        escreverArquivo("isto não é um xml");

        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
    }

    @Test
    void carregarXmlMalFormadoLancaExcecao() throws IOException {
        persistencia.salvar(original, arquivo);
        String xml = lerArquivo();
        escreverArquivo(xml.substring(0, xml.length() / 2));

        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
    }

    @Test
    void carregarXmlComRaizDiferenteLancaExcecao() throws IOException {
        escreverArquivo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><fabrica></fabrica>");

        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
    }

    @Test
    void carregarXmlComDoctypeEhRejeitadoPorSeguranca() throws IOException {
        escreverArquivo("<?xml version=\"1.0\"?><!DOCTYPE planta [<!ENTITY x \"y\">]><planta></planta>");

        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
    }

    @Test
    void carregarPastaNoLugarDeArquivoLancaExcecao() {
        assertThrows(PersistenciaException.class, () -> persistencia.carregar(pasta));
    }

    @Test
    void carregarSemAConfiguracaoLancaExcecao() throws IOException {
        persistencia.salvar(original, arquivo);
        escreverArquivo(Pattern.compile("<configuracao>.*</configuracao>", Pattern.DOTALL)
                .matcher(lerArquivo()).replaceFirst(""));

        PersistenciaException erro = assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));

        assertTrue(erro.getMessage().contains("configuracao"));
    }

    @Test
    void carregarNumeroInvalidoEmCampoNumericoLancaExcecao() throws IOException {
        carregarComTagAlterada("budget", "muito dinheiro");
    }

    // carregar: dados que violam as regras do domínio

    @Test
    void carregarOrcamentoNegativoLancaExcecao() throws IOException {
        PersistenciaException erro = carregarComTagAlterada("budget", "-5.0");

        assertTrue(erro.getMessage().toLowerCase().contains("orçamento"));
    }

    @Test
    void carregarCenarioDesconhecidoLancaExcecao() throws IOException {
        carregarComTagAlterada("cenario", "APOCALIPSE_ZUMBI");
    }

    @Test
    void carregarTipoDeMaquinaDesconhecidoLancaExcecao() throws IOException {
        carregarComTagAlterada("tipo", "Maquina Voadora");
    }

    @Test
    void carregarSaudeDeMaquinaForaDaFaixaLancaExcecao() throws IOException {
        carregarComTagAlterada("saude", "150.0");
    }

    @Test
    void carregarCapacidadeDeMaquinaNaoPositivaLancaExcecao() throws IOException {
        carregarComTagAlterada("capacidadeMaxima", "0.0");
    }

    @Test
    void carregarProbabilidadeDeFalhaForaDaFaixaLancaExcecao() throws IOException {
        carregarComTagAlterada("probabilidadeFalha", "2.0");
    }

    @Test
    void carregarTemperaturaDoMotorForaDaFaixaLancaExcecao() throws IOException {
        carregarComTagAlterada("temperatura", "500.0");
    }

    @Test
    void carregarQuantidadeDeMateriaPrimaNegativaLancaExcecao() throws IOException {
        carregarComTagAlterada("quantidade", "-1.0");
    }

    @Test
    void carregarStatusDeDemandaDesconhecidoLancaExcecao() throws IOException {
        carregarComTagAlterada("status", "TALVEZ");
    }

    @Test
    void carregarInstrumentoDesconhecidoLancaExcecao() throws IOException {
        carregarComTagAlterada("instrumento", "TROMBONE");
    }

    @Test
    void carregarQuantidadeDeDemandaNegativaLancaExcecaoDeDominio() throws IOException {
        PersistenciaException erro = carregarComTagAlterada("quantidadeProdutos", "-3");

        assertTrue(erro.getMessage().contains("regras do domínio"));
    }

    @Test
    void carregarMateriaPrimaComIdRepetidoLancaExcecao() throws IOException {
        persistencia.salvar(original, arquivo);
        escreverArquivo(lerArquivo().replace("<id>MP02</id>", "<id>MP01</id>"));

        PersistenciaException erro = assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));

        assertTrue(erro.getMessage().contains("repetida"));
    }

    @Test
    void carregarDemandaRepetidaLancaExcecao() throws IOException {
        persistencia.salvar(original, arquivo);
        String xml = lerArquivo();
        Matcher primeira = Pattern.compile("<demanda>.*?</demanda>", Pattern.DOTALL).matcher(xml);
        assertTrue(primeira.find());
        escreverArquivo(xml.replace("</demandas>", primeira.group() + "</demandas>"));

        PersistenciaException erro = assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));

        assertTrue(erro.getMessage().contains("repetida"));
    }

    @Test
    void carregarMaquinaComFerramentasIncompativeisLancaExcecao() throws IOException {
        persistencia.salvar(original, arquivo);
        Matcher ferramenta = Pattern.compile("<ferramenta>.*?</ferramenta>", Pattern.DOTALL).matcher(lerArquivo());
        assertTrue(ferramenta.find());
        escreverArquivo(lerArquivo().replaceFirst(Pattern.quote(ferramenta.group()), ""));

        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));
    }

    // consistência diante de falhas

    @Test
    void carregamentoComFalhaNaoAlteraAPlantaEmMemoria() throws IOException {
        double budgetAntes = original.getBudget();
        int demandasAntes = original.getDemandas().size();
        persistencia.salvar(original, arquivo);
        escreverArquivo("<planta><corrompido>");

        assertThrows(PersistenciaException.class, () -> persistencia.carregar(arquivo));

        // quem chama só troca de planta quando carregar() devolve um objeto: nada mudou aqui
        assertEquals(budgetAntes, original.getBudget(), DELTA);
        assertEquals(demandasAntes, original.getDemandas().size());
        assertEquals(74.5, original.getMateriasPrimas().get(1).getQuantidade(), DELTA);
    }

    @Test
    void falhaAoSalvarNaoDestroiOArquivoAnterior() throws IOException {
        persistencia.salvar(original, arquivo);
        String conteudoAnterior = lerArquivo();

        // salvar(null, ...) falha antes de escrever qualquer coisa
        assertThrows(PersistenciaException.class, () -> persistencia.salvar(null, arquivo));

        assertEquals(conteudoAnterior, lerArquivo());
        assertEquals(1234.5, persistencia.carregar(arquivo).getBudget(), DELTA);
    }
}
