package br.edu.msusuario;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
  private final UsuarioService service;

  public UsuarioController(UsuarioService s) {
    service = s;
  }

  @PostMapping
  public ResponseEntity<UsuarioDto> criar(@Valid @RequestBody UsuarioDto.Entrada e) {
    var u = service.criar(e);
    return ResponseEntity.created(java.net.URI.create("/api/v1/usuarios/" + u.id())).body(u);
  }

  @GetMapping("/{id}")
  public UsuarioDto buscar(@PathVariable UUID id) {
    return service.buscar(id);
  }
}
