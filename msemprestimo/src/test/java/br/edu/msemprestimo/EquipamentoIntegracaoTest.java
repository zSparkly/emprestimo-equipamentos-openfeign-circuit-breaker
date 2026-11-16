package br.edu.msemprestimo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest(classes = EquipamentoIntegracaoTest.Config.class,
    properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class EquipamentoIntegracaoTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
  static class Config {
    @Bean EquipamentoClient cliente() { return mock(EquipamentoClient.class); }
    @Bean EquipamentoIntegracao integracao(EquipamentoClient cliente) {
      return new EquipamentoIntegracao(cliente);
    }
  }

  @Autowired EquipamentoClient cliente;
  @Autowired EquipamentoIntegracao integracao;
  @Autowired CircuitBreakerRegistry registro;
  final UUID equipamento = UUID.randomUUID();

  @BeforeEach void preparar() {
    reset(cliente);
    registro.circuitBreaker("alterarCondicao").reset();
  }

  FeignException erro(int status) {
    var request = Request.create(Request.HttpMethod.PUT, "http://msequipamento/condicao",
        Map.of(), null, java.nio.charset.StandardCharsets.UTF_8, null);
    return FeignException.errorStatus("alterarCondicao",
        Response.builder().request(request).status(status).headers(Map.of()).build());
  }

  @Test void erro404MantemSignificadoENaoContaNoCircuito() {
    when(cliente.alterarCondicao(any(), any())).thenThrow(erro(404));
    for (int i = 0; i < 6; i++) {
      var erro = assertThrows(ApiException.class,
          () -> integracao.alterarCondicao(equipamento, CondicaoEquipamento.BOM));
      assertEquals(404, erro.status.value());
    }
    var circuito = registro.circuitBreaker("alterarCondicao");
    assertEquals(State.CLOSED, circuito.getState());
    assertEquals(0, circuito.getMetrics().getNumberOfFailedCalls());
    assertEquals(0, circuito.getMetrics().getNumberOfBufferedCalls());
  }

  @Test void falhasAbremCircuitoBloqueiamChamadasEPermitemRecuperacao() {
    when(cliente.alterarCondicao(any(), any())).thenThrow(erro(503));
    for (int i = 0; i < 4; i++) {
      var erro = assertThrows(ApiException.class,
          () -> integracao.alterarCondicao(equipamento, CondicaoEquipamento.AVARIADO));
      assertEquals(503, erro.status.value());
    }
    var circuito = registro.circuitBreaker("alterarCondicao");
    assertEquals(State.OPEN, circuito.getState());
    verify(cliente, times(3)).alterarCondicao(any(), any());
    assertEquals(1, circuito.getMetrics().getNumberOfNotPermittedCalls());
    reset(cliente);
    circuito.transitionToHalfOpenState();
    integracao.alterarCondicao(equipamento, CondicaoEquipamento.BOM);
    assertEquals(State.HALF_OPEN, circuito.getState());
    integracao.alterarCondicao(equipamento, CondicaoEquipamento.BOM);
    assertEquals(State.CLOSED, circuito.getState());
  }
}
