package br.unicamp.mc322.model.componentes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.unicamp.mc322.exception.EntradaInvalidaException;

/** Motor, sensor e ferramenta: as partes que compõem uma máquina. */
class ComponentesTest {

    private static final double DELTA = 0.0001;

    // MotorDeAcionamento

    @Test
    void motorNovoEstaNaTemperaturaAmbienteESemDesgaste() {
        MotorDeAcionamento motor = new MotorDeAcionamento();

        assertEquals(25.0, motor.getTemperatura(), DELTA);
        assertEquals(0.0, motor.getDesgaste(), DELTA);
        assertFalse(motor.estaQuebrado());
        assertFalse(motor.estaSuperaquecido());
    }

    @Test
    void operarEsquentaEDesgastaOMotor() {
        MotorDeAcionamento motor = new MotorDeAcionamento();

        motor.operar(1.0);

        assertEquals(29.0, motor.getTemperatura(), DELTA);
        assertEquals(0.8, motor.getDesgaste(), DELTA);
    }

    @Test
    void resfriarVoltaParaATemperaturaAmbiente() {
        MotorDeAcionamento motor = new MotorDeAcionamento();
        motor.restaurarEstado(90.0, 10.0);

        motor.resfriar();

        assertEquals(25.0, motor.getTemperatura(), DELTA);
    }

    @Test
    void motorComTemperaturaAltaEstaSuperaquecido() {
        MotorDeAcionamento motor = new MotorDeAcionamento();

        motor.restaurarEstado(90.0, 10.0);

        assertTrue(motor.estaSuperaquecido());
    }

    @Test
    void motorComDesgasteMaximoEstaQuebradoEReparoRestauraOEstado() {
        MotorDeAcionamento motor = new MotorDeAcionamento();
        motor.restaurarEstado(60.0, 100.0);
        assertTrue(motor.estaQuebrado());

        motor.reparar();

        assertFalse(motor.estaQuebrado());
        assertEquals(0.0, motor.getDesgaste(), DELTA);
        assertEquals(25.0, motor.getTemperatura(), DELTA);
    }

    @Test
    void operarComMultiplicadorNegativoLancaExcecao() {
        MotorDeAcionamento motor = new MotorDeAcionamento();

        assertThrows(EntradaInvalidaException.class, () -> motor.operar(-1.0));
    }

    @Test
    void restaurarEstadoInvalidoDoMotorLancaExcecaoEMantemOEstado() {
        MotorDeAcionamento motor = new MotorDeAcionamento();

        assertThrows(EntradaInvalidaException.class, () -> motor.restaurarEstado(10.0, 5.0));
        assertThrows(EntradaInvalidaException.class, () -> motor.restaurarEstado(130.0, 5.0));
        assertThrows(EntradaInvalidaException.class, () -> motor.restaurarEstado(50.0, 101.0));
        assertThrows(EntradaInvalidaException.class, () -> motor.restaurarEstado(Double.NaN, 5.0));
        assertEquals(25.0, motor.getTemperatura(), DELTA);
        assertEquals(0.0, motor.getDesgaste(), DELTA);
    }

    // SensorDeVibracao

    @Test
    void sensorNovoTemCalibracaoMaxima() {
        SensorDeVibracao sensor = new SensorDeVibracao();

        assertEquals(100.0, sensor.getCalibracao(), DELTA);
        assertFalse(sensor.estaDescalibrado());
    }

    @Test
    void usarOSensorReduzACalibracao() {
        SensorDeVibracao sensor = new SensorDeVibracao();

        sensor.registrarUso(1.0);

        assertEquals(99.4, sensor.getCalibracao(), DELTA);
    }

    @Test
    void sensorMuitoDesgastadoFicaDescalibradoECalibrarCorrige() {
        SensorDeVibracao sensor = new SensorDeVibracao();
        sensor.restaurarEstado(10.0);
        assertTrue(sensor.estaDescalibrado());

        sensor.calibrar();

        assertFalse(sensor.estaDescalibrado());
        assertEquals(100.0, sensor.getCalibracao(), DELTA);
    }

    @Test
    void calibracaoInvalidaLancaExcecao() {
        SensorDeVibracao sensor = new SensorDeVibracao();

        assertThrows(EntradaInvalidaException.class, () -> sensor.restaurarEstado(-1.0));
        assertThrows(EntradaInvalidaException.class, () -> sensor.restaurarEstado(100.5));
        assertThrows(EntradaInvalidaException.class, () -> sensor.registrarUso(-0.1));
    }

    // FerramentaDeTrabalho

    @Test
    void ferramentaNovaNaoTemDesgaste() {
        FerramentaDeTrabalho ferramenta = new FerramentaDeTrabalho("Garra 1");

        assertEquals("Garra 1", ferramenta.getNome());
        assertEquals(0.0, ferramenta.getDesgaste(), DELTA);
        assertFalse(ferramenta.estaGasta());
        assertFalse(ferramenta.estaQuebrada());
    }

    @Test
    void ferramentaSemNomeLancaExcecao() {
        assertThrows(EntradaInvalidaException.class, () -> new FerramentaDeTrabalho(" "));
        assertThrows(EntradaInvalidaException.class, () -> new FerramentaDeTrabalho(null));
    }

    @Test
    void desgastarSomaODesgastePorUsoVezesOMultiplicador() {
        FerramentaDeTrabalho ferramenta = new FerramentaDeTrabalho("Garra 1");

        ferramenta.desgastar(2.0);

        assertEquals(3.0, ferramenta.getDesgaste(), DELTA);
    }

    @Test
    void ferramentaAcimaDoLimiteEstaGastaEGeraPenalidade() {
        FerramentaDeTrabalho ferramenta = new FerramentaDeTrabalho("Garra 1");
        ferramenta.restaurarEstado(70.0);

        assertTrue(ferramenta.estaGasta());
        assertTrue(ferramenta.getPenalidadeQualidade() > 0.0);
    }

    @Test
    void repararZeraODesgaste() {
        FerramentaDeTrabalho ferramenta = new FerramentaDeTrabalho("Garra 1");
        ferramenta.restaurarEstado(100.0);
        assertTrue(ferramenta.estaQuebrada());

        ferramenta.reparar();

        assertEquals(0.0, ferramenta.getDesgaste(), DELTA);
        assertFalse(ferramenta.estaQuebrada());
    }

    @Test
    void desgasteInvalidoDaFerramentaLancaExcecao() {
        FerramentaDeTrabalho ferramenta = new FerramentaDeTrabalho("Garra 1");

        assertThrows(EntradaInvalidaException.class, () -> ferramenta.restaurarEstado(-1.0));
        assertThrows(EntradaInvalidaException.class, () -> ferramenta.restaurarEstado(100.1));
        assertThrows(EntradaInvalidaException.class, () -> ferramenta.desgastar(-1.0));
        assertEquals(0.0, ferramenta.getDesgaste(), DELTA);
    }
}
