# Exercício do módulo 30 — JDBC, novos campos e estoque

Projeto baseado em `mod30/ExemploVendasMod30` do professor Rodrigo Pires:
https://github.com/digaomilleniun/backend-java-ebac/tree/main/mod30/ExemploVendasMod30

Referência do código-base: commit `a841df3ca36f5ba0878c6ca0338e63ccc072b815`.

Este é o exercício do **módulo 30**, com a arquitetura genérica da aula. É um projeto separado do exemplo simplificado do módulo 29. Não substitua os arquivos do módulo 29 por estes.

## Requisitos atendidos

| Parte | Implementação |
|---|---|
| Propriedade nova em cliente | `email`, anotado com `@ColunaTabela`, getters e setters |
| Propriedade nova em produto | `categoria`, anotada com `@ColunaTabela`, getters e setters |
| Persistência | INSERT e UPDATE com os novos campos e parâmetros JDBC |
| Consultas genéricas | `GenericDAO` usa as anotações e reflection em consultar/buscarTodos |
| Consultas de venda | Factories e SELECTs com os novos campos de cliente e produto |
| Testes | Testes anteriores mantidos, suporte de testes atualizado e testes novos de persistência |
| Desafio opcional | Tabela de estoque associada ao produto, entradas, baixa na venda e devolução no cancelamento |

O campo `email` é o contato do cliente. `categoria` classifica o produto, por exemplo, Cozinha ou Utensílios. Os campos aceitam nulo para manter compatibilidade com os testes e cadastros anteriores.

## 1. Executar os testes sem configurar PostgreSQL

Requisitos: **JDK 17 ou superior** e **Maven**. Extraia o ZIP, abra a pasta `modulo30` no VS Code e abra um terminal nela. O `pom.xml` deve estar nessa pasta.

```powershell
mvn clean test
```

Resultado esperado:

```text
Tests run: 51, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Por padrão, os testes usam H2 em memória no modo PostgreSQL. Eles executam SQL pelos DAOs, sem substituir o banco por mocks. Apenas os testes de serviço usam os DAOs de teste em memória. A suíte `AllTests` do professor permanece disponível, mas é excluída da descoberta do Maven para não executar novamente os mesmos testes.

## 2. Validar no PostgreSQL antes da entrega

No pgAdmin, conectado ao servidor, execute isoladamente e fora de uma transação:

```sql
CREATE DATABASE modulo30_testes;
```

No terminal **PowerShell**, dentro da pasta `modulo30`:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/modulo30_testes"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "SUA_SENHA_LOCAL"
mvn clean test -Ppostgres
```

Ajuste a porta e a senha conforme a instalação local. Os testes criam as tabelas pelo `src/main/resources/schema.sql`. O banco deve ter o nome `modulo30_testes`: existe uma verificação para evitar executar a limpeza num banco diferente.

**Use um banco exclusivo e vazio de testes.** Antes de cada teste, são removidos os registros das cinco tabelas desse banco. Não use esse banco para cadastros que deseja guardar.

Não grave sua senha em arquivos que serão enviados ao GitHub.

## 3. Usar o projeto fora dos testes

O projeto contém DAOs, serviços e testes; não possui menu de console. Para criar um banco de uso normal, execute `src/main/resources/schema.sql` no banco e configure `DB_URL`, `DB_USER` e `DB_PASSWORD` antes de chamar os DAOs. Fora dos testes, a URL padrão é `jdbc:postgresql://localhost:5432/modulo30`.

Se você já possui o banco do **exemplo original do módulo 30**, há um script `sql/migracao_mod30.sql` para acrescentar os campos e a tabela de estoque. Ele inicia em zero o estoque dos produtos antigos; informe as quantidades reais por entradas. Esse script não se aplica ao banco simplificado do módulo 29, que usa outra estrutura de cliente.

## 4. Entender o estoque

