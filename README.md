# Triagem de Suporte — API de classificação e encaminhamento de chamados

Backend desenvolvido em **Java 17 e Spring Boot** para receber chamados de suporte, classificá-los com **IA local (Ollama + Spring AI)**, validar a classificação usando um catálogo de políticas e encaminhar solicitações para listas de atendimento no **Trello**. Os dados e os resultados de encaminhamento são persistidos no **PostgreSQL**.

> **Contexto:** projeto de conclusão de curso, construído para demonstrar desenvolvimento de APIs REST, integração com IA e ferramentas externas, persistência, tratamento de falhas e testes automatizados. Não é um sistema de atendimento pronto para produção.

## Funcionalidades

- Recebimento de chamados por `POST /chamados`, com `ticketId`, `customerId` e `message`.
- Validação de mensagem e identificador do ticket; rejeição de `ticketId` duplicado.
- Persistência do chamado antes da classificação, preservando seu registro quando a IA falha.
- Consulta das políticas de atendimento por uma ferramenta (`PoliticasTool`) disponibilizada ao modelo.
- Classificação em `COBRANCA`, `PROBLEMA_TECNICO` ou `OUTRO`.
- Priorização em `NORMAL` ou `ALTA`, conforme as políticas; respostas ambíguas devem seguir para revisão humana.
- Validação dos valores retornados pela IA, incluindo equipe de destino e necessidade de revisão humana.
- Encaminhamento de chamados classificados automaticamente para listas do Trello, com registro do ID do cartão criado.
- Estados específicos para revisão humana e falha no encaminhamento.
- Consulta de chamados por `ticketId` com `GET /chamados/{ticketId}`.
- Testes com JUnit 5 e Mockito para cenários de sucesso, falhas e revisão humana.

## Tecnologias e decisões técnicas

| Tecnologia | Utilização e justificativa |
|---|---|
| **Java 17** | Linguagem principal, com tipagem estática e suporte a recursos modernos como `switch` expressions. |
| **Spring Boot 4.1.2** | Estrutura da API REST, injeção de dependência e configuração da aplicação. |
| **Spring Web MVC / RestClient** | Exposição dos endpoints HTTP e consumo da API REST do Trello. |
| **Spring Data JPA / Hibernate** | Persistência e consultas dos chamados sem SQL repetitivo para operações básicas. |
| **PostgreSQL** | Banco relacional para armazenar chamados, classificação, estado e identificador externo. |
| **Spring AI 2.0.0** | Integração com o modelo e disponibilização da ferramenta de consulta às políticas. |
| **Ollama + `qwen3.5:4b`** | Execução local de um modelo de linguagem, sem depender de uma API comercial para a classificação. |
| **Trello REST API** | Registro externo do chamado na lista da equipe correspondente. |
| **JUnit 5 + Mockito** | Testes unitários isolados, sem acessar banco, IA ou Trello. |
| **Maven** | Gerenciamento de dependências e execução do projeto. |

A interface `EncaminhamentoService` permite trocar a implementação de encaminhamento (por exemplo, por uma integração com Jira) sem mudar o contrato utilizado pelo serviço principal.

## Arquitetura e fluxo

```text
Sistema externo / Postman
          |
          v
  POST /chamados
          |
          v
  ChamadoController
          |
          v
    ChamadoService
          |
          +--> valida entrada e ticketId
          +--> salva RECEBIDO no PostgreSQL
          |
          v
  ClassificacaoService
          |
          +--> Ollama / Spring AI
          +--> PoliticasTool --> politicas-atendimento.json
          |
          v
  ValidacaoClassificacaoService
          |
          +--> resposta inválida ou falha da IA
          |       `--> REVISAO_PENDENTE
          |
          +--> revisão humana necessária
          |       `--> REVISAO_PENDENTE (sem envio automático)
          |
          `--> classificação aprovada para encaminhamento
                     |
                     v
           EncaminhamentoService
                     |
                     v
            TrelloEncaminhamentoService
                     |
                     +--> sucesso: ID externo + ENCAMINHADO
                     `--> exceção: FALHA_ENCAMINHAMENTO
```

