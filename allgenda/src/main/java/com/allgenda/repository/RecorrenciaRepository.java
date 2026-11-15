package com.allgenda.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allgenda.model.Recorrencia;

public interface RecorrenciaRepository extends JpaRepository<Recorrencia, UUID> {

    Optional<Recorrencia> findByDisciplinaId(UUID disciplinaId);

    void deleteByDisciplinaId(UUID disciplinaId);

    // recorrências das disciplinas em que a pessoa participa (convite aceito)
    @Query("""
            select r from Recorrencia r
            where r.disciplina.id in (
                select p.disciplina.id from Participacao p
                where p.usuario.id = :usuarioId and p.status = com.allgenda.model.StatusParticipacao.ACEITO
            )
            """)
    List<Recorrencia> findDasDisciplinasDoUsuario(@Param("usuarioId") UUID usuarioId);
}
