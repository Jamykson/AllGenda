package com.allgenda.framework.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allgenda.framework.model.Recorrencia;

public interface RecorrenciaRepository extends JpaRepository<Recorrencia, UUID> {

    Optional<Recorrencia> findByGrupoId(UUID grupoId);

    void deleteByGrupoId(UUID grupoId);

    // recorrências dos grupos em que a pessoa participa (convite aceito)
    @Query("""
            select r from Recorrencia r
            where r.grupo.id in (
                select p.grupo.id from Participacao p
                where p.usuario.id = :usuarioId and p.status = com.allgenda.framework.model.StatusParticipacao.ACEITO
            )
            """)
    List<Recorrencia> findDosGruposDoUsuario(@Param("usuarioId") UUID usuarioId);
}