O `ChamadoService` orquestra o fluxo; `ClassificacaoService` consulta a IA; `ValidacaoClassificacaoService` verifica a resposta estruturada; `TrelloEncaminhamentoService` é responsável pelo envio HTTP. A entidade `Chamado` é persistida por `ChamadoRepository`.

### Políticas de atendimento

As regras ficam em [`src/main/resources/policies/politicas-atendimento.json`](src/main/resources/policies/politicas-atendimento.json), em vez de serem definidas exclusivamente no prompt.

| Categoria | Destino previsto |
|---|---|
| `COBRANCA` | `FINANCEIRO` |
| `PROBLEMA_TECNICO` | `TECNICO` |
| `OUTRO` | `REVISAO_HUMANA` |

- **`ALTA`**: indisponibilidade confirmada que impede o cliente de utilizar o produto.
- **`NORMAL`**: informações suficientes para concluir que a regra de alta prioridade não se aplica.
- **Revisão humana**: informações insuficientes, ambiguidade ou proposta inválida da IA; nesse caso, a prioridade pode ficar `null`.

A IA é orientada a consultar a ferramenta de políticas e a ignorar instruções presentes na mensagem do cliente que tentem alterar as regras. A saída da IA passa por validação no backend antes do encaminhamento.

### Estados do chamado

| Status | Significado |
|---|---|
| `RECEBIDO` | Chamado registrado no banco. |
| `EM_ANALISE` | Classificação aceita e processamento em andamento, antes da confirmação externa. |
| `REVISAO_PENDENTE` | Não foi possível tomar uma decisão automática segura, ou a revisão humana é necessária. |
| `ENCAMINHADO` | A criação do cartão foi confirmada e o identificador externo foi registrado. |
| `FALHA_ENCAMINHAMENTO` | Houve exceção durante o envio ou a confirmação do encaminhamento. |

> Uma falha de rede pode ocorrer depois de o Trello criar um cartão. Por esse motivo, o status de falha não prova que não existe cartão externo; uma nova tentativa automática sem checagem pode produzir duplicidades.

## Requisitos para execução local

- **JDK 17**;
- **PostgreSQL** instalado e em execução;
- **Ollama** instalado e em execução, com o modelo `qwen3.5:4b` disponível;
- Acesso a uma **conta de teste do Trello** com permissões para criar cartões;
- Git e Maven (ou o Maven Wrapper incluído no repositório).

### 1. Clonar o repositório

```bash
git clone https://github.com/giaccneto/triagem-suporte.git
cd triagem-suporte
```

### 2. Preparar o banco de dados

Crie um banco chamado `triagem_suporte` no PostgreSQL. A configuração usada durante o desenvolvimento aponta para a porta **5433**, que pode ser diferente da porta da sua instalação (**5432** é comum).

Ajuste `src/main/resources/application.properties` para usar variáveis de ambiente, por exemplo:

```properties
spring.application.name=triagem-suporte
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/triagem_suporte}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

spring.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
spring.ai.ollama.chat.model=qwen3.5:4b

trello.lists.financeiro=${TRELLO_LIST_FINANCEIRO}
trello.lists.tecnico=${TRELLO_LIST_TECNICO}
trello.lists.revisao-humana=${TRELLO_LIST_REVISAO_HUMANA}
```

**Atenção:** o trecho acima é uma **configuração recomendada para aplicar no projeto**, não uma garantia de que a versão atualmente publicada já usa essas variáveis. `ddl-auto=update` é conveniente para desenvolvimento; migrações versionadas são preferíveis em ambientes de produção.

### 3. Configurar as credenciais e IDs das listas

Crie um quadro de testes no Trello e três listas: `FINANCEIRO`, `TECNICO` e `REVISAO_HUMANA`. Identifique os IDs das listas pela API do Trello.

Configure as seguintes **variáveis de ambiente** no IntelliJ (**Run → Edit Configurations → Environment variables**) ou no terminal:

| Variável | Finalidade |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL. |
| `DB_USER` | Usuário do banco. |
| `DB_PASSWORD` | Senha do banco. |
| `TRELLO_API_KEY` | Chave da API do Trello. |
| `TRELLO_TOKEN` | Token de autorização com permissão para criar cartões. |
| `TRELLO_LIST_FINANCEIRO` | ID da lista financeira. |
| `TRELLO_LIST_TECNICO` | ID da lista técnica. |
| `TRELLO_LIST_REVISAO_HUMANA` | ID da lista para revisão humana. |
| `OLLAMA_BASE_URL` | Endereço do Ollama (opcional se usar o padrão). |

