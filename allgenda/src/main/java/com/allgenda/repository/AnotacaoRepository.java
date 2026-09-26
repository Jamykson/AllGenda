package com.allgenda.repository;

import java.util.UUID;

import com.allgenda.model.Anotacao;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID> {
}