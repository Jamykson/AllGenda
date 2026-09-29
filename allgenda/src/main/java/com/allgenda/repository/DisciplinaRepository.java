package com.allgenda.repository;

import com.allgenda.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DisciplinaRepository
    extends JpaRepository<Disciplina, UUID> {
}