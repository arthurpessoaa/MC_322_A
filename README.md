# Fábrica Harmonia (MC322)

Simulador de uma fábrica de instrumentos musicais para a disciplina MC322 (Programação Orientada a Objetos).

## Como Executar

O projeto utiliza o Gradle Wrapper, não sendo necessário instalar o Gradle globalmente.

No Linux / macOS:
```bash
./gradlew build    # Compila
./gradlew run      # Executa
./gradlew javadoc  # Gera a documentação 
./gradlew test  # Testes
```

No windows
```cmd
gradlew.bat build
gradlew.bat run
gradlew.bat javadoc
./gradlew test
```

## local do relatorio 
build/reports/tests/test/index.html

## Escopo e Limitações

### Dados Persistidos vs. Não Persistidos

O estado é salvo em `dados/planta.xml` (opções 8 e 9 do menu), pela classe `PersistenciaPlanta`. Se o arquivo não existir, a aplicação inicia com a planta padrão. Ao carregar, os dados são validados antes de substituir a planta atual; se algo falhar, a planta em uso é mantida.

**Persistidos**
- Estoque de matérias-primas (obrigatório): é o recurso consumido pela produção.
- Máquinas com motor, sensor e ferramentas (obrigatório): preservam a composição e o desgaste de cada parte.
- Demandas com seus status (obrigatório).
- Orçamento, cenário e nome da linha (opcional): sem eles o estado recuperado não seria equivalente ao salvo.

**Apenas em memória**
- Estratégia de produção ativa: é uma escolha de uso; a aplicação sempre inicia com Ordem de Pedido.
- Produtos já fabricados e contador de lotes: são histórico de saída, e deixá-los de fora mantém o XML simples.

**Limitações:** um único arquivo de salvamento, sem validação por XSD (a validação é feita em código) e sem salvamento automático ao sair.

### Explicação da Suíte de Testes

Os testes usam JUnit 5, rodam sem interação com o terminal e são independentes entre si. Os de persistência usam `@TempDir`, sem tocar em `dados/planta.xml`.

- **Regras de negócio:** `MateriaPrimaTest`, `DemandaTest`, `ComponentesTest`, `MaquinaTest`, `LinhaDeProducaoTest`, `EstrategiasProducaoTest` e `GerenciadorProducaoTest` verificam criação, mudança de estado e as regras da produção.
- **Casos de exceção:** as mesmas classes verificam entradas inválidas e violações de regra (estoque ou orçamento insuficiente, máquina desligada), garantindo que nada muda quando há erro.
- **Persistência:** `PersistenciaPlantaTest` verifica salvar e carregar (os dados voltam iguais), além de arquivo inexistente, vazio, corrompido e com dados fora das regras do domínio.

## Autor

Arthur Lopes Pessoa