As variáveis `DB_URL`, `DB_USER` e os IDs de listas podem utilizar os padrões do exemplo, conforme o ambiente. **Nunca publique chaves, tokens ou senhas no repositório.**

Para que o Spring injete as credenciais no `TrelloEncaminhamentoService`, mantenha os nomes `TRELLO_API_KEY` e `TRELLO_TOKEN`.

### 4. Iniciar o Ollama

```bash
ollama pull qwen3.5:4b
ollama serve
```

Se o serviço do Ollama já estiver rodando, não é necessário iniciar uma segunda instância. Confira os modelos disponíveis com `ollama list`.

### 5. Executar a aplicação

No Windows, na pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS:

```bash
./mvnw spring-boot:run
```

Também é possível executar `TriagemSuporteApplication` pelo IntelliJ. Com a configuração padrão, a API fica disponível em `http://localhost:8080`.

## API REST

### Criar e processar chamado

**`POST /chamados`**

```http
POST http://localhost:8080/chamados
Content-Type: application/json
```

Corpo de exemplo:

```json
{
  "ticketId": "DEMO-COBRANCA-001",
  "customerId": "CLIENTE-DEMO",
  "message": "Preciso da segunda via da minha fatura."
}
```

Exemplo de resposta **obtida em teste manual** (IDs ilustrativos abaixo):

```json
{
  "id": 6,
  "ticketId": "DEMO-COBRANCA-001",
  "customerId": "CLIENTE-DEMO",
  "message": "Preciso da segunda via da minha fatura.",
  "categoria": "COBRANCA",
  "prioridade": "NORMAL",
  "status": "ENCAMINHADO",
  "justificativa": "Solicitação de segunda via de fatura, relacionada a cobranças.",
  "equipeDestino": "FINANCEIRO",
  "idExterno": "ID_DO_CARTAO_CRIADO_NO_TRELLO"
}
```

A criação responde **HTTP 201 Created**, mesmo quando o processamento resulta em `REVISAO_PENDENTE` ou `FALHA_ENCAMINHAMENTO`; consulte o campo `status` para saber o resultado. O tempo de resposta depende também da execução local do modelo.

Outros comportamentos tratados pela API:

| Situação | HTTP | Observação |
|---|---|---|
| Chamado criado | `201` | O status do processamento é informado no corpo. |
| Mensagem ou `ticketId` vazio | `400` | Entrada inválida. |
| `ticketId` repetido | `409` | Conflito com chamado existente. |

### Consultar chamado

**`GET /chamados/{ticketId}`**

Exemplo:

```http
GET http://localhost:8080/chamados/DEMO-COBRANCA-001
```

Responde com o chamado registrado, ou **HTTP 404** caso não exista.

## Integração com Trello

`TrelloEncaminhamentoService` utiliza `RestClient` para enviar `POST https://api.trello.com/1/cards` com o identificador da lista (`idList`) e o nome do cartão. O título inclui o `ticketId` e a categoria.

Uma criação confirmada retorna o ID do cartão, persistido em `idExterno`; então o chamado recebe status `ENCAMINHADO`. Em caso de exceção, o fluxo registra `FALHA_ENCAMINHAMENTO`, sem marcar sucesso.

A integração externa é abstraída pela interface `EncaminhamentoService`, facilitando testes com mocks e futura substituição da plataforma.

## Testes

O projeto utiliza **JUnit 5** e **Mockito**. Na versão local validada durante o desenvolvimento, o conjunto `ChamadoServiceTest` passou em **cinco cenários**:

1. Encaminhamento bem-sucedido: `ENCAMINHADO` e `idExterno` preenchido.
2. Falha simulada do Trello: `FALHA_ENCAMINHAMENTO`.
3. Revisão humana: `REVISAO_PENDENTE` e nenhuma chamada ao encaminhamento.
4. Classificação inválida: revisão humana sem encaminhamento.
5. Falha da IA: revisão humana sem encaminhamento.

