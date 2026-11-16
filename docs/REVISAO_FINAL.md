# Revisao final da etapa 2

Conferencia realizada em 16/11/2026 com base nas duas paginas do enunciado.

| Item | Resultado conferido |
| --- | --- |
| RF10 | Campo condicao na entidade Equipamento existente, enum BOM/AVARIADO e migracao Flyway V2 com padrao BOM |
| RF11 | PUT /api/v1/equipamentos/{id}/condicao recebe a condicao, atualiza e retorna o equipamento; inexistente retorna 404 |
| RF12 | PUT /api/v1/emprestimos/{id}/devolucao recebe condicao, atualiza via Feign antes de liberar a reserva e gravar DEVOLVIDO |
| RF13 | @CircuitBreaker somente na integracao da alteracao de condicao; indisponibilidade retorna 503 e preserva emprestimo ATIVO |
| Configuracao | Janela 5, minimo 3 chamadas, limiar 50%, espera 30s e 2 chamadas em HALF_OPEN |
| Erros de negocio | Respostas 4xx preservam seu status e nao entram na janela do circuito |
| Repeticao | Devolucao ja concluida conserva a data e nao altera novamente o equipamento ou outra reserva |
| Estrutura | Mesmas cinco aplicacoes, tres entidades e tres bancos; sem novas filas ou microsservicos |
| Material | Codigo-fonte, migracao, exemplos Postman, README, roteiro de demonstracao e relatorios |

## Validacao repetida

- Maven verify: cinco modulos compilados, sete testes Java passaram, sem falhas ou erros.
- Regressao por HTTP: 26 cenarios passaram.
- Etapa 2 por HTTP: 32 verificacoes passaram, com servicos reais, Gateway e MySQL.
- Colecoes Postman: 29 requisicoes e seus scripts passaram por validador local; o aplicativo Postman nao foi aberto.
- Comparacao local: os 60 arquivos de codigo, configuracao e scripts usados nos testes eram iguais aos da copia publicada.

CT01 e CT02 conferiram BOM e AVARIADO, equipamento liberado e emprestimo DEVOLVIDO. CT03 conferiu 404 real no endpoint de condicao; o teste Java da integracao, por proxy Spring e com Feign simulado, verificou seis respostas 404 sem contabilizar chamadas na janela. CT04 e CT05 desligaram o processo real de msequipamento, verificaram 503, emprestimo ATIVO, OPEN e bloqueio. CT06 restaurou o processo, aguardou 31 segundos e verificou HALF_OPEN seguido de CLOSED apos dois sucessos.

Os resultados estao nos JSON desta pasta e em resultado-testes-java.txt. Nao foi identificada divergencia funcional em relacao aos requisitos do PDF. O roteiro DEMONSTRACAO.md permite repetir o sucesso e o fallback exigidos.

## Envio e apresentacao

O enunciado pede envio para southiagorm@gmail.com, assunto "Exercicio 2 Microsservicos Pos Graduacao" (usar a acentuacao do modelo em ENTREGA.md) e identificacao dos participantes. O nome completo do aluno consta no README e no modelo de email. O PDF tambem orienta realizacao em sala e demonstracao do funcionamento; a revisao tecnica nao comprova participacao em sala nem substitui eventual apresentacao solicitada pelo professor.
