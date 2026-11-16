package com.allgenda.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;

public interface ParticipacaoRepository extends JpaRepository<Participacao, UUID> {
    void deleteByGrupoId(UUID grupoId);
    Optional<Participacao> findByUsuarioIdAndGrupoId(UUID usuarioId, UUID grupoId);
    List<Participacao> findByUsuarioIdAndStatus(UUID usuarioId, StatusParticipacao status);
    // só convites (papel MEMBRO): a participação do dono (DONO/ACEITO) não é um convite
    List<Participacao> findByUsuarioIdAndPapelAndStatus(UUID usuarioId, PapelParticipacao papel, StatusParticipacao status);
    List<Participacao> findByGrupoIdAndStatus(UUID grupoId, StatusParticipacao status);
    boolean existsByUsuarioIdAndGrupoIdAndStatus(UUID usuarioId, UUID grupoId, StatusParticipacao status);
}
