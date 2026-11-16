package br.edu.msemprestimo;

import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ErrorHandler {
  private ResponseEntity<?> error(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "status",
                status.value(),
                "mensagem",
                message,
                "timestamp",
                Instant.now().toString()));
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> api(ApiException e) {
    return error(e.status, e.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<?> invalid(Exception e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Dados inválidos. Confira os campos obrigatórios, o e-mail e os identificadores UUID.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> conflict(Exception e) {
    return error(
        HttpStatus.CONFLICT,
        "Já existe um registro com esses dados ou houve um conflito de integridade.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(ErrorHandler.class).error("Falha interna", e);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Consulte o log do serviço.");
  }
}
