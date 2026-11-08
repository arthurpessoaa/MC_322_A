package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import maquinas.Maquina;
import maquinas.MaquinaEmbalagem;
import maquinas.MaquinaInspecaoSonora;
import maquinas.MaquinaMontagem;
import producao.Demanda;
import producao.GerenciadorProducao;
import producao.MateriaPrima;

/** Responsável pela escrita e leitura do estado da planta industrial em XML. */
public class PersistenciaPlanta {

    /** Adiciona um elemento com conteúdo textual a outro elemento XML. */
    private void adicionarElemento(Document documento, Element elementoPrincipal,
                                   String nome, String valor) {
        Element elemento = documento.createElement(nome);
        elemento.setTextContent(valor);
        elementoPrincipal.appendChild(elemento);
    }

    /** Salva o estoque, as máquinas e as demandas da fábrica em um arquivo XML. */
    public void salvar(GerenciadorProducao gerenciador, Path arquivo) throws IOException {
        try {
            Document documento = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            Element raiz = documento.createElement("planta");
            documento.appendChild(raiz);

            Element estoque = documento.createElement("materiasPrimas");
            raiz.appendChild(estoque);

            for (MateriaPrima materia : gerenciador.getMateriasPrimas()) {
                Element elemento = documento.createElement("materiaPrima");
                estoque.appendChild(elemento);
                adicionarElemento(documento, elemento, "id", materia.getId());
                adicionarElemento(documento, elemento, "nome", materia.getNome());
                adicionarElemento(documento, elemento, "quantidade",
                        String.valueOf(materia.getQuantidade()));
                adicionarElemento(documento, elemento, "unidade", materia.getUnidade());
                adicionarElemento(documento, elemento, "custoPorUnidade",
                        String.valueOf(materia.getCustoPorUnidade()));
            }

            Element maquinas = documento.createElement("maquinas");
            raiz.appendChild(maquinas);

            for (Maquina maquina : gerenciador.getLinha().getMaquinas()) {
                Element elementoMaquina = documento.createElement("maquina");
                maquinas.appendChild(elementoMaquina);

                adicionarElemento(documento, elementoMaquina, "tipo", maquina.getTipo());
                adicionarElemento(documento, elementoMaquina, "nome", maquina.getNome());
                adicionarElemento(documento, elementoMaquina, "ligada",
                        String.valueOf(maquina.estaLigada()));
                adicionarElemento(documento, elementoMaquina, "saude",
                        String.valueOf(maquina.getSaude()));
                adicionarElemento(documento, elementoMaquina, "capacidadeMaxima",
                        String.valueOf(maquina.getCapacidadeMaxima()));
                adicionarElemento(documento, elementoMaquina, "custoOperacao",
                        String.valueOf(maquina.getCustoOperacao()));
                adicionarElemento(documento, elementoMaquina, "historicoFalhas",
                        String.valueOf(maquina.getHistoricoFalhas()));
                adicionarElemento(documento, elementoMaquina, "probabilidadeFalha",
                        String.valueOf(maquina.getProbabilidadeFalhaBase()));

                Element motor = documento.createElement("motor");
                elementoMaquina.appendChild(motor);
                adicionarElemento(documento, motor, "temperatura",
                        String.valueOf(maquina.lerTemperaturaMotor()));
                adicionarElemento(documento, motor, "desgaste",
                        String.valueOf(maquina.lerDesgasteMotor()));

                Element sensor = documento.createElement("sensor");
                elementoMaquina.appendChild(sensor);
                adicionarElemento(documento, sensor, "calibracao",
                        String.valueOf(maquina.lerCalibracaoSensor()));

                Element ferramentas = documento.createElement("ferramentas");
                elementoMaquina.appendChild(ferramentas);

                for (Maquina.EstadoFerramenta estado : maquina.consultarFerramentas()) {
                    Element ferramenta = documento.createElement("ferramenta");
                    ferramentas.appendChild(ferramenta);
                    adicionarElemento(documento, ferramenta, "nome", estado.nome());
                    adicionarElemento(documento, ferramenta, "desgaste",
                            String.valueOf(estado.desgaste()));
                }
            }

            Element demandas = documento.createElement("demandas");
            raiz.appendChild(demandas);

            for (Demanda demanda : gerenciador.getDemandas()) {
                Element elementoDemanda = documento.createElement("demanda");
                demandas.appendChild(elementoDemanda);
                adicionarElemento(documento, elementoDemanda, "linha",
                        demanda.getLinha().name());
                adicionarElemento(documento, elementoDemanda, "instrumento",
                        demanda.getInstrumento().name());
                adicionarElemento(documento, elementoDemanda, "quantidadeProdutos",
                        String.valueOf(demanda.getQuantidadeProdutos()));
                adicionarElemento(documento, elementoDemanda, "status",
                        demanda.getStatus().name());
            }

            escreverArquivo(documento, arquivo);
        } catch (ParserConfigurationException e) {
            throw new IOException("Erro ao criar o documento XML.", e);
        }
    }

