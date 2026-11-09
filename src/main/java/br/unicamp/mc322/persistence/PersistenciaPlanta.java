package br.unicamp.mc322.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import br.unicamp.mc322.exception.FabricaException;
import br.unicamp.mc322.exception.PersistenciaException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import br.unicamp.mc322.model.maquinas.LinhaDeProducao;
import br.unicamp.mc322.model.maquinas.Maquina;
import br.unicamp.mc322.model.maquinas.MaquinaEmbalagem;
import br.unicamp.mc322.model.maquinas.MaquinaInspecaoSonora;
import br.unicamp.mc322.model.maquinas.MaquinaMontagem;
import br.unicamp.mc322.persistence.PlantaXml.ConfiguracaoXml;
import br.unicamp.mc322.persistence.PlantaXml.DemandaXml;
import br.unicamp.mc322.persistence.PlantaXml.FerramentaXml;
import br.unicamp.mc322.persistence.PlantaXml.MaquinaXml;
import br.unicamp.mc322.persistence.PlantaXml.MateriaPrimaXml;
import br.unicamp.mc322.persistence.PlantaXml.MotorXml;
import br.unicamp.mc322.persistence.PlantaXml.SensorXml;
import br.unicamp.mc322.model.producao.Cenario;
import br.unicamp.mc322.model.producao.Demanda;
import br.unicamp.mc322.service.GerenciadorProducao;
import br.unicamp.mc322.model.producao.MateriaPrima;
import br.unicamp.mc322.model.producao.StatusDemanda;
import br.unicamp.mc322.model.produtos.Instrumento;
import br.unicamp.mc322.model.produtos.LinhaProduto;

/**
 * Responsável pela escrita e leitura do estado da planta industrial em XML, usando
 * Jakarta XML Binding (marshalling e unmarshalling).
 * <p>
 * As entidades de domínio não conhecem o XML: o estado da fábrica é copiado para as classes de
 * {@link PlantaXml} (marshalling) e, na leitura, o XML é convertido de volta em objetos de domínio
 * somente depois de validado (unmarshalling). Persiste configuração (cenário, orçamento e nome da
 * linha), estoque, máquinas com seus componentes e demandas com seus status. Uma falha de
 * carregamento nunca altera a planta que estava em uso.
 * </p>
 *
 * @author Arthur Lopes Pessoa
 */
public class PersistenciaPlanta {

