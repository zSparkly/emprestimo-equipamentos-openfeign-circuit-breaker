package br.edu.msemprestimo;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "msusuario")
public interface UsuarioClient {
  @GetMapping("/api/v1/usuarios/{id}")
  Object buscar(@PathVariable("id") UUID id);
}
