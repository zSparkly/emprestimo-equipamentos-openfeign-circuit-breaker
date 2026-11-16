package br.edu.msemprestimo;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmprestimoDto(
    UUID id,
    UUID usuarioId,
    UUID equipamentoId,
    LocalDateTime dataEmprestimo,
    LocalDateTime dataDevolucao,
    StatusEmprestimo status) {
  public record Devolucao(@NotNull CondicaoEquipamento condicao) {}

  public record Entrada(@NotNull UUID usuarioId, @NotNull UUID equipamentoId) {}

  static EmprestimoDto from(Emprestimo e) {
    return new EmprestimoDto(
        e.getId(),
        e.getUsuarioId(),
        e.getEquipamentoId(),
        e.getDataEmprestimo(),
        e.getDataDevolucao(),
        e.getStatus());
  }
}
