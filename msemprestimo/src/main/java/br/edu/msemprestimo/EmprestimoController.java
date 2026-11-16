package br.edu.msemprestimo;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emprestimos")
public class EmprestimoController {
  private final EmprestimoService service;

  public EmprestimoController(EmprestimoService s) {
    service = s;
  }

  @PostMapping
  public ResponseEntity<EmprestimoDto> criar(@Valid @RequestBody EmprestimoDto.Entrada d) {
    var e = service.criar(d);
    return ResponseEntity.created(java.net.URI.create("/api/v1/emprestimos/" + e.id())).body(e);
  }

  @GetMapping("/{id}")
  public EmprestimoDto buscar(@PathVariable UUID id) {
    return service.buscar(id);
  }

  @GetMapping("/usuarios/{usuarioId}")
  public List<EmprestimoDto> usuario(@PathVariable UUID usuarioId) {
    return service.porUsuario(usuarioId);
  }

  @PutMapping("/{id}/devolucao")
  public EmprestimoDto devolver(@PathVariable UUID id, @Valid @RequestBody EmprestimoDto.Devolucao dados) {
    return service.devolver(id, dados.condicao());
  }
}
