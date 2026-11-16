package com.allgenda.framework.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.framework.model.Grupo;

public interface GrupoRepository extends JpaRepository<Grupo, UUID> {
}
