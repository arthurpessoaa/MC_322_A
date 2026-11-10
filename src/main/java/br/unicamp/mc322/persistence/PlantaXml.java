package br.unicamp.mc322.persistence;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Representação em XML (contrato de dados) do estado da planta, usada pelo Jakarta XML Binding.
 *
 * @author Arthur Lopes Pessoa
 */
@XmlRootElement(name = "planta")
@XmlAccessorType(XmlAccessType.FIELD)
public class PlantaXml {

    @XmlElement(name = "configuracao")
    public ConfiguracaoXml configuracao;

    @XmlElementWrapper(name = "materiasPrimas")
    @XmlElement(name = "materiaPrima")
    public List<MateriaPrimaXml> materiasPrimas;

    @XmlElementWrapper(name = "maquinas")
    @XmlElement(name = "maquina")
    public List<MaquinaXml> maquinas;

    @XmlElementWrapper(name = "demandas")
    @XmlElement(name = "demanda")
    public List<DemandaXml> demandas;

    /** Construtor sem argumentos exigido pelo JAXB para recriar o objeto. */
    public PlantaXml() { }

    /** Cenário, orçamento e nome da linha de produção. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ConfiguracaoXml {
        public String cenario;
        public Double budget;
        public String nomeLinha;

        public ConfiguracaoXml() { }
    }

    /** Uma matéria-prima do estoque. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MateriaPrimaXml {
        public String id;
        public String nome;
        public Double quantidade;
        public String unidade;
        public Double custoPorUnidade;

        public MateriaPrimaXml() { }
    }

    /** Uma máquina, com os componentes (motor, sensor e ferramentas) que a compõem. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MaquinaXml {
        public String tipo;
        public String nome;
        public Boolean ligada;
        public Double saude;
        public Double capacidadeMaxima;
        public Double custoOperacao;
        public Integer historicoFalhas;
        public Double probabilidadeFalha;
        public MotorXml motor;
        public SensorXml sensor;

        @XmlElementWrapper(name = "ferramentas")
        @XmlElement(name = "ferramenta")
        public List<FerramentaXml> ferramentas;

        public MaquinaXml() { }
    }

    /** Estado do motor de uma máquina. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MotorXml {
        public Double temperatura;
        public Double desgaste;

        public MotorXml() { }
    }

    /** Estado do sensor de uma máquina. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SensorXml {
        public Double calibracao;

        public SensorXml() { }
    }

    /** Estado de uma ferramenta de uma máquina. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FerramentaXml {
        public String nome;
        public Double desgaste;

        public FerramentaXml() { }
    }

    /** Uma demanda de produção com seu status. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DemandaXml {
        public String linha;
        public String instrumento;
        public Integer quantidadeProdutos;
        public String status;

        public DemandaXml() { }
    }
}