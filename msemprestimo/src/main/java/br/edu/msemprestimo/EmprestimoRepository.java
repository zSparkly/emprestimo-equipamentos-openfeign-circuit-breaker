package br.edu.msemprestimo;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, UUID> {
  List<Emprestimo> findByUsuarioId(UUID usuarioId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Emprestimo e where e.id=:id")
  Optional<Emprestimo> bloquear(@Param("id") UUID id);
}
