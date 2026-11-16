package com.allgenda.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.model.Grupo;

public interface GrupoRepository extends JpaRepository<Grupo, UUID> {
}
