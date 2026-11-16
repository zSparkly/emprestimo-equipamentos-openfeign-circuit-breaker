package br.edu.msemprestimo;

import feign.FeignException;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmprestimoService {
  private final EmprestimoRepository repo;
  private final UsuarioClient usuarios;
  private final EquipamentoClient equipamentos;
  private final TransactionTemplate tx;
  private final EquipamentoIntegracao integracao;

  public EmprestimoService(
      EmprestimoRepository r, UsuarioClient u, EquipamentoClient e, TransactionTemplate t, EquipamentoIntegracao integracao) {
    repo = r;
    usuarios = u;
    equipamentos = e;
    tx = t;
    this.integracao = integracao;
  }

  private ApiException remoto(FeignException e) {
    if (e.status() == 404)
      return new ApiException(HttpStatus.NOT_FOUND, "Usuário ou equipamento não encontrado");
    if (e.status() == 409)
      return new ApiException(
          HttpStatus.CONFLICT, "Equipamento indisponivel ou reserva conflitante");
    return new ApiException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Não foi possível acessar um dos serviços. Tente novamente em instantes.");
  }

  public EmprestimoDto criar(EmprestimoDto.Entrada d) {
    UUID id = UUID.randomUUID();
    try {
      usuarios.buscar(d.usuarioId());
      equipamentos.buscar(d.equipamentoId());
      equipamentos.reservar(d.equipamentoId(), id);
    } catch (FeignException e) {
      throw remoto(e);
    }
    try {
      return tx.execute(
          s -> {
            var e = new Emprestimo();
            e.setId(id);
            e.setUsuarioId(d.usuarioId());
            e.setEquipamentoId(d.equipamentoId());
            e.setDataEmprestimo(
                LocalDateTime.now(java.time.ZoneOffset.UTC)
                    .truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            e.setStatus(StatusEmprestimo.ATIVO);
            return EmprestimoDto.from(repo.saveAndFlush(e));
          });
    } catch (RuntimeException failure) {
      try {
        equipamentos.liberar(d.equipamentoId(), id);
      } catch (Exception compensation) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .error(
                "Não foi possível desfazer a reserva: equipamento={} empréstimo={}",
                d.equipamentoId(),
                id,
                compensation);
      }
      throw failure;
    }
  }

  public EmprestimoDto buscar(UUID id) {
    return EmprestimoDto.from(
        repo.findById(id)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado")));
  }

  public List<EmprestimoDto> porUsuario(UUID id) {
    return repo.findByUsuarioId(id).stream().map(EmprestimoDto::from).toList();
  }

  public EmprestimoDto devolver(UUID id, CondicaoEquipamento condicao) {
    return tx.execute(
        s -> {
          var e =
              repo.bloquear(id)
                  .orElseThrow(
                      () -> new ApiException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado"));
          if (e.getStatus() == StatusEmprestimo.DEVOLVIDO) return EmprestimoDto.from(e);
          if (e.getStatus() != StatusEmprestimo.ATIVO)
            throw new ApiException(HttpStatus.CONFLICT, "O empréstimo não está ativo");
          integracao.alterarCondicao(e.getEquipamentoId(), condicao);
          try {
            equipamentos.liberar(e.getEquipamentoId(), id);
          } catch (FeignException ex) {
            throw remoto(ex);
          }
          e.setStatus(StatusEmprestimo.DEVOLVIDO);
          e.setDataDevolucao(
              LocalDateTime.now(java.time.ZoneOffset.UTC)
                  .truncatedTo(java.time.temporal.ChronoUnit.MICROS));
          return EmprestimoDto.from(repo.saveAndFlush(e));
        });
  }
}
