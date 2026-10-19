package com.allgenda.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

// url: caminho do GET que devolve a imagem (exige o token, como os demais endpoints)
public record ImagemAulaResponseDTO(
    UUID id,
    String legenda, // null quando a imagem não tem legenda
    String nome,
    String contentType,
    long tamanho,
    String url,
    LocalDateTime dataCriacao,
    UUID autorId, // o front compara com o id do usuário logado para saber se a imagem é dele
    String autorNome,
    UUID aulaId
) {}
