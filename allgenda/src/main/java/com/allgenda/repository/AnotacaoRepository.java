package com.allgenda.repository;

import com.allgenda.model.Anotacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID> {
}