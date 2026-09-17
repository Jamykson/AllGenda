package com.allgenda.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.Aula;

public interface AulaRepository extends JpaRepository<Aula, UUID> {

    List<Aula> findByDisciplinaIdOrderByDataAsc(UUID disciplinaId);
}