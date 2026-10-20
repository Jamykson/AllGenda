package com.allgenda.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.allgenda.model.ImagemAula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagemAulaRepository extends JpaRepository<ImagemAula, UUID> {

    // ordem da galeria; a data desempata imagens com a mesma posição
    List<ImagemAula> findByAulaIdOrderByPosicaoAscDataCriacaoAsc(UUID aulaId);

    // -1 quando a aula ainda não tem imagens, para que a primeira fique na posição 0
    @Query("select coalesce(max(i.posicao), -1) from ImagemAula i where i.aula.id = :aulaId")
    int findMaiorPosicaoByAulaId(@Param("aulaId") UUID aulaId);

    // exige que a imagem pertença à aula do caminho, para que um id de outra aula dê 404
    Optional<ImagemAula> findByIdAndAulaId(UUID id, UUID aulaId);

    // só as chaves dos arquivos, das imagens avulsas de todas as aulas da disciplina
    @Query("select i.chaveArmazenamento from ImagemAula i where i.aula.disciplina.id = :disciplinaId")
    List<String> findChavesArmazenamentoByDisciplinaId(@Param("disciplinaId") UUID disciplinaId);
}
