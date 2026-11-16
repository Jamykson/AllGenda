package com.allgenda.framework.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allgenda.framework.model.Encontro;

public interface EncontroRepository extends JpaRepository<Encontro, UUID> {

    List<Encontro> findByGrupoId(UUID grupoId);
}
