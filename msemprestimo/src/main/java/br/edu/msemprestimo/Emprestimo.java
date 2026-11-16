package br.edu.msemprestimo;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "tb_emprestimos")
@Getter
@Setter
@NoArgsConstructor
public class Emprestimo {
  @Id private UUID id;
  private UUID usuarioId;
  private UUID equipamentoId;
  private LocalDateTime dataEmprestimo;
  private LocalDateTime dataDevolucao;

  @Enumerated(EnumType.STRING)
  private StatusEmprestimo status;
}
