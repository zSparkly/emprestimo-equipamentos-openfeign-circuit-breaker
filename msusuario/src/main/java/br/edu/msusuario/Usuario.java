package br.edu.msusuario;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "tb_usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
  @Id private UUID id;
  private String nome;
  private String email;
  private String matricula;
}