    /**
     * Salva o estado da fábrica em um arquivo XML. A escrita é feita em um arquivo temporário
     * e só depois substitui o arquivo final, preservando os dados anteriores se algo falhar.
     *
     * @param gerenciador fábrica a ser salva
     * @param arquivo caminho do XML de destino
     * @throws PersistenciaException se não for possível escrever o arquivo
     */
    public void salvar(GerenciadorProducao gerenciador, Path arquivo) {
        if (gerenciador == null || arquivo == null) {
            throw new PersistenciaException("Não há planta ou arquivo de destino para salvar.");
        }

        PlantaXml planta = paraXml(gerenciador);

        Path absoluto = arquivo.toAbsolutePath();
        Path temporario = absoluto.resolveSibling(absoluto.getFileName() + ".tmp");

        try {
            Files.createDirectories(absoluto.getParent());

            Marshaller marshaller = JAXBContext.newInstance(PlantaXml.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(planta, temporario.toFile());

            Files.move(temporario, absoluto, StandardCopyOption.REPLACE_EXISTING);
        } catch (JAXBException | IOException e) {
            try {
                Files.deleteIfExists(temporario);
            } catch (IOException ignorada) {
                // nada a fazer: o arquivo temporário é apenas lixo
            }
            throw new PersistenciaException("Não foi possível salvar o arquivo: " + absoluto, e);
        }
    }

    /**
     * Lê o XML, valida os dados e reconstrói a fábrica (estoque, máquinas e componentes,
     * orçamento, cenário e demandas). Nada é devolvido se qualquer dado for inválido.
     *
     * @param arquivo caminho do XML
     * @return gerenciador recuperado
     * @throws PersistenciaException se o arquivo não existir, estiver vazio, corrompido,
     *                               não puder ser lido ou tiver dados incompatíveis com o domínio
     */
    public GerenciadorProducao carregar(Path arquivo) {
        if (arquivo == null || !Files.exists(arquivo)) {
            throw new PersistenciaException("Arquivo de persistência não encontrado.");
        }

        try {
            if (Files.size(arquivo) == 0) {
                throw new PersistenciaException("O arquivo de persistência está vazio.");
            }

            return deXml(lerXml(arquivo));

        } catch (PersistenciaException e) {
            throw e;
        } catch (FabricaException e) {
            throw new PersistenciaException(
                    "Dados incompatíveis com as regras do domínio: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PersistenciaException("Falha ao ler o arquivo de persistência.", e);
        }
    }

    /** Converte o arquivo XML em {@link PlantaXml}, recusando DOCTYPE e qualquer erro de formato. */
    private PlantaXml lerXml(Path arquivo) throws IOException {
        try (InputStream entrada = Files.newInputStream(arquivo)) {
            SAXParserFactory fabrica = SAXParserFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            XMLReader leitor = fabrica.newSAXParser().getXMLReader();

            Unmarshaller unmarshaller = JAXBContext.newInstance(PlantaXml.class).createUnmarshaller();
            // Por padrão o JAXB ignora valores inválidos (ex.: texto em campo numérico);
            // aqui qualquer problema interrompe a leitura.
            unmarshaller.setEventHandler(evento -> false);

            Object lido = unmarshaller.unmarshal(new SAXSource(leitor, new InputSource(entrada)));
            if (!(lido instanceof PlantaXml planta)) {
                throw new PersistenciaException("Estrutura XML inválida: raiz deve ser <planta>.");
            }
            return planta;
        } catch (JAXBException | SAXException e) {
            throw new PersistenciaException("Arquivo XML corrompido ou com formato inválido.", e);
        } catch (ParserConfigurationException e) {
            throw new PersistenciaException("Erro ao configurar a leitura do XML.", e);
        }
    }

    /** Copia o estado da fábrica para a representação XML. */
    private PlantaXml paraXml(GerenciadorProducao gerenciador) {
        PlantaXml planta = new PlantaXml();

        planta.configuracao = new ConfiguracaoXml();
        planta.configuracao.cenario = gerenciador.getCenario().name();
        planta.configuracao.budget = gerenciador.getBudget();
        planta.configuracao.nomeLinha = gerenciador.getLinha().getNome();

        planta.materiasPrimas = new ArrayList<>();
        for (MateriaPrima materia : gerenciador.getMateriasPrimas()) {
            MateriaPrimaXml xml = new MateriaPrimaXml();
            xml.id = materia.getId();
            xml.nome = materia.getNome();
            xml.quantidade = materia.getQuantidade();
            xml.unidade = materia.getUnidade();
            xml.custoPorUnidade = materia.getCustoPorUnidade();
            planta.materiasPrimas.add(xml);
        }

        planta.maquinas = new ArrayList<>();
        for (Maquina maquina : gerenciador.getLinha().getMaquinas()) {
            MaquinaXml xml = new MaquinaXml();
            xml.tipo = maquina.getTipo();
            xml.nome = maquina.getNome();
            xml.ligada = maquina.estaLigada();
            xml.saude = maquina.getSaude();
            xml.capacidadeMaxima = maquina.getCapacidadeMaxima();
            xml.custoOperacao = maquina.getCustoOperacao();
            xml.historicoFalhas = maquina.getHistoricoFalhas();
            xml.probabilidadeFalha = maquina.getProbabilidadeFalhaBase();

            xml.motor = new MotorXml();
            xml.motor.temperatura = maquina.lerTemperaturaMotor();
            xml.motor.desgaste = maquina.lerDesgasteMotor();

            xml.sensor = new SensorXml();
            xml.sensor.calibracao = maquina.lerCalibracaoSensor();

            xml.ferramentas = new ArrayList<>();
            for (Maquina.EstadoFerramenta estado : maquina.consultarFerramentas()) {
                FerramentaXml ferramenta = new FerramentaXml();
                ferramenta.nome = estado.nome();
                ferramenta.desgaste = estado.desgaste();
                xml.ferramentas.add(ferramenta);
            }

            planta.maquinas.add(xml);
        }

        planta.demandas = new ArrayList<>();
        for (Demanda demanda : gerenciador.getDemandas()) {
            DemandaXml xml = new DemandaXml();
            xml.linha = demanda.getLinha().name();
            xml.instrumento = demanda.getInstrumento().name();
            xml.quantidadeProdutos = demanda.getQuantidadeProdutos();
            xml.status = demanda.getStatus().name();
            planta.demandas.add(xml);
        }

        return planta;
    }

    /** Valida a representação XML e reconstrói a fábrica. */
    private GerenciadorProducao deXml(PlantaXml planta) {
        ConfiguracaoXml configuracao = exigir(planta.configuracao, "configuracao");
        Cenario cenario = lerEnum(Cenario.class, configuracao.cenario, "cenario");
        double budget = exigir(configuracao.budget, "budget");
        if (!Double.isFinite(budget) || budget < 0) {
            throw new PersistenciaException("Orçamento inválido no arquivo.");
        }
        String nomeLinha = exigirTexto(configuracao.nomeLinha, "nomeLinha");

        ArrayList<MateriaPrima> materias = lerMateriasPrimas(vazioSeNulo(planta.materiasPrimas));
        LinhaDeProducao linha = lerLinha(vazioSeNulo(planta.maquinas), nomeLinha);
        List<Demanda> demandas = lerDemandas(vazioSeNulo(planta.demandas));

        GerenciadorProducao gerenciador = new GerenciadorProducao(linha, materias, cenario);
        gerenciador.restaurarEstado(budget, demandas);
        return gerenciador;
    }

    /** Converte o estoque, rejeitando ids repetidos e valores negativos. */
    private ArrayList<MateriaPrima> lerMateriasPrimas(List<MateriaPrimaXml> lista) {
        ArrayList<MateriaPrima> materias = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();

        for (MateriaPrimaXml xml : lista) {
            String id = exigirTexto(xml.id, "id");
            String nome = exigirTexto(xml.nome, "nome");
            String unidade = exigirTexto(xml.unidade, "unidade");
            double quantidade = lerNumero(xml.quantidade, "quantidade");
            double custo = lerNumero(xml.custoPorUnidade, "custoPorUnidade");

            if (quantidade < 0 || custo < 0) {
                throw new PersistenciaException("Quantidade ou custo inválido: " + id);
            }
            if (!idsVistos.add(id)) {
                throw new PersistenciaException("Matéria-prima repetida: " + id);
            }

            materias.add(new MateriaPrima(id, nome, quantidade, unidade, custo));
        }

        return materias;
    }

    /** Converte as máquinas e seus componentes, remontando a linha de produção. */
    private LinhaDeProducao lerLinha(List<MaquinaXml> lista, String nomeLinha) {
        LinhaDeProducao linha = new LinhaDeProducao(nomeLinha);

        for (MaquinaXml xml : lista) {
            String tipo = exigirTexto(xml.tipo, "tipo");
            String nome = exigirTexto(xml.nome, "nome");
            boolean ligada = exigir(xml.ligada, "ligada");
            double capacidade = lerNumero(xml.capacidadeMaxima, "capacidadeMaxima");
            double probabilidade = lerNumero(xml.probabilidadeFalha, "probabilidadeFalha");
            double custo = lerNumero(xml.custoOperacao, "custoOperacao");

            if (capacidade <= 0 || probabilidade < 0 || probabilidade > 1 || custo < 0) {
                throw new PersistenciaException("Configuração inválida da máquina: " + nome);
            }

            Maquina maquina;
            switch (tipo) {
                case "Maquina de Montagem":
                    maquina = new MaquinaMontagem(nome, ligada, capacidade, probabilidade, custo);
                    break;
                case "Maquina de Embalagem":
                    maquina = new MaquinaEmbalagem(nome, ligada, capacidade, probabilidade, custo);
                    break;
                case "Máquina de Inspeção Sonora":
                    maquina = new MaquinaInspecaoSonora(nome, ligada, capacidade, probabilidade, custo);
                    break;
                default:
                    throw new PersistenciaException("Tipo de máquina inválido: " + tipo);
            }

            MotorXml motor = exigir(xml.motor, "motor");
            SensorXml sensor = exigir(xml.sensor, "sensor");

            List<Maquina.EstadoFerramenta> estadosFerramentas = new ArrayList<>();
            for (FerramentaXml ferramenta : vazioSeNulo(xml.ferramentas)) {
                estadosFerramentas.add(new Maquina.EstadoFerramenta(
                        exigirTexto(ferramenta.nome, "nome da ferramenta"),
                        lerNumero(ferramenta.desgaste, "desgaste da ferramenta")));
            }

            // Valida e restaura saúde, falhas, motor, sensor e ferramentas (composição)
            maquina.restaurarEstado(
                    lerNumero(xml.saude, "saude"),
                    exigir(xml.historicoFalhas, "historicoFalhas"),
                    lerNumero(motor.temperatura, "temperatura"),
                    lerNumero(motor.desgaste, "desgaste do motor"),
                    lerNumero(sensor.calibracao, "calibracao"),
                    estadosFerramentas);

            linha.adicionarMaquina(maquina);
        }

        return linha;
    }

    /** Converte as demandas e seus status, rejeitando duplicatas de linha e instrumento. */
    private List<Demanda> lerDemandas(List<DemandaXml> lista) {
        List<Demanda> demandas = new ArrayList<>();
        Set<String> chavesVistas = new HashSet<>();

        for (DemandaXml xml : lista) {
            LinhaProduto linha = lerEnum(LinhaProduto.class, xml.linha, "linha");
            Instrumento instrumento = lerEnum(Instrumento.class, xml.instrumento, "instrumento");
            int quantidade = exigir(xml.quantidadeProdutos, "quantidadeProdutos");
            StatusDemanda status = lerEnum(StatusDemanda.class, xml.status, "status");

            if (!chavesVistas.add(linha + "/" + instrumento)) {
                throw new PersistenciaException(
                        "Demanda repetida: " + instrumento.getNome() + " (" + linha.getNome() + ")");
            }

            Demanda demanda = new Demanda(linha, quantidade, instrumento);
            demanda.restaurarStatus(status);
            demandas.add(demanda);
        }

        return demandas;
    }

    /** Lista ausente no XML equivale a lista vazia (ex.: nenhuma demanda registrada). */
    private <T> List<T> vazioSeNulo(List<T> lista) {
        return lista == null ? new ArrayList<>() : lista;
    }

    /** Garante que um elemento obrigatório está presente. */
    private <T> T exigir(T valor, String nome) {
        if (valor == null) {
            throw new PersistenciaException("Tag XML ausente ou vazia: " + nome);
        }
        return valor;
    }

    /** Garante que um texto obrigatório está presente e não está em branco. */
    private String exigirTexto(String valor, String nome) {
        if (valor == null || valor.isBlank()) {
            throw new PersistenciaException("Valor vazio na tag: " + nome);
        }
        return valor.trim();
    }

    /** Garante que um número obrigatório está presente e é finito. */
    private double lerNumero(Double valor, String nome) {
        double numero = exigir(valor, nome);
        if (!Double.isFinite(numero)) {
            throw new PersistenciaException("Número não finito na tag: " + nome);
        }
        return numero;
    }

    /** Converte o nome de uma constante de enum, rejeitando valores desconhecidos. */
    private <E extends Enum<E>> E lerEnum(Class<E> tipo, String valor, String nome) {
        String texto = exigirTexto(valor, nome);
        try {
            return Enum.valueOf(tipo, texto);
        } catch (IllegalArgumentException e) {
            throw new PersistenciaException("Valor desconhecido em <" + nome + ">: " + texto, e);
        }
    }
}