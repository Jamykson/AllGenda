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

    // só as chaves dos arquivos, de todas as anotações de todos os encontros do grupo
    @Query("select i.chaveArmazenamento from ImagemAnotacao i where i.anotacao.encontro.grupo.id = :grupoId")
    List<String> findChavesArmazenamentoByGrupoId(@Param("grupoId") UUID grupoId);

    // só as chaves dos arquivos, das anotações de um encontro
    @Query("select i.chaveArmazenamento from ImagemAnotacao i where i.anotacao.encontro.id = :encontroId")
    List<String> findChavesArmazenamentoByEncontroId(@Param("encontroId") UUID encontroId);
}
