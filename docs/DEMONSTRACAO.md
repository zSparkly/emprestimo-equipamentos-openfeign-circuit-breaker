# Roteiro de demonstração

Aluno: Daniel de Oliveira Barreto Moura

## Apresentação

O projeto continua com os mesmos serviços da primeira etapa. A mudança principal está na devolução: agora é preciso informar como o equipamento voltou. O serviço de empréstimos atualiza essa informação no serviço de equipamentos antes de liberar a reserva e encerrar o empréstimo.

## Demonstração automática

1. Abra o PowerShell na pasta extraída e execute `scripts/setup.ps1` se as ferramentas ainda não estiverem instaladas.
2. Execute `scripts/start.ps1` e aguarde a mensagem de sistema pronto.
3. Execute `scripts/test-etapa2.ps1`. Ele vai criar dados próprios e mostrar cada resultado no terminal.
4. Mostre `docs/resultado-etapa2.json`: há devoluções BOM e AVARIADO, repetições, validações e respostas 503 com o empréstimo ainda ativo.
5. Mostre `docs/eventos-circuito-etapa2.json`: procure eventos STATE_TRANSITION para CLOSED_TO_OPEN, OPEN_TO_HALF_OPEN e HALF_OPEN_TO_CLOSED. Os eventos NOT_PERMITTED comprovam bloqueio pelo circuito.

O script desliga e restaura somente o serviço de equipamentos deste projeto. A recuperação inclui uma espera real de 31 segundos. O relatório final só é gravado se todas as verificações passarem.

## Demonstração no Postman

Importe `postman_etapa2.json`. Execute a pasta de fluxo normal em ordem. Ela mostra as duas condições e deixa um empréstimo ativo preparado para a falha.

No PowerShell, pare o equipamento:

```powershell
Stop-Process -Id ([int](Get-Content .runtime/msequipamento.pid))
```

Na pasta de indisponibilidade, execute a devolução até o circuito abrir. Como a janela pode conter sucessos anteriores, o número de falhas para abrir pode variar. Confira 503 e consulte o empréstimo: ele deve continuar ATIVO e sem data de devolução.

Consulte `http://localhost:8083/actuator/circuitbreakers` e `http://localhost:8083/actuator/circuitbreakerevents`. O primeiro informa o estado e o segundo mantém os eventos recentes.

Restaure o serviço:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start.ps1 -SkipBuild
```

Aguarde 31 segundos e execute a devolução na pasta de recuperação. A primeira atualização remota bem-sucedida deixa o circuito em HALF_OPEN. Para fechar o circuito, cadastre outro equipamento, crie outro empréstimo e faça outra devolução. Repetir um empréstimo já devolvido não faz chamada remota e, por isso, não serve como segundo sucesso do circuito.

## Explicação do resultado

Quando a atualização funciona, o sistema libera a reserva e registra DEVOLVIDO. Quando o equipamento está fora do ar, o fallback responde 503 e a devolução fica pendente. Depois de falhas suficientes, o circuito bloqueia novas tentativas por um período. Após a espera, duas chamadas de teste bem-sucedidas permitem voltar ao funcionamento normal.

Um equipamento que não existe é um erro de negócio: retorna 404 e não aumenta a contagem de falhas técnicas. Isso é verificado no teste Java da integração.
