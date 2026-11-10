package br.unicamp.mc322.support;

import java.util.ArrayList;
import java.util.List;

import br.unicamp.mc322.model.maquinas.LinhaDeProducao;
import br.unicamp.mc322.model.maquinas.Maquina;
import br.unicamp.mc322.model.maquinas.MaquinaEmbalagem;
import br.unicamp.mc322.model.maquinas.MaquinaInspecaoSonora;
import br.unicamp.mc322.model.maquinas.MaquinaMontagem;
import br.unicamp.mc322.model.producao.Cenario;
import br.unicamp.mc322.model.producao.MateriaPrima;
import br.unicamp.mc322.service.GerenciadorProducao;
import br.unicamp.mc322.strategy.EstrategiaOrdemDePedido;

/**
 * Montadores de objetos usados pelos testes.
 *
 * <p>Cada chamada devolve objetos novos, para que nenhum teste dependa do estado deixado por
 * outro. As máquinas de montagem e de embalagem criadas aqui têm probabilidade de falha zero e
 * nunca reprovam um produto, então a produção nos testes é determinística.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /** Estoque padrão: mogno, cedro, nylon e alumínio (índices 0 a 3). */
    public static ArrayList<MateriaPrima> materiasPrimas() {
        ArrayList<MateriaPrima> materias = new ArrayList<>();
        materias.add(new MateriaPrima("MP01", "Madeira de mogno", 100.0, "kg", 1.0));
        materias.add(new MateriaPrima("MP02", "Madeira de cedro", 100.0, "kg", 1.0));
        materias.add(new MateriaPrima("MP03", "Cordas de nylon", 200.0, "unidades", 1.0));
        materias.add(new MateriaPrima("MP04", "Alumínio", 50.0, "kg", 0.1));
        return materias;
    }

    /** Linha com montagem (custo 25) e embalagem (custo 10), ambas sem falhas aleatórias. */
    public static LinhaDeProducao linhaDeterministica() {
        LinhaDeProducao linha = new LinhaDeProducao("Linha de teste");
        linha.adicionarMaquina(new MaquinaMontagem("Montagem", true, 30.0, 0.0, 25.0));
        linha.adicionarMaquina(new MaquinaEmbalagem("Embalagem", true, 30.0, 0.0, 10.0));
        return linha;
    }

    /** Gerenciador determinístico no cenário ideal (budget inicial R$ 5000,00). */
    public static GerenciadorProducao gerenciador() {
        return gerenciador(materiasPrimas(), linhaDeterministica());
    }

    public static GerenciadorProducao gerenciador(ArrayList<MateriaPrima> materias, LinhaDeProducao linha) {
        GerenciadorProducao gerenciador = new GerenciadorProducao(linha, materias, Cenario.IDEAL);
        gerenciador.setEstrategia(new EstrategiaOrdemDePedido());
        return gerenciador;
    }

    /** Linha com os três tipos de máquina, usada nos testes de persistência (não é processada). */
    public static LinhaDeProducao linhaCompleta() {
        LinhaDeProducao linha = new LinhaDeProducao("Linha principal");
        linha.adicionarMaquina(new MaquinaMontagem("Montagem", true, 30.0, 0.0, 25.0));
        linha.adicionarMaquina(new MaquinaEmbalagem("Embalagem", true, 28.0, 0.0, 10.0));
        linha.adicionarMaquina(new MaquinaInspecaoSonora("Inspeção", false, 25.0, 0.1, 15.0));
        return linha;
    }

    /** Estado atual das ferramentas da máquina, com o desgaste de cada uma trocado. */
    public static List<Maquina.EstadoFerramenta> ferramentasComDesgaste(Maquina maquina, double... desgastes) {
        List<Maquina.EstadoFerramenta> atuais = maquina.consultarFerramentas();
        List<Maquina.EstadoFerramenta> novas = new ArrayList<>();
        for (int i = 0; i < atuais.size(); i++) {
            novas.add(new Maquina.EstadoFerramenta(atuais.get(i).nome(), desgastes[i]));
        }
        return novas;
    }
}
