package com.allgenda.framework.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.framework.acesso.Papel;
import com.allgenda.framework.model.Participacao;
import com.allgenda.framework.model.StatusParticipacao;

public interface ParticipacaoRepository extends JpaRepository<Participacao, UUID> {
    void deleteByGrupoId(UUID grupoId);
    Optional<Participacao> findByUsuarioIdAndGrupoId(UUID usuarioId, UUID grupoId);
    List<Participacao> findByUsuarioIdAndStatus(UUID usuarioId, StatusParticipacao status);
    // só convites (papel de convidado): a participação de quem criou o grupo não é um convite
    List<Participacao> findByUsuarioIdAndPapelAndStatus(UUID usuarioId, Papel papel, StatusParticipacao status);
    List<Participacao> findByGrupoIdAndStatus(UUID grupoId, StatusParticipacao status);
}
