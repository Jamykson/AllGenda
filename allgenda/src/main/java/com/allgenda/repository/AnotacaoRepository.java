package com.allgenda.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.Anotacao;
import com.allgenda.model.VisibilidadeAnotacao;
import java.util.Collection;


public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID> {

    List<Anotacao> findByEncontroIdOrderByDataCriacaoAsc(UUID encontroId);

    // quantas anotações de um autor, com a visibilidade dada, existem no grupo
    long countByEncontroGrupoIdAndAutorIdAndVisibilidade(UUID grupoId, UUID autorId, VisibilidadeAnotacao visibilidade);

    // navega anotacao -> encontro -> grupo; ordena pela data do encontro e depois pela criação da anotação
    List<Anotacao> findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(UUID grupoId);

    // como o nome da tag é único, o join com tags não duplica anotações
    List<Anotacao> findByTagsNomeIgnoreCaseOrderByEncontroDataAscDataCriacaoAsc(String nomeTag);

    // anotações que usam a tag, para tirar o vínculo antes de excluí-la
    List<Anotacao> findByTagsId(UUID tagId);

    List<Anotacao> findByEncontroGrupoIdAndTagsNomeIgnoreCaseOrderByEncontroDataAscDataCriacaoAsc(UUID grupoId, String nomeTag);

    // anotações do grupo escritas por outros usuários (exclui as do próprio solicitante)
    List<Anotacao> findByEncontroGrupoIdAndAutorIdNotOrderByEncontroDataAscDataCriacaoAsc(UUID grupoId, UUID autorId);

    // filtro por tag restrito aos grupos informados (aqueles em que o usuário participa)
    List<Anotacao> findByTagsNomeIgnoreCaseAndEncontroGrupoIdInOrderByEncontroDataAscDataCriacaoAsc(String nomeTag, Collection<UUID> grupoIds);
}
