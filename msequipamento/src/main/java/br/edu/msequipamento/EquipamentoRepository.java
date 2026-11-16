package br.edu.msequipamento;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID> {
  List<Equipamento> findByDisponivelTrue();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Equipamento e where e.id=:id")
  Optional<Equipamento> bloquear(@Param("id") UUID id);
}
