package com.allgenda.service;

import com.allgenda.dto.response.TagResponseDTO;
import com.allgenda.model.Disciplina;
import com.allgenda.model.Tag;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// Tags são da disciplina. Listar, renomear e excluir exigem participação ACEITO nela.
public interface TagService {
    // reaproveita as tags da disciplina com esses nomes (sem diferenciar maiúsculas) e cria as que faltam
    Set<Tag> buscarOuCriar(Disciplina disciplina, List<String> nomes);
    // sem disciplina, lista as tags de todas as disciplinas em que o usuário participa; em ordem alfabética
    List<TagResponseDTO> listar(UUID disciplinaId);
    TagResponseDTO renomear(UUID tagId, String nome);
    void excluir(UUID tagId); // tira a tag de todas as anotações que a usam
}
