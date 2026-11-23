# Plano de refatoração — evolução para qualidade de pleno

## Escopo desta primeira entrega
- [x] Senha PostgreSQL deixa de estar no arquivo atual de configuração (a versão anterior ainda existe no histórico Git).
- [x] Configuração de conexão/integrações via variáveis de ambiente, mantendo IDs de listas de teste como defaults.
- [x] DTO de entrada `CriarChamadoRequest` com Bean Validation, sem expor campos internos.
- [x] Resposta 400 com campos inválidos; 409/400 estruturados como JSON.
- [x] Tratar resultado `null` da IA como revisão humana.
- [x] Tirar logs `System.out` e registrar falhas com SLF4J.
- [x] Persistir `EM_ANALISE` antes do envio externo (ainda não é uma transação outbox).
- [x] Marcar falha caso integração não devolva ID externo.
- [x] Desabilitar teste que cria cartões reais no Trello em execução automática.
- [x] Acrescentar testes de regressão no serviço.
- [ ] **Executar Maven/testes no ambiente real** — NÃO executado por esta alteração remota.
- [ ] Verificar compatibilidade de configuração Spring AI das propriedades de limite de tool calls.

## Etapas seguintes para Codex
1. Testes HTTP e integração com Trello mockado, sem rede.
2. Separar DTO de resposta da entidade; esconder detalhes internos.
3. Estratégia de migrações versionadas e PostgreSQL testcontainers ou banco de teste.
4. Separar transições do fluxo e cobrir falhas de concorrência.
5. Reconciliar cartões criados quando Trello falha após criação; desenhar idempotência.
6. Timeout explícito, política de retry consciente (sem duplicar cartões).
7. CI GitHub Actions, documentação revisada e ADRs.

## Mudanças de configuração que requerem atenção
- `DB_PASSWORD` passou a ser **obrigatória** no Spring Boot; configurar no IntelliJ e ambiente de execução.
- `DB_URL`, `DB_USER`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL` são parametrizáveis.
- `TRELLO_API_KEY` e `TRELLO_TOKEN` permanecem exigidos pelo Trello service.
- Variáveis opcionais `TRELLO_LIST_FINANCEIRO`, `TRELLO_LIST_TECNICO`, `TRELLO_LIST_REVISAO_HUMANA` substituem as listas de testes.
- A resposta de erros 400/409 passa de texto simples para objeto JSON: atualizar consumidores.
- **Segurança:** rotacionar senha PostgreSQL que apareceu em commits anteriores e investigar limpeza do histórico usando procedimento seguro, sem reescrever histórico automaticamente.

## Trade-offs
- Mantemos o monolito enquanto os requisitos não justificarem microsserviços.
- A integração continua síncrona por compatibilidade. Estado EM_ANALISE persistido melhora inspeção, mas não garante entrega única diante de falha entre o Trello e o banco.
- `ddl-auto=update` permanece temporariamente para não migrar schema sem executar/testar em banco real.
