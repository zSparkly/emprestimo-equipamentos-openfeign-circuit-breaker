package br.edu.msequipamento;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipamentoService {
  private final EquipamentoRepository repo;

  public EquipamentoService(EquipamentoRepository r) {
    repo = r;
  }

  @Transactional
  public EquipamentoDto criar(EquipamentoDto.Entrada d) {
    var e = new Equipamento();
    e.setId(UUID.randomUUID());
    e.setNome(d.nome().trim());
    e.setTipo(d.tipo().trim());
    e.setPatrimonio(d.patrimonio().trim());
    e.setDisponivel(true);
    return EquipamentoDto.from(repo.saveAndFlush(e));
  }

  @Transactional
  public EquipamentoDto alterarCondicao(UUID id, CondicaoEquipamento condicao) {
    var equipamento = repo.bloquear(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));
    equipamento.setCondicao(condicao);
    return EquipamentoDto.from(equipamento);
  }

  public EquipamentoDto buscar(UUID id) {
    return EquipamentoDto.from(
        repo.findById(id)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "Equipamento não encontrado")));
  }

  public List<EquipamentoDto> disponiveis() {
    return repo.findByDisponivelTrue().stream().map(EquipamentoDto::from).toList();
  }

  @Transactional
  public EquipamentoDto reservar(UUID id, UUID reserva) {
    var e =
        repo.bloquear(id)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));
    if (reserva.equals(e.getReservaId())) return EquipamentoDto.from(e);
    if (!e.getDisponivel()) throw new ApiException(HttpStatus.CONFLICT, "Equipamento indisponivel");
    e.setReservaId(reserva);
    e.setDisponivel(false);
    return EquipamentoDto.from(e);
  }

  @Transactional
  public void liberar(UUID id, UUID reserva) {
    var e =
        repo.bloquear(id)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));
    if (e.getDisponivel()) return;
    if (!reserva.equals(e.getReservaId()))
      throw new ApiException(HttpStatus.CONFLICT, "A reserva pertence a outro empréstimo");
    e.setReservaId(null);
    e.setDisponivel(true);
  }
}
