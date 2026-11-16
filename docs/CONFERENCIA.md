# Conferência da atividade - Etapa 2

Aluno: Daniel de Oliveira Barreto Moura

| Requisito | Implementação |
| --- | --- |
| RF10 | Enum BOM/AVARIADO, campo na entidade existente e migração V2 com padrão BOM |
| RF11 | PUT /api/v1/equipamentos/{id}/condicao, validação do corpo, retorno atualizado e 404 |
| RF12 | PUT de devolução recebe condição; atualiza via Feign antes de liberar reserva e concluir |
| RF13 | @CircuitBreaker na integração da condição, fallback 503 e 4xx ignorados no circuito |
| Parâmetros | Janela 5, mínimo 3, limiar 50%, espera 30s e 2 chamadas em HALF_OPEN |
| Repetição | Empréstimo já devolvido conserva data, condição e reserva atual do equipamento |
| Estrutura | Mesmas cinco aplicações, três entidades e três bancos da primeira etapa |
| Entrega | Código-fonte, migração, Postman, README, roteiro e relatórios |

## Verificação

Os testes Java conferem a sequência das operações, a ausência de gravação após falha remota, a repetição de devolução e o funcionamento real da anotação por proxy Spring. O teste da integração utiliza um cliente Feign simulado para produzir respostas 404 e 503 de forma controlada. O 404 não conta como falha nem como sucesso na janela.

O script `scripts/test-etapa2.ps1` verifica CT01 e CT02 por HTTP, com Gateway e MySQL reais. Para CT03, verifica o 404 no endpoint de condição; a exclusão desse erro das métricas também é coberta pelo teste Java da integração. CT04 e CT05 desligam o processo real de equipamentos, conferem o 503 e o empréstimo ATIVO, verificam OPEN e uma chamada bloqueada. CT06 restaura o serviço, aguarda mais de 30 segundos e confere HALF_OPEN e CLOSED após dois sucessos de empréstimos diferentes.

Os relatórios `resultado-etapa2.json` e `eventos-circuito-etapa2.json` registram a execução HTTP e as transições do circuito. A regressão das funções da primeira etapa também foi repetida com o novo contrato PUT: 26 cenários passaram em `resultado-testes.json`. O validador local executou 29 requisições e seus scripts das coleções Postman, registradas em `resultado-postman-etapa2.json`; o aplicativo Postman não foi aberto. Os outros relatórios antigos se referem à execução original da primeira etapa.

## Limites

Não há transação distribuída: a condição pode ter sido atualizada remotamente antes de uma falha na liberação ou no commit do empréstimo. Uma repetição enquanto o empréstimo estiver ATIVO refaz a atualização da condição e tenta concluir o fluxo. Uma repetição depois de DEVOLVIDO não chama novamente o equipamento. Não foram acrescentadas retentativas automáticas, filas ou compensações novas.

## Envio

Destinatário: southiagorm@gmail.com. Assunto: **Exercício 2 Microsserviços Pós Graduação**. Participante: **Daniel de Oliveira Barreto Moura**. O PDF desta etapa não fixa uma data de entrega.
