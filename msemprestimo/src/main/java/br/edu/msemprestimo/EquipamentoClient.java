package br.edu.msemprestimo;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "msequipamento")
public interface EquipamentoClient {
  @PutMapping("/api/v1/equipamentos/{id}/condicao")
  Object alterarCondicao(@PathVariable("id") UUID id, @RequestBody EmprestimoDto.Devolucao dados);

  @GetMapping("/api/v1/equipamentos/{id}")
  Object buscar(@PathVariable("id") UUID id);

  @PutMapping("/api/v1/equipamentos/{id}/reservas/{emprestimoId}")
  Object reservar(@PathVariable("id") UUID id, @PathVariable("emprestimoId") UUID emprestimoId);

  @DeleteMapping("/api/v1/equipamentos/{id}/reservas/{emprestimoId}")
  void liberar(@PathVariable("id") UUID id, @PathVariable("emprestimoId") UUID emprestimoId);
}