    /** Grava um documento DOM em XML no caminho especificado. */
    private void escreverArquivo(Document documento, Path arquivo) throws IOException {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(documento),
                    new StreamResult(arquivo.toFile()));
        } catch (TransformerException e) {
            throw new IOException("Erro ao escrever arquivo XML.", e);
        }
    }

    /**
     * Lê o XML e prepara os objetos que serão utilizados na reconstrução da fábrica.
     * Ainda falta restaurar componentes e demandas e construir o gerenciador final.
     *
     * @param arquivo caminho do XML
     * @return gerenciador recuperado, quando a implementação estiver concluída
     * @throws IOException se houver erro de leitura ou dados inválidos
     */
    public GerenciadorProducao carregar(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) {
            throw new IOException("Arquivo de persistência não encontrado.");
        }
        if (Files.size(arquivo) == 0) {
            throw new IOException("O arquivo de persistência está vazio.");
        }

        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);
            DocumentBuilder builder = fabrica.newDocumentBuilder();
            Document documento = builder.parse(arquivo.toFile());
            Element raiz = documento.getDocumentElement();

            if (raiz == null || !raiz.getTagName().equals("planta")) {
                throw new IOException("Estrutura XML inválida.");
            }

            NodeList secoesEstoque = raiz.getElementsByTagName("materiasPrimas");
            if (secoesEstoque.getLength() != 1) {
                throw new IOException("Seção de matérias-primas inválida.");
            }
            Element estoque = (Element) secoesEstoque.item(0);
            NodeList elementosMateria = estoque.getElementsByTagName("materiaPrima");
            List<MateriaPrima> materiasRecuperadas = new ArrayList<>();

            for (int i = 0; i < elementosMateria.getLength(); i++) {
                Element elemento = (Element) elementosMateria.item(i);
                String id = lerTexto(elemento, "id");
                String nome = lerTexto(elemento, "nome");
                String unidade = lerTexto(elemento, "unidade");
                double quantidade = lerDouble(elemento, "quantidade");
                double custo = lerDouble(elemento, "custoPorUnidade");
                if (quantidade < 0 || custo < 0) {
                    throw new IOException("Quantidade ou custo inválido: " + id);
                }
                materiasRecuperadas.add(new MateriaPrima(id, nome, quantidade, unidade, custo));
            }

            NodeList secoesMaquinas = raiz.getElementsByTagName("maquinas");
            if (secoesMaquinas.getLength() != 1) {
                throw new IOException("Seção de máquinas inválida.");
            }
            Element elementoMaquinas = (Element) secoesMaquinas.item(0);
            NodeList listaMaquinas = elementoMaquinas.getElementsByTagName("maquina");
            List<Maquina> maquinasRecuperadas = new ArrayList<>();

            for (int i = 0; i < listaMaquinas.getLength(); i++) {
                Element elementoMaquina = (Element) listaMaquinas.item(i);
                String tipo = lerTexto(elementoMaquina, "tipo");
                String nome = lerTexto(elementoMaquina, "nome");
                String valorLigada = lerTexto(elementoMaquina, "ligada");
                if (!valorLigada.equals("true") && !valorLigada.equals("false")) {
                    throw new IOException("Estado inválido da máquina: " + nome);
                }
                boolean ligada = Boolean.parseBoolean(valorLigada);
                double capacidade = lerDouble(elementoMaquina, "capacidadeMaxima");
                double probabilidade = lerDouble(elementoMaquina, "probabilidadeFalha");
                double custo = lerDouble(elementoMaquina, "custoOperacao");
                if (capacidade <= 0 || probabilidade < 0 || probabilidade > 1 || custo < 0) {
                    throw new IOException("Configuração inválida da máquina: " + nome);
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
                        throw new IOException("Tipo de máquina inválido: " + tipo);
                }

                maquinasRecuperadas.add(maquina);
            }

            throw new UnsupportedOperationException(
                    "Carregamento ainda incompleto: faltam estados dos componentes, demandas e gerenciador.");
        } catch (ParserConfigurationException | SAXException | IllegalArgumentException e) {
            throw new IOException("Erro ao interpretar arquivo XML.", e);
        }
    }

    /** Lê o texto obrigatório de uma tag, rejeitando ausências e repetições. */
    private String lerTexto(Element elementoPrincipal, String nomeTag) throws IOException {
        NodeList elementos = elementoPrincipal.getElementsByTagName(nomeTag);
        if (elementos.getLength() != 1) {
            throw new IOException("Tag XML inválida: " + nomeTag);
        }
        String valor = elementos.item(0).getTextContent();
        if (valor == null || valor.isBlank()) {
            throw new IOException("Valor vazio na tag: " + nomeTag);
        }
        return valor.trim();
    }

    /** Recupera um número double obrigatório e finito. */
    private double lerDouble(Element elemento, String nomeTag) throws IOException {
        try {
            double valor = Double.parseDouble(lerTexto(elemento, nomeTag));
            if (!Double.isFinite(valor)) {
                throw new IOException("Número não finito na tag: " + nomeTag);
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IOException("Número inválido na tag: " + nomeTag, e);
        }
    }
}
