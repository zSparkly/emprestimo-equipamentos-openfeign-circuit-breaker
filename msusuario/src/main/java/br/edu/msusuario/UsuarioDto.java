package br.edu.msusuario;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record UsuarioDto(UUID id, String nome, String email, String matricula) {
  public record Entrada(
      @NotBlank @Size(max = 150) String nome,
      @NotBlank @Email @Size(max = 200) String email,
      @NotBlank @Size(max = 60) String matricula) {}

  static UsuarioDto from(Usuario u) {
    return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getMatricula());
  }
}
