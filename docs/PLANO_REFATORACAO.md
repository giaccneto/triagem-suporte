# Refatoração de segurança e confiabilidade

## Decisões e trade-offs

| Problema | Decisão | Teste | Trade-off |
|---|---|---|---|
| A API devolvia a entidade JPA e tinha formatos diferentes de erro. | DTO público, erros JSON e limite de tamanho na entrada. O POST mantém `201` e a consulta usa o `ticketId`. | MockMvc cobre 200, 201, 400, 404, 409, campos omitidos e correlação. | Consumidores que liam `id`, `customerId` ou `message` devem se adaptar. |
| O modelo decidia se chamava a ferramenta que fornecia as políticas. | Incluímos o catálogo estático na mensagem de sistema, sem ferramentas disponíveis. O backend mapeia a saída para enums e valida revisão, prioridade admissível e destino. | Modelo simulado recebe a política e uma tentativa de injeção em papéis separados; saída que tenta mandar cobrança de alta prioridade ao técnico é rejeitada. | Os metadados do Spring AI 2.0.0 não declaram as antigas propriedades de limite. O catálogo ocupa contexto a cada chamada. A validação deve acompanhar alterações no catálogo e não consegue provar se o relato do cliente realmente descreve indisponibilidade. |
| Duas requisições simultâneas podiam passar pela consulta prévia de unicidade. | O índice único no PostgreSQL é a garantia final. O serviço traduz o conflito em `409` e grava cada transição em chamada separada ao repositório. | Corrida com duas threads, erro de unicidade simulado e leitura do estado já confirmado no banco dentro dos mocks externos. | São feitas mais gravações. Uma interrupção do processo ainda pode deixar um registro intermediário. |
| O schema evoluía por `ddl-auto=update`. | Flyway V1 cria o schema de referência e V2 amplia campos de texto e garante o índice único. Hibernate passa a validar o mapeamento. | PostgreSQL 17 com banco novo e teste de baseline com registro legado e restrição de unicidade anterior. | A migração pode bloquear a tabela enquanto altera colunas ou cria o índice. Bancos legados precisam de pré-checagem. Uma restrição antiga equivalente pode deixar dois índices até revisão operacional. |
| O POST externo pode criar o cartão e perder a confirmação. | Timeouts explícitos, credenciais no cabeçalho e uma busca por marcador na lista em falhas ambíguas. O fluxo faz um único POST. | HTTP local com resposta truncada, 503, credencial recusada, marcador duplicado e verificação dos dados enviados. | A consulta acrescenta latência em falhas e precisa de permissão de leitura. Uma consulta sem resultado ainda pode ser inconclusiva. |
| Logs de exceções externas podiam incluir URLs, credenciais ou mensagens. | Logs por evento, ID interno, tipo da falha e ID de correlação gerado no servidor. | Cabeçalho de correlação verificado no MVC; o teste HTTP verifica credenciais fora da URL e ausência do texto do cliente nos dados enviados ao Trello. | A investigação depende do ID interno ou do ID de correlação, e os logs têm menos detalhes da exceção. |
| Testes anteriores dependiam de serviços reais e o Mockito tentava anexar um agente dinamicamente. | Servidor de loopback, PostgreSQL descartável na CI e Mockito baseado em subclasses. | Execução Maven com Java 17 e PostgreSQL 17 isolado. | Testes do banco exigem ativação e uma instância descartável. Mocks de classes finais exigiriam rever a estratégia do Mockito. |

## Migração de banco existente

1. Faça backup e valide a restauração em uma cópia. Compare tipos, identidade da coluna `id`, colunas esperadas e enums armazenados com a V1 e o modelo atual. Bancos com IDs antigos incompatíveis ou colunas divergentes exigem uma migração específica antes do baseline.
2. Verifique se há `ticket_id` duplicados. Resolva divergências com os responsáveis pelos dados antes de aplicar a V2. Evite iniciar gravações durante a operação.

   ```sql
   SELECT ticket_id, count(*)
   FROM chamado
   WHERE ticket_id IS NOT NULL
   GROUP BY ticket_id
   HAVING count(*) > 1;
   ```

3. Para uma tabela existente compatível e sem histórico Flyway, execute `baseline` na versão `1` com o Flyway CLI, apontando para o schema correto. Configure URL, usuário e senha no ambiente do Flyway. Use `flyway -baselineVersion=1 baseline`, seguido de `flyway migrate` ou da inicialização normal da aplicação. Banco vazio deve aplicar V1 e V2 normalmente.
4. Confira o histórico em `flyway_schema_history`, a unicidade e a leitura de tickets preexistentes. Mantenha o baseline automático desativado para que um schema inesperado impeça a inicialização.

## Reconciliação e operação

A criação envia o título com o identificador do ticket e a categoria, além de um marcador `triagem-ticket:` seguido do identificador em Base64 URL-safe sem padding. Use listas dedicadas por ambiente. Um erro ambíguo dispara uma consulta à lista original e só confirma a entrega se houver exatamente um cartão com marcador correspondente e ID válido. Rejeições HTTP 4xx, exceto 408, encerram o envio imediatamente. O Trello recebe apenas dados de roteamento, sem mensagem ou identificador do cliente. O [formato de autenticação](https://developer.atlassian.com/cloud/trello/guides/rest-api/authorization/) e a [consulta de cartões por lista](https://developer.atlassian.com/cloud/trello/rest/api-group-lists/) seguem a API da Atlassian.

Se o cartão ainda não aparecer na consulta, foi movido ou teve o marcador editado, ou se o processo parar depois de criar o cartão e antes de gravar o ID, a reconciliação imediata pode falhar. Para chamados em `EM_ANALISE` ou `FALHA_ENCAMINHAMENTO`, consulte a lista original e o quadro pelo marcador ou título antes de qualquer reenvio manual. Se encontrar um cartão único, registre o ID em uma intervenção auditada. A entrega exatamente uma vez exigiria reconciliação durável e uma estratégia para reenvios. Um POST repetido para o mesmo `ticketId` continua retornando `409`.

A IA tem uma tentativa e timeout de leitura configurável. O teste de injeção verifica o isolamento da entrada e a rejeição de uma saída inconsistente, sem medir a capacidade de um Ollama real de resistir a todas as instruções maliciosas. Gire a senha antiga do banco que apareceu no Git. A revisão do histórico deve ser tratada separadamente.

## Execução verificada

- Referência na branch original com Java 17 e os testes determinísticos selecionados: 13 executados, 8 erros de inicialização do Mockito por falha de attach da JVM.
- Execução final: `TRIAGEM_TEST_POSTGRES=true ./mvnw clean verify` com Java 17 e PostgreSQL descartável 17.7. Build aprovado, 30 testes executados, zero falhas, erros ou testes ignorados.
