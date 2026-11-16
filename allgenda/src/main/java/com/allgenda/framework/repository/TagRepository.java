package com.allgenda.framework.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.framework.model.Tag;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    // "Java" e "java" são a mesma tag dentro de um grupo
    Optional<Tag> findByGrupoIdAndNomeIgnoreCase(UUID grupoId, String nome);

    List<Tag> findByGrupoIdIn(Collection<UUID> grupoIds);
}
