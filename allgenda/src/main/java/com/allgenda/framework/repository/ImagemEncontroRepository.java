package com.allgenda.framework.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.allgenda.framework.model.ImagemEncontro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagemEncontroRepository extends JpaRepository<ImagemEncontro, UUID> {

    // ordem da galeria; a data desempata imagens com a mesma posição
    List<ImagemEncontro> findByEncontroIdOrderByPosicaoAscDataCriacaoAsc(UUID encontroId);

    // imagens avulsas de todos os encontros do grupo: encontros por data, depois a ordem da galeria
    List<ImagemEncontro> findByEncontroGrupoIdOrderByEncontroDataAscPosicaoAscDataCriacaoAsc(UUID grupoId);

    // -1 quando o encontro ainda não tem imagens, para que a primeira fique na posição 0
    @Query("select coalesce(max(i.posicao), -1) from ImagemEncontro i where i.encontro.id = :encontroId")
    int findMaiorPosicaoByEncontroId(@Param("encontroId") UUID encontroId);

    // exige que a imagem pertença ao encontro do caminho, para que um id de outro encontro dê 404
    Optional<ImagemEncontro> findByIdAndEncontroId(UUID id, UUID encontroId);

    // só as chaves dos arquivos, das imagens avulsas de todos os encontros do grupo
    @Query("select i.chaveArmazenamento from ImagemEncontro i where i.encontro.grupo.id = :grupoId")
    List<String> findChavesArmazenamentoByGrupoId(@Param("grupoId") UUID grupoId);

    // só as chaves dos arquivos, das imagens avulsas de um encontro
    @Query("select i.chaveArmazenamento from ImagemEncontro i where i.encontro.id = :encontroId")
    List<String> findChavesArmazenamentoByEncontroId(@Param("encontroId") UUID encontroId);
}