Também foram executados testes da validação de categorias, prioridades e equipes, além de teste manual com Postman e criação real de cartão no Trello.

Para executar **somente os testes unitários**, evitando disparar integrações reais:

```powershell
.\mvnw.cmd "-Dtest=ChamadoServiceTest,ValidacaoClassificacaoServiceTest" test
```

> Alguns testes de integração (`TrelloEncaminhamentoServiceTest` e `ClassificacaoServiceTest`) utilizam serviços reais. Ao executá-los, é necessário configurar suas dependências e, no caso do Trello, **um cartão pode ser criado a cada execução**. Evite executar a suíte completa indiscriminadamente.

## Segurança e limites conhecidos

- **Credenciais:** usar variáveis de ambiente; não adicionar tokens ou senhas ao código, aos logs ou ao Git.
- **Segurança do prompt:** a mensagem do cliente é tratada como entrada não confiável, e a IA é orientada a não aceitar instruções que alterem as políticas.
- **Validação posterior à IA:** a resposta estruturada é validada antes do encaminhamento. Entretanto, validar o enum e o destino não comprova, por si só, que uma prioridade `ALTA` corresponde realmente à indisponibilidade do produto.
- **Chamados ambíguos:** devem seguir para `REVISAO_PENDENTE`, com prioridade não definida quando aplicável.
- **Limites do agente:** o número máximo de chamadas à ferramenta precisa ser **verificado na configuração efetivamente aplicada**; uma instrução no prompt para consultar uma vez não constitui um limite técnico garantido.
- **Reenvio e idempotência:** a aplicação não implementa reconciliação automática de cartões após falhas de rede; reenvios sem verificação podem gerar duplicidades no Trello.
- **Webhooks e autenticação de clientes:** o endpoint HTTP pode ser chamado por um sistema externo, mas esta implementação demonstrativa não inclui autenticação da API, verificação de assinatura de webhook nem fila de processamento.
- **Banco e migrações:** `ddl-auto=update` foi usado em desenvolvimento; para produção, usar migrações versionadas e controles de acesso.
- **Dados sensíveis:** não utilizar dados reais de clientes no ambiente demonstrativo; evitar mensagens de cliente em logs.

## Estrutura principal do projeto

```text
src/
├── main/
│   ├── java/com/giaccneto/triagem_suporte/
│   │   ├── controller/ChamadoController.java
│   │   ├── dto/ClassificacaoResposta.java
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/ChamadoRepository.java
│   │   ├── service/
│   │   │   ├── ChamadoService.java
│   │   │   ├── ClassificacaoService.java
│   │   │   ├── ValidacaoClassificacaoService.java
│   │   │   ├── EncaminhamentoService.java
│   │   │   └── TrelloEncaminhamentoService.java
│   │   └── tool/PoliticasTool.java
│   └── resources/
│       ├── application.properties
│       └── policies/politicas-atendimento.json
└── test/java/
    └── ... testes JUnit e Mockito
```

## Demonstração sugerida

1. Mostrar o quadro Trello com as três listas.
2. Apresentar o catálogo de políticas e a organização do projeto no IntelliJ.
3. Enviar um `POST /chamados` pelo Postman com um `ticketId` novo e mensagem de cobrança.
4. Exibir a classificação, o status `ENCAMINHADO` e o `idExterno` na resposta.
5. Abrir o cartão criado na lista `FINANCEIRO`.
6. Consultar o mesmo ticket com `GET /chamados/{ticketId}`.
7. Mostrar os testes unitários aprovados e explicar brevemente os cenários de falha e revisão humana.

## Possíveis evoluções

- Autenticação e autorização para consumidores da API.
- Webhooks assinados, fila de eventos e processamento assíncrono.
- Idempotência e reconciliação com o Trello após falhas de comunicação.
- Limites técnicos de chamadas e tempo de execução do agente, com testes específicos.
- Testes adicionais de prioridade `ALTA`, injeção de prompt e falhas intermitentes.
- Migrações de banco com Flyway ou Liquibase e observabilidade estruturada.
- Integração alternativa com Jira via a interface existente.

---

**Autor:** [giaccneto](https://github.com/giaccneto)  
**Repositório:** https://github.com/giaccneto/triagem-suporte
