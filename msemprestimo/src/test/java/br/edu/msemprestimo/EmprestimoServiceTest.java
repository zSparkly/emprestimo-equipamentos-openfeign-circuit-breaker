package br.edu.msemprestimo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class EmprestimoServiceTest {
  EmprestimoRepository repo;
  UsuarioClient usuarios;
  EquipamentoClient equipamentos;
  EmprestimoService service;
  UUID usuario = UUID.randomUUID(), equipamento = UUID.randomUUID();

  @BeforeEach
  void setup() {
    repo = mock(EmprestimoRepository.class);
    usuarios = mock(UsuarioClient.class);
    equipamentos = mock(EquipamentoClient.class);
    PlatformTransactionManager manager =
        new PlatformTransactionManager() {
          public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
          }

          public void commit(TransactionStatus status) {}

          public void rollback(TransactionStatus status) {}
        };
    service = new EmprestimoService(repo, usuarios, equipamentos, new TransactionTemplate(manager), new EquipamentoIntegracao(equipamentos));
  }

  @Test
  void persisteSomenteDepoisDaReserva() {
    when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    var result = service.criar(new EmprestimoDto.Entrada(usuario, equipamento));
    var order = inOrder(usuarios, equipamentos, repo);
    order.verify(usuarios).buscar(usuario);
    order.verify(equipamentos).buscar(equipamento);
    order.verify(equipamentos).reservar(equipamento, result.id());
    order.verify(repo).saveAndFlush(any());
    assertEquals(StatusEmprestimo.ATIVO, result.status());
  }

  @Test
  void falhaDePersistenciaCompensaUsandoOMesmoId() {
    when(repo.saveAndFlush(any())).thenThrow(new IllegalStateException("Banco indisponivel"));
    assertThrows(
        IllegalStateException.class,
        () -> service.criar(new EmprestimoDto.Entrada(usuario, equipamento)));
    var id = org.mockito.ArgumentCaptor.forClass(UUID.class);
    verify(equipamentos).reservar(eq(equipamento), id.capture());
    verify(equipamentos).liberar(equipamento, id.getValue());
  }

  @Test
  void devolucaoConcluidaNaoLiberaUmaNovaReserva() {
    var e = new Emprestimo();
    e.setId(UUID.randomUUID());
    e.setStatus(StatusEmprestimo.DEVOLVIDO);
    e.setDataDevolucao(java.time.LocalDateTime.of(2026, 11, 14, 10, 0));
    when(repo.bloquear(e.getId())).thenReturn(Optional.of(e));
    assertEquals(e.getDataDevolucao(), service.devolver(e.getId(), CondicaoEquipamento.BOM).dataDevolucao());
    verifyNoInteractions(equipamentos);
    verify(repo, never()).saveAndFlush(any());
  }

  @Test
  void atualizaCondicaoAntesDeLiberarEConcluir() {
    var e = ativo();
    when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    var resultado = service.devolver(e.getId(), CondicaoEquipamento.AVARIADO);
    var ordem = inOrder(equipamentos, repo);
    ordem.verify(repo).bloquear(e.getId());
    ordem.verify(equipamentos).alterarCondicao(equipamento,
        new EmprestimoDto.Devolucao(CondicaoEquipamento.AVARIADO));
    ordem.verify(equipamentos).liberar(equipamento, e.getId());
    ordem.verify(repo).saveAndFlush(e);
    assertEquals(StatusEmprestimo.DEVOLVIDO, resultado.status());
  }

  @Test
  void falhaRemotaNaoLiberaNemConcluiDevolucao() {
    var e = ativo();
    var request = feign.Request.create(feign.Request.HttpMethod.PUT, "http://equipamento",
        java.util.Map.of(), null, java.nio.charset.StandardCharsets.UTF_8, null);
    when(equipamentos.alterarCondicao(any(), any())).thenThrow(
        feign.FeignException.errorStatus("alterarCondicao", feign.Response.builder()
            .request(request).status(503).headers(java.util.Map.of()).build()));
    assertThrows(feign.FeignException.class,
        () -> service.devolver(e.getId(), CondicaoEquipamento.BOM));
    assertEquals(StatusEmprestimo.ATIVO, e.getStatus());
    assertNull(e.getDataDevolucao());
    verify(equipamentos, never()).liberar(any(), any());
    verify(repo, never()).saveAndFlush(any());
  }

  private Emprestimo ativo() {
    var e = new Emprestimo();
    e.setId(UUID.randomUUID());
    e.setEquipamentoId(equipamento);
    e.setStatus(StatusEmprestimo.ATIVO);
    when(repo.bloquear(e.getId())).thenReturn(Optional.of(e));
    return e;
  }
}
