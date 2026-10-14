package com.allgenda.repository;

import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipacaoRepository extends JpaRepository<Participacao, UUID> {
    Optional<Participacao> findByUsuarioIdAndDisciplinaId(UUID usuarioId, UUID disciplinaId);
    List<Participacao> findByUsuarioIdAndStatus(UUID usuarioId, StatusParticipacao status);
}
