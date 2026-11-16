package br.edu.msequipamento;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "tb_equipamentos")
@Getter
@Setter
@NoArgsConstructor
public class Equipamento {
  @Id private UUID id;
  private String nome;
  private String tipo;
  private String patrimonio;
  private Boolean disponivel;
  private UUID reservaId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CondicaoEquipamento condicao = CondicaoEquipamento.BOM;
}
