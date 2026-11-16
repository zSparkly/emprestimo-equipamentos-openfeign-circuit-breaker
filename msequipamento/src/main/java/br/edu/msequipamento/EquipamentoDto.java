package br.edu.msequipamento;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record EquipamentoDto(
    UUID id, String nome, String tipo, String patrimonio, Boolean disponivel, CondicaoEquipamento condicao) {
  public record Entrada(
      @NotBlank @Size(max = 150) String nome,
      @NotBlank @Size(max = 60) String tipo,
      @NotBlank @Size(max = 100) String patrimonio) {}

  public record AlteracaoCondicao(@NotNull CondicaoEquipamento condicao) {}

  static EquipamentoDto from(Equipamento e) {
    return new EquipamentoDto(
        e.getId(), e.getNome(), e.getTipo(), e.getPatrimonio(), e.getDisponivel(), e.getCondicao());
  }
}