- `ProdutoDAO.cadastrar(produto, 10)` salva o produto e as 10 unidades iniciais na mesma transação.
- `ProdutoDAO.cadastrar(produto)` mantém o contrato anterior e cria estoque com saldo zero.
- `EstoqueDAO.adicionar(codigo, 5)` registra uma entrada de mais 5 unidades num produto existente.
- `EstoqueDAO.consultar(codigo)` devolve o saldo atual ou nulo se não encontrar.
- `VendaDAO.cadastrar(venda)` persiste a venda, os itens e a baixa de estoque na mesma transação. A baixa ocorre já na venda INICIADA.
- `finalizarVenda` conclui a venda sem dar uma segunda baixa.
- `cancelarVenda` devolve as quantidades persistidas uma única vez. Uma venda CONCLUÍDA não pode ser cancelada por esse fluxo.
- Se um dos produtos não tem saldo, ocorre rollback: nenhuma venda, item ou baixa parcial fica salva.
- O UPDATE com condição `quantidade >= ?` impede saldo negativo mesmo com vendas concorrentes.

Exemplo de cadastro e consulta, para usar dentro de um método Java com tratamento de exceções:

```java
Produto produto = new Produto();
produto.setCodigo("P01");
produto.setNome("Panela");
produto.setDescricao("Panela de ferro");
produto.setValor(new java.math.BigDecimal("50.00"));
produto.setCategoria("Cozinha");
new ProdutoDAO().cadastrar(produto, 10);
Estoque saldo = new EstoqueDAO().consultar("P01");
System.out.println(saldo.getQuantidade()); // 10
```

Use os testes de `EstoqueTest` para acompanhar o fluxo completo. Os métodos de adicionar/remover produtos de uma `Venda` alteram o objeto em memória, como na aula; a persistência dos itens ocorre no cadastro da venda. O projeto não oferece edição persistida dos itens de uma venda já cadastrada.

## 5. O que estudar e comparar com a aula

1. `domain/Cliente.java` e `domain/Produto.java`: novos campos mapeados.
2. `dao/ClienteDAO.java` e `dao/ProdutoDAO.java`: ordem dos parâmetros no INSERT e UPDATE; cópia dos novos campos em `atualiarDados` (nome original preservado).
3. `dao/generic/GenericDAO.java`: interpretação de `@Tabela`, `@TipoChave` e `@ColunaTabela`; conversão por reflection e fechamento de recursos. Os campos novos são String, um tipo já suportado pelo mecanismo.
4. `dao/factory/ClienteFactory.java`, `ProdutoFactory.java` e `VendaDAO.java`: consulta de vendas também carrega email e categoria.
5. `NovosCamposTest`: cadastro, leitura, atualização, listagem e cópia dos campos novos.
6. `EstoqueTest`: transações, saldo insuficiente, rollback e concorrência.

Para depurar só um teste:

```powershell
mvn "-Dtest=NovosCamposTest#atualizarEmail" test
```

## 6. Enviar ao GitHub e à EBAC

Crie um repositório **público** chamado `modulo30`. Você pode deixar a criação inicial de README desmarcada, pois o projeto já contém um.

Pelo site do GitHub, abra **Add file / Adicionar arquivo → Upload files / Carregar arquivos** e envie **a pasta `src` inteira**, `sql`, `pom.xml`, `README.md`, `.gitignore` e `validacao.txt`. Não envie `target` nem apenas o README. Confirme o commit.

Se preferir Git, dentro da pasta `modulo30`, após criar um repositório remoto vazio:

```powershell
git init
git add .
git commit -m "Implementa exercicio do modulo 30 com novos campos e estoque"
git branch -M main
git remote add origin https://github.com/LazarorpDEV/modulo30.git
git push -u origin main
```

Confira no GitHub se `src` e `pom.xml` aparecem junto do README. Depois de validar os testes no PostgreSQL, envie à EBAC o endereço do repositório: `https://github.com/LazarorpDEV/modulo30`.

## Validação realizada

Java 17, Maven 3.9.9 e H2 2.3.232 em modo PostgreSQL: **51 testes, zero falhas, zero erros**, com `mvn clean test`.

A suíte foi executada de verdade e seu resumo está em `validacao.txt`. A execução em servidor PostgreSQL não pôde ser concluída neste ambiente, que não permite iniciar o servidor com usuário separado. Por isso, a validação com `mvn clean test -Ppostgres` deve ser feita na sua instalação; o sucesso no H2 não substitui essa conferência.
