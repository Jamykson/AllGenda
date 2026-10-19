package com.allgenda.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.allgenda.model.ImagemAula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagemAulaRepository extends JpaRepository<ImagemAula, UUID> {

    List<ImagemAula> findByAulaIdOrderByDataCriacaoAsc(UUID aulaId);

    // exige que a imagem pertença à aula do caminho, para que um id de outra aula dê 404
    Optional<ImagemAula> findByIdAndAulaId(UUID id, UUID aulaId);

    // só as chaves dos arquivos, das imagens avulsas de todas as aulas da disciplina
    @Query("select i.chaveArmazenamento from ImagemAula i where i.aula.disciplina.id = :disciplinaId")
    List<String> findChavesArmazenamentoByDisciplinaId(@Param("disciplinaId") UUID disciplinaId);
}
