package com.allgenda.framework.service;

import com.allgenda.framework.model.Grupo;
import com.allgenda.framework.model.Tag;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// Tags são do grupo. Listar exige VER_CONTEUDO; renomear e excluir, CRIAR_ANOTACAO.
public interface TagService {
    // reaproveita as tags do grupo com esses nomes (sem diferenciar maiúsculas) e cria as que faltam
    Set<Tag> buscarOuCriar(Grupo grupo, List<String> nomes);
    // sem grupo, lista as tags de todos os grupos em que o usuário participa; em ordem alfabética
    List<Tag> listar(UUID grupoId);
    Tag renomear(UUID tagId, String nome);
    void excluir(UUID tagId); // tira a tag de todas as anotações que a usam
}
