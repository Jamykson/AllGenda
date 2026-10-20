package com.allgenda.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.Tag;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    // "Java" e "java" são a mesma tag dentro de uma disciplina
    Optional<Tag> findByDisciplinaIdAndNomeIgnoreCase(UUID disciplinaId, String nome);

    List<Tag> findByDisciplinaIdIn(Collection<UUID> disciplinaIds);
}
