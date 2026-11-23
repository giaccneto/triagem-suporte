# Instruções para Codex — Triagem de Suporte

Você está refatorando um backend Java 17 / Spring Boot 4.1.2, JPA/PostgreSQL, Spring AI + Ollama e Trello. Objetivo: qualidade de engenharia compatível com desenvolvedor pleno, sem reescrever desnecessariamente e sem introduzir infra de produção que o projeto não exige.

## Regras obrigatórias
- Antes de editar, leia README, pom.xml, fluxo do ChamadoService, políticas JSON e testes. Execute testes determinísticos e registre comandos e resultados reais.
- Preserve o contrato de negócio: POST /chamados => 201; GET /chamados/{ticketId}; COBRANCA/PROBLEMA_TECNICO/OUTRO; NORMAL/ALTA; revisão humana; falha de integração.
- Nunca acessar Trello real, dados reais ou banco real durante testes automáticos. Use mocks/stubs e contexto isolado.
- Nunca incluir segredos ou dados pessoais em código, testes, logs ou commits. A senha antiga já esteve no Git; exigir rotação, não reproduzi-la. Histórico exige tratamento separado.
- Evitar código pronto sem explicação ao aluno: usuário quer aprender com refatoração, porém autorizou implementação. Para cada mudança, registrar "problema -> decisão -> teste -> trade-off".
- Trabalhar em branch própria e PR; não alterar main diretamente, não fazer merge sem revisão.
- Não afirmar testes passando sem realmente executar, não mudar versão de dependências sem confirmar compatibilidade.
- Evitar abstrações genéricas, arquitetura de microserviços, filas e Kubernetes por aparência; aplicar mudanças com custo/benefício justificável.

## Objetivos da refatoração completa, em etapas
1. **Segurança e confiança:** remover segredos fixos, variáveis de ambiente, separar DTO da entidade, validar entradas e respostas da IA, mensagens de erro uniformes, não enviar dados sensíveis em log.
2. **Testabilidade:** unitários de sucesso/erro, testes HTTP de 400/404/409/201, integração isolada Trello com servidor mock, testes de banco isolado. Teste Trello externo manual opt-in.
3. **Separação de responsabilidades:** reduzir orquestrador, mapear resultado de classificação para objeto de domínio com enums validados, separar política/determinação do destino e aplicação de transições de status; manter interfaces só onde há benefício.
4. **Consistência:** tratar concorrência de ticketId com índice único e teste; investigar idempotência de retries Trello, persistência segura diante de falha parcial, nenhuma transação longa durante chamadas HTTP.
5. **Persistência:** usar migrações versionadas (Flyway ou Liquibase); introduzir schema inicial sem destruir dados existentes, tomar cuidado com `ddl-auto=update`; colunas de texto adequadas a mensagens longas.
6. **HTTP e configuração:** DTOs de request/response, padronizar erros, documentação OpenAPI se compatível, timeouts externos, config type-safe quando fizer sentido.
7. **Observabilidade:** SLF4J, logs estruturados por ticketId sem payload sensível, correlation IDs, health checks opcionais.
8. **CI e documentação:** workflow que executa testes isolados, documentação de variáveis, ADRs para decisões importantes, exemplos de requests, limitações de Ollama local e Trello.
9. **Revisão de segurança de IA:** aplicar validação backend como fonte de verdade, testes de prompt injection e classificação inválida; não presumir que propriedades de limite de tools realmente funcionam sem teste na versão usada.

## Critérios de aceite
- Build e testes verificáveis, sem rede externa/segredos exigidos para testes unitários.
- Controller não faz regra de negócio; domínio não depende de Trello/Ollama.
- Erros 400/409/404 e estados persistidos são testados.
- Nenhuma alteração quebra dados persistidos sem estratégia explícita de migração.
- README é atualizado; PR explica o que mudou, riscos e o que ainda falta.
- Descrever honestamente limites: Trello pode criar cartão e perder resposta; sem reconciliação não há exatamente uma entrega garantida.

## Como trabalhar
Primeiro dê um resumo de diagnóstico e plano curto. Faça alterações reais com diffs e testes, em incrementos revisáveis. Quando terminar cada etapa, registre resultados e próximo passo. Não só descrever arquitetura: implemente, teste, atualize documentação e apresente PR.
