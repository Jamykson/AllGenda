package com.allgenda.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;

public interface ParticipacaoRepository extends JpaRepository<Participacao, UUID> {
    Optional<Participacao> findByUsuarioIdAndDisciplinaId(UUID usuarioId, UUID disciplinaId);
    List<Participacao> findByUsuarioIdAndStatus(UUID usuarioId, StatusParticipacao status);
    // só convites (papel MEMBRO): a participação do dono (DONO/ACEITO) não é um convite
    List<Participacao> findByUsuarioIdAndPapelAndStatus(UUID usuarioId, PapelParticipacao papel, StatusParticipacao status);
    boolean existsByUsuarioIdAndDisciplinaIdAndStatus(UUID usuarioId, UUID disciplinaId, StatusParticipacao status);
}
