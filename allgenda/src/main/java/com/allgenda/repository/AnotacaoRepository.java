package com.allgenda.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.Anotacao;
import java.util.Collection;


public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID> {

    List<Anotacao> findByAulaIdOrderByDataCriacaoAsc(UUID aulaId);

    // navega anotacao -> aula -> disciplina; ordena pela data da aula e depois pela criação da anotação
    List<Anotacao> findByAulaDisciplinaIdOrderByAulaDataAscDataCriacaoAsc(UUID disciplinaId);

    // como o nome da tag é único, o join com tags não duplica anotações
    List<Anotacao> findByTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc(String nomeTag);

    // anotações que usam a tag, para tirar o vínculo antes de excluí-la
    List<Anotacao> findByTagsId(UUID tagId);

    List<Anotacao> findByAulaDisciplinaIdAndTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc(UUID disciplinaId, String nomeTag);

    // anotações da disciplina escritas por outros usuários (exclui as do próprio solicitante)
    List<Anotacao> findByAulaDisciplinaIdAndAutorIdNotOrderByAulaDataAscDataCriacaoAsc(UUID disciplinaId, UUID autorId);

    // filtro por tag restrito às disciplinas informadas (aquelas em que o usuário participa)
    List<Anotacao> findByTagsNomeIgnoreCaseAndAulaDisciplinaIdInOrderByAulaDataAscDataCriacaoAsc(String nomeTag, Collection<UUID> disciplinaIds);
}
