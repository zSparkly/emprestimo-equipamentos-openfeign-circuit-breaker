package br.edu.msemprestimo;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EquipamentoIntegracao {
  private final EquipamentoClient cliente;

  public EquipamentoIntegracao(EquipamentoClient cliente) { this.cliente = cliente; }

  @CircuitBreaker(name = "alterarCondicao", fallbackMethod = "indisponivel")
  public void alterarCondicao(UUID id, CondicaoEquipamento condicao) {
    try {
      cliente.alterarCondicao(id, new EmprestimoDto.Devolucao(condicao));
    } catch (FeignException erro) {
      // Uma resposta de negócio não representa indisponibilidade do serviço.
      if (erro.status() >= 400 && erro.status() < 500) {
        throw new ErroNegocio(HttpStatus.valueOf(erro.status()),
            erro.status() == 404 ? "Equipamento não encontrado" : "Alteração de condição recusada pelo serviço de equipamentos");
      }
      throw erro;
    }
  }

  private void indisponivel(UUID id, CondicaoEquipamento condicao, FeignException erro) {
    throw falha();
  }

  private void indisponivel(UUID id, CondicaoEquipamento condicao, CallNotPermittedException erro) {
    throw falha();
  }

  private ApiException falha() {
    return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
        "O serviço de equipamentos está indisponível. A devolução não foi concluída. Tente novamente em instantes.");
  }

  public static class ErroNegocio extends ApiException {
    public ErroNegocio(HttpStatus status, String mensagem) { super(status, mensagem); }
  }
}
