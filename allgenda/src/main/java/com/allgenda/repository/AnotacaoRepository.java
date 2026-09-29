package com.allgenda.repository;

import java.util.List;
import java.util.UUID;

import com.allgenda.model.Anotacao;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID> {

    List<Anotacao> findByAulaIdOrderByDataCriacaoAsc(UUID aulaId);

    // navega anotacao -> aula -> disciplina; ordena pela data da aula e depois pela criação da anotação
    List<Anotacao> findByAulaDisciplinaIdOrderByAulaDataAscDataCriacaoAsc(UUID disciplinaId);

    // como o nome da tag é único, o join com tags não duplica anotações
    List<Anotacao> findByTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc(String nomeTag);

    List<Anotacao> findByAulaDisciplinaIdAndTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc(UUID disciplinaId, String nomeTag);
}
