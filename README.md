# Triagem de Suporte

API Java 17 e Spring Boot para receber chamados, classificar com Spring AI e Ollama, validar o resultado, encaminhar ao Trello e registrar o estado no PostgreSQL. Projeto de conclusão de curso voltado a demonstrar APIs REST, persistência e tratamento de falhas.

## Execução local

Use JDK 17, PostgreSQL, Ollama com o modelo `qwen3.5:4b` e um quadro descartável do Trello. Configure as variáveis no terminal ou em **Run → Edit Configurations → Environment variables** no IntelliJ.

| Variável | Valor ou finalidade |
|---|---|
| `DB_URL` | Padrão `jdbc:postgresql://localhost:5433/triagem_suporte`. Ajuste a porta à instalação. |
| `DB_USER` | Padrão `postgres`. |
| `DB_PASSWORD` | Senha do banco. Obrigatória. |
| `TRELLO_API_KEY`, `TRELLO_TOKEN` | Credenciais da conta de teste, com leitura e criação de cartões. Obrigatórias. |
| `TRELLO_LIST_FINANCEIRO`, `TRELLO_LIST_TECNICO`, `TRELLO_LIST_REVISAO_HUMANA` | IDs das listas do quadro de teste. Obrigatórios. |
| `OLLAMA_BASE_URL` | Padrão `http://localhost:11434`. |
| `OLLAMA_MODEL` | Padrão `qwen3.5:4b`. |
| `OLLAMA_READ_TIMEOUT` | Padrão `45s`. Ajuste se a execução local precisar de mais tempo. |

Crie o banco vazio para uma instalação nova. Se já existe uma tabela `chamado`, siga a [migração de bancos existentes](docs/PLANO_REFATORACAO.md#migração-de-banco-existente) antes de iniciar. Rotacione a senha que apareceu em commits antigos.

```bash
ollama pull qwen3.5:4b
./mvnw spring-boot:run
```

Inicie `ollama serve` se o Ollama ainda estiver parado. No Windows, use `.\mvnw.cmd spring-boot:run`.

## API REST

```bash
curl -i http://localhost:8080/chamados \
  -H 'Content-Type: application/json' \
  -d '{"ticketId":"DEMO-001","customerId":"CLIENTE-DEMO","message":"Preciso da segunda via da minha fatura."}'
curl -i http://localhost:8080/chamados/DEMO-001
```

| Operação | Resposta |
|---|---|
| `POST /chamados` | `201` com o registro e o resultado da triagem, inclusive em revisão ou falha de encaminhamento. |
| `GET /chamados/{ticketId}` | `200` com o registro ou `404` com `{"erro":"Chamado não encontrado"}`. |
| Entrada inválida | `400` com `erro` e, quando aplicável, os erros por campo em `campos`. |
| Ticket já registrado | `409` com `erro`. O envio duplicado não gera outro cartão. |

Exemplo de resposta de sucesso:

```json
{
  "ticketId": "DEMO-001",
  "categoria": "COBRANCA",
  "prioridade": "NORMAL",
  "status": "ENCAMINHADO",
  "justificativa": "Solicitação de segunda via da fatura.",
  "equipeDestino": "FINANCEIRO",
  "idExterno": "cartao-exemplo"
}
```

A resposta pública omite `id`, `customerId` e `message`. Os identificadores de entrada aceitam até 255 caracteres e a mensagem até 10.000. Use `X-Request-Id` para correlacionar a resposta aos logs. A autenticação da API segue fora do escopo demonstrativo.

## Fluxo e arquitetura

| Componente | Responsabilidade |
|---|---|
| Spring MVC e DTOs | Contrato HTTP, validação de entrada e erros. |
| `ChamadoService` | Orquestra persistência, classificação e encaminhamento. |
| `Chamado` e `Classificacao` | Aplicam os resultados e estados com tipos de domínio. |
| JPA, PostgreSQL e Flyway | Persistem os estados e garantem unicidade de `ticketId`; evoluem o schema com SQL versionado. |
| Spring AI e Ollama | Convertem a mensagem em proposta estruturada com o [catálogo de políticas](src/main/resources/policies/politicas-atendimento.json). O modelo roda localmente. |
| `EncaminhamentoService` | Porta para o registro externo. O adaptador usa a API REST do Trello. |

`COBRANCA` segue para `FINANCEIRO`, `PROBLEMA_TECNICO` para `TECNICO` e `OUTRO` para `REVISAO_HUMANA`. `ALTA` é reservada para indisponibilidade que impede o uso do produto. Casos ambíguos, respostas incoerentes ou falhas da IA vão para `REVISAO_PENDENTE`.

O backend persiste `RECEBIDO` antes de chamar a IA e `EM_ANALISE` antes de chamar o Trello. Com ID de cartão confirmado, persiste `ENCAMINHADO`. Quando o resultado externo continua inconclusivo, persiste `FALHA_ENCAMINHAMENTO`. Uma leitura de reconciliação por marcador pode recuperar a confirmação perdida, com os [limites descritos no plano](docs/PLANO_REFATORACAO.md#reconciliação-e-operação).

Java 17 oferece tipagem estática e suporte aos recursos usados no domínio, como records e expressões `switch`. Spring Boot organiza a configuração, a validação e os componentes da API. PostgreSQL garante unicidade sob concorrência e permite migrações transacionais. Spring AI integra o Ollama para executar a classificação localmente, sem uma API comercial.

## Testes

```bash
./mvnw test
```

Essa execução roda testes unitários, Spring MVC e um servidor HTTP de loopback para o Trello. Os testes de PostgreSQL usam um banco descartável e exigem ativação explícita. A CI já ativa essa etapa:

```bash
docker run --rm --name triagem-postgres-teste -p 127.0.0.1:55432:5432 \
  -e POSTGRES_DB=triagem_teste -e POSTGRES_USER=triagem_teste \
  -e POSTGRES_PASSWORD=triagem_teste -d postgres:17
TRIAGEM_TEST_POSTGRES=true ./mvnw verify
docker stop triagem-postgres-teste
```

A suíte automatizada usa credenciais sintéticas e substitui Ollama e Trello. Qualquer demonstração com o Trello é manual, iniciando a aplicação com um quadro de teste e um `ticketId` sintético novo.

[Referência OpenAPI](docs/openapi.yaml) · [Decisões, testes e trade-offs](docs/PLANO_REFATORACAO.md)
