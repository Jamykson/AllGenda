package com.allgenda.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.allgenda.model.ImagemAnotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagemAnotacaoRepository extends JpaRepository<ImagemAnotacao, UUID> {

    // exige que a imagem pertença à anotação do caminho, para que um id de outra anotação dê 404
    Optional<ImagemAnotacao> findByIdAndAnotacaoId(UUID id, UUID anotacaoId);

    // só as chaves dos arquivos, de todas as anotações de todas as aulas da disciplina
    @Query("select i.chaveArmazenamento from ImagemAnotacao i where i.anotacao.aula.disciplina.id = :disciplinaId")
    List<String> findChavesArmazenamentoByDisciplinaId(@Param("disciplinaId") UUID disciplinaId);

    // só as chaves dos arquivos, das anotações de uma aula
    @Query("select i.chaveArmazenamento from ImagemAnotacao i where i.anotacao.aula.id = :aulaId")
    List<String> findChavesArmazenamentoByAulaId(@Param("aulaId") UUID aulaId);
}
