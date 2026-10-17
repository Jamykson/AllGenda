package com.allgenda.repository;

import java.util.Optional;
import java.util.UUID;

import com.allgenda.model.ImagemAnotacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagemAnotacaoRepository extends JpaRepository<ImagemAnotacao, UUID> {

    // exige que a imagem pertença à anotação do caminho, para que um id de outra anotação dê 404
    Optional<ImagemAnotacao> findByIdAndAnotacaoId(UUID id, UUID anotacaoId);
}
