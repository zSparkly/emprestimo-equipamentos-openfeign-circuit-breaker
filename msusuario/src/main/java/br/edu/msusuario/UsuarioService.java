package br.edu.msusuario;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
  private final UsuarioRepository repo;

  public UsuarioService(UsuarioRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public UsuarioDto criar(UsuarioDto.Entrada e) {
    Usuario u = new Usuario();
    u.setId(UUID.randomUUID());
    u.setNome(e.nome().trim());
    u.setEmail(e.email().trim().toLowerCase(Locale.ROOT));
    u.setMatricula(e.matricula().trim());
    return UsuarioDto.from(repo.saveAndFlush(u));
  }

  public UsuarioDto buscar(UUID id) {
    return UsuarioDto.from(
        repo.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado")));
  }
}
