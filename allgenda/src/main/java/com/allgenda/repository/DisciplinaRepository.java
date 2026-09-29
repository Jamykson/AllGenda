package com.allgenda.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.Disciplina;

public interface DisciplinaRepository extends JpaRepository<Disciplina, UUID> {
}