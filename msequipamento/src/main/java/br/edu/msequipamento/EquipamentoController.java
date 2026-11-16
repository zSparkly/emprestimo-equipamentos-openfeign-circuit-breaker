package br.edu.msequipamento;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/equipamentos")
public class EquipamentoController {
  private final EquipamentoService service;

  public EquipamentoController(EquipamentoService s) {
    service = s;
  }

  @PostMapping
  public ResponseEntity<EquipamentoDto> criar(@Valid @RequestBody EquipamentoDto.Entrada d) {
    var e = service.criar(d);
    return ResponseEntity.created(java.net.URI.create("/api/v1/equipamentos/" + e.id())).body(e);
  }

  @PutMapping("/{id}/condicao")
  public EquipamentoDto alterarCondicao(@PathVariable UUID id,
      @Valid @RequestBody EquipamentoDto.AlteracaoCondicao dados) {
    return service.alterarCondicao(id, dados.condicao());
  }

  @GetMapping("/{id}")
  public EquipamentoDto buscar(@PathVariable UUID id) {
    return service.buscar(id);
  }

  @GetMapping("/disponiveis")
  public List<EquipamentoDto> disponiveis() {
    return service.disponiveis();
  }

  @PutMapping("/{id}/reservas/{emprestimoId}")
  public EquipamentoDto reservar(@PathVariable UUID id, @PathVariable UUID emprestimoId) {
    return service.reservar(id, emprestimoId);
  }

  @DeleteMapping("/{id}/reservas/{emprestimoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void liberar(@PathVariable UUID id, @PathVariable UUID emprestimoId) {
    service.liberar(id, emprestimoId);
  }
}
